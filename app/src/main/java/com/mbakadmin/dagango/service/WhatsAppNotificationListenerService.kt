package com.mbakadmin.dagango.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.mbakadmin.dagango.data.local.MbakAdminDatabase
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class WhatsAppNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    // Cache untuk mencegah membalas pesan yang sama berulang-ulang
    private val processedMessageCache = ConcurrentHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        // Target WhatsApp consumer & WhatsApp Business
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return

        val notification = sbn.notification ?: return

        // 1. Ekstrak pesan menggunakan MessagingStyle (Anti-Truncate)
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        
        var senderName = "Kak"
        var incomingText = ""

        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val latestMessage = messagingStyle.messages.last()
            incomingText = latestMessage.text?.toString() ?: ""
            senderName = latestMessage.person?.name?.toString() ?: "Kak"
        } else {
            // Fallback ke extras standar jika format non-messaging style
            val extras = notification.extras
            incomingText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            senderName = extras.getString(Notification.EXTRA_TITLE) ?: "Kak"
        }

        if (incomingText.isBlank()) return

        // Filter: Abaikan pesan sistem WA (misal: "Memeriksa pesan baru", "Pesan dari N chat")
        if (incomingText.contains("pesan baru", ignoreCase = true) || incomingText.contains("messages from", ignoreCase = true)) {
            return
        }

        // 2. Cegah duplikasi respon dalam interval 10 detik
        val messageHash = "${sbn.key}_${incomingText.trim()}"
        val now = System.currentTimeMillis()
        val lastProcessed = processedMessageCache[messageHash] ?: 0L
        if (now - lastProcessed < 10_000) {
            return
        }
        processedMessageCache[messageHash] = now

        // 3. Ekstrak Action Reply & RemoteInput dari Notifikasi
        val replyAction = extractReplyAction(notification) ?: return

        // 4. Proses respon AI di background coroutine
        serviceScope.launch {
            handleIncomingCustomerMessage(
                context = applicationContext,
                senderName = senderName,
                customerMessage = incomingText,
                replyAction = replyAction
            )
        }
    }

    private suspend fun handleIncomingCustomerMessage(
        context: Context,
        senderName: String,
        customerMessage: String,
        replyAction: NotificationCompat.Action
    ) {
        val db = MbakAdminDatabase.getInstance(context)
        val settings = db.storeSettingsDao().getSettings() ?: StoreSettingsEntity()

        if (!settings.isAutoReplyActive) {
            Log.d(TAG, "MbakAdmin auto-reply dinonaktifkan di pengaturan.")
            return
        }

        val products = db.productDao().getActiveProducts()

        // 5. Susun Konteks Toko & Prompt untuk AI
        val systemPrompt = buildSystemPrompt(settings, products)
        val userPrompt = "Pembeli ($senderName): \"$customerMessage\"\nBalaslah sebagai Mbak Admin toko dengan format singkat, ramah, dan solutif."

        // 6. Request ke AI Engine
        val aiReply = callAiEngine(settings, systemPrompt, userPrompt)

        if (aiReply.isNotBlank()) {
            // 7. Kirim balasan via RemoteInput (100% Native Background)
            sendReply(context, replyAction, aiReply)

            // 8. Simpan ke Riwayat Chat Log Lokal
            try {
                db.chatLogDao().insertChatLog(
                    com.mbakadmin.dagango.data.local.entity.ChatLogEntity(
                        senderName = senderName,
                        incomingMessage = customerMessage,
                        aiReply = aiReply
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menyimpan chat log: ${e.message}")
            }
        }
    }

    private fun extractReplyAction(notification: Notification): NotificationCompat.Action? {
        // 1. Coba dari direct notification actions
        val actionsCount = NotificationCompat.getActionCount(notification)
        for (i in 0 until actionsCount) {
            val action = NotificationCompat.getAction(notification, i)
            if (action != null && hasRemoteInput(action)) {
                return action
            }
        }

        // 2. Coba dari WearableExtender actions (WhatsApp sering menaruh remote input di sini)
        val wearableExtender = NotificationCompat.WearableExtender(notification)
        for (action in wearableExtender.actions) {
            if (hasRemoteInput(action)) return action
        }

        // 3. Fallback: Cek actions dari standard notification.actions (Android framework)
        notification.actions?.forEach { frameworkAction ->
            frameworkAction.remoteInputs?.let { remoteInputs ->
                if (remoteInputs.isNotEmpty() && frameworkAction.actionIntent != null) {
                    val compatBuilder = NotificationCompat.Action.Builder(
                        frameworkAction.icon,
                        frameworkAction.title,
                        frameworkAction.actionIntent
                    )
                    remoteInputs.forEach { ri ->
                        compatBuilder.addRemoteInput(
                            RemoteInput.Builder(ri.resultKey)
                                .setLabel(ri.label)
                                .setChoices(ri.choices)
                                .setAllowFreeFormInput(ri.allowFreeFormInput)
                                .addExtras(ri.extras)
                                .build()
                        )
                    }
                    return compatBuilder.build()
                }
            }
        }

        return null
    }

    private fun hasRemoteInput(action: NotificationCompat.Action): Boolean {
        return action.remoteInputs?.any { it.allowFreeFormInput } == true
    }

    private fun sendReply(context: Context, action: NotificationCompat.Action, replyText: String) {
        val remoteInputs = action.remoteInputs ?: return
        val pendingIntent = action.actionIntent ?: return
        val intent = Intent()
        val bundle = Bundle()

        remoteInputs.forEach { remoteInput ->
            bundle.putCharSequence(remoteInput.resultKey, replyText)
        }

        RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
        try {
            pendingIntent.send(context, 0, intent)
            Log.d(TAG, "MbakAdmin sukses mengirim balasan otomatis: $replyText")
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "Gagal mengirim PendingIntent: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saat eksekusi reply PendingIntent: ${e.message}")
        }
    }

    private fun buildSystemPrompt(settings: StoreSettingsEntity, products: List<ProductEntity>): String {
        val productCatalog = StringBuilder()
        if (products.isEmpty()) {
            productCatalog.append("Katalog: (Belum ada data produk tersimpan, minta pembeli menunggu admin manual jika tanya produk khusus)")
        } else {
            productCatalog.append("Katalog Produk Aktif:\n")
            products.forEach { p ->
                productCatalog.append("- ${p.name} | Rp${p.price.toInt()} | Stok: ${p.stock}")
                if (p.variants.isNotBlank()) productCatalog.append(" | Varian: ${p.variants}")
                if (p.description.isNotBlank()) productCatalog.append(" | Deskripsi: ${p.description}")
                productCatalog.append("\n")
            }
        }

        val adminName = settings.adminName.ifBlank { "Mbak Admin" }

        return """
            Kamu adalah '$adminName', asisten penjualan resmi toko '${settings.storeName}'.
            Gaya Bicara: ${settings.botTone}.
            
            Informasi Pembayaran:
            ${settings.paymentInfo}
            
            Informasi Pengiriman:
            ${settings.shippingInfo}
            
            $productCatalog
            
            Instruksi Wajib:
            1. Selalu ramah, gunakan panggilan 'kak'.
            2. Jawab pertanyaan seputar stok, harga, varian, ongkir, dan rekening pembayaran sesuai data di atas.
            3. Jika stok habis atau tidak ada di katalog, katakan dengan sopan dan tawarkan produk alternatif yang mirip jika ada.
            4. Balaslah seringkas dan sealami mungkin (1-3 kalimat) seperti admin WhatsApp sungguhan. Jangan bertele-tele dan hindari format Markdown tabel/bullet tebal yang kaku.
        """.trimIndent()
    }

    private fun callAiEngine(settings: StoreSettingsEntity, systemPrompt: String, userPrompt: String): String {
        val adminName = settings.adminName.ifBlank { "Mbak Admin" }

        // Fallback jika API key belum diisi pengguna
        if (settings.aiApiKey.isBlank()) {
            return "Halo kak! Pesan sudah $adminName terima ya, mohon ditunggu sebentar biar segera dicek manual oleh tim kami 🙏"
        }

        try {
            val payload = JSONObject().apply {
                put("model", settings.aiModel.ifBlank { "gpt-4o-mini" })
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 200)
            }

            var baseUrl = settings.aiApiBaseUrl.trim()
            if (baseUrl.isBlank()) {
                baseUrl = "https://api.openai.com/v1"
            }
            // Normalisasi URL endpoint: pastikan mengarah ke /chat/completions
            val endpoint = if (baseUrl.endsWith("/chat/completions")) {
                baseUrl
            } else {
                "${baseUrl.trimEnd('/')}/chat/completions"
            }

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer ${settings.aiApiKey}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request AI gagal code: ${response.code}, body: $errBody")
                    return ""
                }
                val body = response.body?.string() ?: return ""
                val json = JSONObject(body)
                val choices = json.optJSONArray("choices") ?: return ""
                if (choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val message = choice.optJSONObject("message")
                    return message?.optString("content")?.trim() ?: ""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal request AI: ${e.message}")
        }
        return ""
    }

    companion object {
        private const val TAG = "MbakAdminListener"
    }
}
