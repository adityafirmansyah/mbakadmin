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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class WhatsAppNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    // Cache untuk mencegah membalas pesan yang sama dalam jendela 20 detik (berbasis Sender + Teks)
    private val processedMessageCache = ConcurrentHashMap<String, Long>()

    // Set lock untuk mencegah konkurensi (jika WhatsApp mengirim beberapa event notifikasi sekaligus)
    private val inFlightMessages = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    // Cache balasan yang baru saja dikirim bot agar tidak membalas pesan sendiri jika terjadi echo di notifikasi
    private val recentSentReplies = ConcurrentHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        // Target WhatsApp consumer & WhatsApp Business
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") return

        val notification = sbn.notification ?: return

        // 1. FILTER KRUSIAL: Abaikan Group Summary Notification
        // WhatsApp selalu menerbitkan 2 notifikasi: notifikasi chat individual + notifikasi summary grup.
        // Jika summary tidak diabaikan, setiap chat akan diproses 2 kali!
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return
        }

        // 2. Ekstrak pesan menggunakan MessagingStyle (Anti-Truncate)
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)

        var senderName = "Kak"
        var incomingText = ""

        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val latestMessage = messagingStyle.messages.last()

            // FILTER KRUSIAL: Abaikan pesan yang dikirim oleh diri sendiri (bot/user reply)
            // Di Android MessagingStyle, jika person == null maka pesan tersebut berasal dari pengguna lokal
            if (latestMessage.person == null) {
                return
            }

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

        // Filter: Abaikan pesan dari diri sendiri
        if (senderName.equals("You", ignoreCase = true) || senderName.equals("Anda", ignoreCase = true)) {
            return
        }

        // Filter: Abaikan jika incomingText adalah echo dari balasan yang baru saja dikirim bot
        val cleanIncoming = incomingText.trim()
        if (recentSentReplies.containsKey(cleanIncoming)) {
            return
        }

        // 3. DEDUP GLOBAL KUAT: Gunakan (Tag/Sender + Teks), JANGAN gunakan sbn.key!
        // sbn.key berubah saat notifikasi diupdate oleh sistem/WhatsApp sehingga hash lama tidak cocok.
        val chatIdentifier = sbn.tag ?: senderName
        val messageHash = "${chatIdentifier}_${cleanIncoming}"
        val now = System.currentTimeMillis()
        val lastProcessed = processedMessageCache[messageHash] ?: 0L
        if (now - lastProcessed < 60_000) {
            return
        }

        // 4. IN-FLIGHT LOCK: Mencegah eksekusi paralel pada pesan yang sama
        if (!inFlightMessages.add(messageHash)) {
            return
        }
        processedMessageCache[messageHash] = now

        // 5. Ekstrak Action Reply & RemoteInput dari Notifikasi
        val replyAction = extractReplyAction(notification)
        if (replyAction == null) {
            inFlightMessages.remove(messageHash)
            return
        }

        // 6. Proses respon AI di background coroutine dengan isolasi lock
        serviceScope.launch {
            try {
                handleIncomingCustomerMessage(
                    context = applicationContext,
                    senderName = senderName,
                    customerMessage = incomingText,
                    replyAction = replyAction
                )
            } finally {
                // Beri jeda 30 detik sebelum melepas lock agar bounce update notifikasi selesai
                delay(30_000)
                inFlightMessages.remove(messageHash)
            }
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

        // 5. Ambil riwayat percakapan sebelumnya untuk customer ini (Multi-turn conversational context)
        val historyLogs = db.chatLogDao().getRecentMessagesForSender(senderName, limit = 6)

        // 6. Susun Konteks Toko & Prompt untuk AI
        val systemPrompt = buildSystemPrompt(settings, products)

        val adminName = settings.adminName.ifBlank { "Mbak Admin" }
        val fallbackTemplate = settings.fallbackReplyMessage.ifBlank {
            "Halo kak! Pesan sudah {admin} terima ya, mohon ditunggu sebentar biar segera dicek manual oleh tim kami 🙏"
        }
        val fallbackMsg = fallbackTemplate.replace("{admin}", adminName)

        // 7. Request ke AI Engine dengan konteks riwayat percakapan
        val aiReply = callAiEngine(settings, systemPrompt, historyLogs, customerMessage)

        // 8. Jika AI berhasil atau fallback jika error/offline
        val finalReply = if (aiReply.isNotBlank()) {
            aiReply
        } else {
            fallbackMsg
        }

        // 9. Kirim balasan via RemoteInput (100% Native Background)
        sendReply(context, replyAction, finalReply)

        // 10. Deteksi apakah pesan mengindikasikan klaim pembayaran / bukti transfer
        val lowerMsg = customerMessage.lowercase()
        val isPaymentNotice = lowerMsg.contains("sudah tf") ||
            lowerMsg.contains("sudah transfer") ||
            lowerMsg.contains("sudah bayar") ||
            lowerMsg.contains("bukti transfer") ||
            lowerMsg.contains("bukti tf") ||
            lowerMsg.contains("lunas") ||
            lowerMsg.contains("struk") ||
            lowerMsg.contains("cek mutasi")

        // Ekstrak no HP / WhatsApp jika pembeli mengisi format pemesanan
        val phoneRegex = Regex("""(08\d{8,12}|628\d{8,12}|\+628\d{8,12})""")
        val extractedPhone = phoneRegex.find(customerMessage)?.value?.replace("+", "")?.trim() ?: ""

        // 11. Simpan ke Riwayat Chat Log Lokal
        try {
            db.chatLogDao().insertChatLog(
                com.mbakadmin.dagango.data.local.entity.ChatLogEntity(
                    senderName = senderName,
                    incomingMessage = customerMessage,
                    aiReply = if (aiReply.isNotBlank()) finalReply else "[Fallback Offline] $finalReply",
                    isPaymentClaimed = isPaymentNotice,
                    isVerifiedByHuman = false,
                    customerPhone = extractedPhone,
                    orderStatus = if (isPaymentNotice) "PENDING_VERIFICATION" else "PENDING"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menyimpan chat log: ${e.message}")
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

            // Catat teks yang dikirim agar jika WhatsApp memantulkan pesan ini di notifikasi, tidak dibalas lagi
            val cleanReply = replyText.trim()
            recentSentReplies[cleanReply] = System.currentTimeMillis()

            // Bersihkan entri lama di recentSentReplies (> 60 detik)
            val cutoff = System.currentTimeMillis() - 60_000
            recentSentReplies.entries.removeIf { it.value < cutoff }
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
        val shippingFeeText = "Rp${settings.defaultShippingFee.toInt()}"
        val orderFormat = settings.orderFormatTemplate.ifBlank {
            "Nama Penerima:\nNo. HP/WA:\nAlamat Lengkap:\nKecamatan & Kota:\nPesanan & Varian:\nJumlah:"
        }

        return """
            Kamu adalah '$adminName', asisten penjualan toko '${settings.storeName}'.
            Gaya Bicara: ${settings.botTone}.
            
            INFORMASI TOKO:
            - Pengiriman: ${settings.shippingInfo}
            - Estimasi Ongkos Kirim Standar: $shippingFeeText (dapat disesuaikan jika pembeli menyebutkan tujuan spesifik).
            - Rekening Pembayaran:
            ${settings.paymentInfo}
            
            $productCatalog
            
            FORMAT PEMESANAN TOKO:
            $orderFormat
            
            ATURAN ALUR PENJUALAN (SALES ORDER FUNNEL) - WAJIB DIIKUTI SECARA TERTIB TAHAP DEMI TAHAP:
            
            TAHAP 1: KONSULTASI & CEK STOK
            - Jawab pertanyaan seputar ketersediaan stok, harga, varian, dan detail produk dengan ramah dan ringkas.
            
            TAHAP 2: PERMINTAAN FORMAT ORDER (DILARANG KIRIM NO REKENING SEKARANG!)
            - KETIKA pembeli menyatakan ingin membeli/memesan (misal: "mau pesan", "ambil 2 pcs", "order ya kak", "pesan warna biru"):
              JANGAN PERNAH mengirimkan nomor rekening atau meminta transfer sekarang!
            - Mintalah pembeli untuk mengisi FORMAT PEMESANAN TOKO di atas secara lengkap beserta alamat pengirimannya terlebih dahulu.
            
            TAHAP 3: RINCIAN TOTAL + ONGKIR + NOMOR REKENING
            - SETELAH pembeli mengirimkan format pesanan dan alamat:
              1. Tuliskan ringkasan pesanan & alamat pembeli.
              2. Hitung TOTAL PEMBAYARAN = (Harga Produk x Jumlah) + Ongkos Kirim ($shippingFeeText). Tuliskan rincian subtotal barang dan ongkirnya secara jelas.
              3. Berikan nomor rekening / metode pembayaran toko.
              4. Ingatkan pembeli untuk mengirimkan bukti transfer jika sudah melakukan pembayaran.
            
            TAHAP 4: VERIFIKASI PEMBAYARAN OLEH HUMAN (HUMAN VERIFICATION GATE)
            - JIKA pembeli menyatakan sudah transfer/sudah bayar (misal: "sudah transfer", "sudah tf", "ini buktinya", "cek mutasi"):
              JANGAN mengonfirmasi bahwa uang sudah otomatis lunas/selesai!
            - Katakan dengan ramah bahwa konfirmasi/bukti pembayaran sudah diterima dan SEDANG DITERUSKAN UNTUK DICEK & DIVERIFIKASI SECARA MANUAL OLEH ADMIN TOKO.
            - Beritahu bahwa pesanan akan segera diproses setelah verifikasi selesai.
            
            BATASAN FORMAT:
            - Balaslah seringkas dan sealami mungkin (chat WhatsApp Indonesia yang hangat, bukan robot kaku).
        """.trimIndent()
    }

    private fun callAiEngine(
        settings: StoreSettingsEntity,
        systemPrompt: String,
        historyLogs: List<com.mbakadmin.dagango.data.local.entity.ChatLogEntity>,
        customerMessage: String
    ): String {
        val apiKey = settings.aiApiKey.ifBlank { StoreSettingsEntity.DEFAULT_AI_API_KEY }
        val baseUrl = settings.aiApiBaseUrl.ifBlank { StoreSettingsEntity.DEFAULT_AI_BASE_URL }.trim()
        val model = settings.aiModel.ifBlank { StoreSettingsEntity.DEFAULT_AI_MODEL }

        // Jika API key tetap kosong
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
            val client = httpClient.newBuilder()
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

    private fun parseAiResponseContent(rawBody: String): String {
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
        private const val TAG = "MbakAdminListener"
    }
}
