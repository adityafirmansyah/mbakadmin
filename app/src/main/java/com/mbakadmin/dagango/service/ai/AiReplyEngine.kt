package com.mbakadmin.dagango.service.ai

import android.util.Log
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiReplyEngine(private val baseHttpClient: OkHttpClient) {

    fun callAiEngine(
        settings: StoreSettingsEntity,
        systemPrompt: String,
        historyLogs: List<ChatLogEntity>,
        customerMessage: String
    ): String {
        val apiKey = settings.aiApiKey.ifBlank { StoreSettingsEntity.DEFAULT_AI_API_KEY }
        val baseUrl = settings.aiApiBaseUrl.ifBlank { StoreSettingsEntity.DEFAULT_AI_BASE_URL }.trim()
        val model = settings.aiModel.ifBlank { StoreSettingsEntity.DEFAULT_AI_MODEL }

        if (apiKey.isBlank()) {
            return ""
        }

        try {
            val messagesArray = JSONArray().apply {
                // 1. System Prompt
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                // 2. Chat history (urutkan kronologis: terlama ke terbaru)
                historyLogs.reversed().forEach { log ->
                    if (log.incomingMessage.isNotBlank()) {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", log.incomingMessage)
                        })
                    }
                    if (log.aiReply.isNotBlank() && !log.aiReply.startsWith("[Fallback")) {
                        put(JSONObject().apply {
                            put("role", "assistant")
                            put("content", log.aiReply)
                        })
                    }
                }

                // 3. Current customer message
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", customerMessage)
                })
            }

            val payload = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("temperature", 0.7)
                put("max_tokens", 400)
                put("stream", false)
            }

            // Normalisasi URL endpoint: pastikan mengarah ke /chat/completions
            val endpoint = if (baseUrl.endsWith("/chat/completions")) {
                baseUrl
            } else {
                "${baseUrl.trimEnd('/')}/chat/completions"
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            // Timeout dinamis berbasis konfigurasi toko (default 60s, batas 15s - 180s)
            val timeoutSec = settings.aiTimeoutSeconds.toLong().coerceIn(15L, 180L)
            val client = baseHttpClient.newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(timeoutSec, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request AI gagal code: ${response.code}, body: $errBody")
                    return ""
                }
                val body = response.body?.string() ?: return ""
                return parseAiResponseContent(body)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal request AI: ${e.message}")
        }
        return ""
    }

    fun parseAiResponseContent(rawBody: String): String {
        val body = rawBody.trim()
        if (body.isBlank()) return ""

        // 1. Coba parse sebagai standard JSON response
        if (body.startsWith("{")) {
            try {
                val json = JSONObject(body)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val message = choice.optJSONObject("message")
                    val content = message?.optString("content")
                    if (!content.isNullOrBlank()) return content.trim()

                    val delta = choice.optJSONObject("delta")
                    val deltaContent = delta?.optString("content")
                    if (!deltaContent.isNullOrBlank()) return deltaContent.trim()
                }
            } catch (_: Exception) {}
        }

        // 2. Fallback parse SSE stream (data: {...}\n\ndata: {...}) jika gateway mengirim stream
        if (body.contains("data:")) {
            val contentBuilder = StringBuilder()
            body.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("data:") && !trimmed.contains("[DONE]")) {
                    val jsonStr = trimmed.removePrefix("data:").trim()
                    try {
                        val json = JSONObject(jsonStr)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val delta = choice.optJSONObject("delta")
                            val part = delta?.optString("content")
                            if (!part.isNullOrEmpty()) {
                                contentBuilder.append(part)
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
            val sseResult = contentBuilder.toString().trim()
            if (sseResult.isNotBlank()) return sseResult
        }

        return ""
    }

    companion object {
        private const val TAG = "MbakAdminAiEngine"
    }
}
