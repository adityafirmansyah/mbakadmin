package com.mbakadmin.dagango.service

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mbakadmin.dagango.data.local.MbakAdminDatabase
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity
import com.mbakadmin.dagango.service.ai.AiReplyEngine
import com.mbakadmin.dagango.service.ai.PromptBuilder
import com.mbakadmin.dagango.service.notification.WhatsAppReplyDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class WhatsAppNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
    private val aiReplyEngine = AiReplyEngine(httpClient)

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
        var hasAttachment = false

        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val latestMessage = messagingStyle.messages.last()

            // FILTER KRUSIAL: Abaikan pesan yang dikirim oleh diri sendiri (bot/user reply)
            // Di Android MessagingStyle, jika person == null maka pesan tersebut berasal dari pengguna lokal
            if (latestMessage.person == null) {
                return
            }

            incomingText = latestMessage.text?.toString() ?: ""
            senderName = latestMessage.person?.name?.toString() ?: "Kak"

            if (latestMessage.dataMimeType?.startsWith("image/") == true || latestMessage.dataUri != null) {
                hasAttachment = true
            }
        } else {
            // Fallback ke extras standar jika format non-messaging style
            val extras = notification.extras
            incomingText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            senderName = extras.getString(Notification.EXTRA_TITLE) ?: "Kak"

            if (extras != null && (extras.containsKey(Notification.EXTRA_PICTURE) || extras.containsKey("android.picture"))) {
                hasAttachment = true
            }
        }

        // Cek attachment dari extras jika belum terdeteksi
        if (!hasAttachment) {
            val extras = notification.extras
            if (extras != null && (extras.containsKey(Notification.EXTRA_PICTURE) || extras.containsKey("android.picture"))) {
                hasAttachment = true
            }
        }

        // Cek teks notifikasi WhatsApp standar jika memuat indikator media
        if (!hasAttachment) {
            val lowerText = incomingText.lowercase()
            if (incomingText.contains("📷") ||
                lowerText.startsWith("foto") ||
                lowerText.startsWith("photo") ||
                lowerText.contains("📷 foto") ||
                lowerText.contains("📷 photo")
            ) {
                hasAttachment = true
            }
        }

        // Jika pembeli mengirim gambar murni tanpa caption, jangan abaikan notifikasi
        if (incomingText.isBlank() && hasAttachment) {
            incomingText = "📷 [Mengirim Foto Bukti / Gambar]"
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
        val replyAction = WhatsAppReplyDispatcher.extractReplyAction(notification)
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
                    replyAction = replyAction,
                    pkg = pkg,
                    hasAttachment = hasAttachment
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
        replyAction: NotificationCompat.Action,
        pkg: String,
        hasAttachment: Boolean
    ) {
        val db = MbakAdminDatabase.getInstance(context)
        val settings = db.storeSettingsDao().getSettings() ?: StoreSettingsEntity()

        if (!settings.isAutoReplyActive) {
            Log.d(TAG, "MbakAdmin auto-reply dinonaktifkan di pengaturan.")
            return
        }

        // 1. FILTER TARGET WHATSAPP (Standar vs Business)
        if (settings.targetWhatsAppPackage == "WHATSAPP_BUSINESS" && pkg != "com.whatsapp.w4b") {
            Log.d(TAG, "Abaikan pesan: Pengaturan toko disetel khusus WhatsApp Business ($pkg diabaikan).")
            return
        }
        if (settings.targetWhatsAppPackage == "WHATSAPP_STANDARD" && pkg != "com.whatsapp") {
            Log.d(TAG, "Abaikan pesan: Pengaturan toko disetel khusus WhatsApp Standar ($pkg diabaikan).")
            return
        }

        // 2. FILTER PRIVASI KONTAK (Hanya Balas Nomor Tidak Dikenal / Luar Kontak)
        if (settings.onlyReplyUnknownContacts && !WhatsAppReplyDispatcher.isUnknownPhoneNumber(senderName)) {
            Log.d(TAG, "Abaikan pesan dari '$senderName': Pengaturan aktif untuk HANYA membalas nomor baru/luar kontak.")
            return
        }

        val products = db.productDao().getActiveProducts()

        // 5. Ambil riwayat percakapan sebelumnya untuk customer ini (Multi-turn conversational context)
        val historyLogs = db.chatLogDao().getRecentMessagesForSender(senderName, limit = 6)

        // 6. Susun Konteks Toko & Prompt untuk AI
        val systemPrompt = PromptBuilder.buildSystemPrompt(settings, products)

        val adminName = settings.adminName.ifBlank { "Mbak Admin" }
        val fallbackTemplate = settings.fallbackReplyMessage.ifBlank {
            "Halo kak! Pesan sudah {admin} terima ya, mohon ditunggu sebentar biar segera dicek manual oleh tim kami 🙏"
        }
        val fallbackMsg = fallbackTemplate.replace("{admin}", adminName)

        // Sisipkan informasi status lampiran media ke pesan pelanggan untuk AI
        val promptForAi = if (hasAttachment) {
            "$customerMessage\n[Catatan Sistem: Pembeli MELAMPIRKAN foto/gambar/struk bukti]"
        } else {
            "$customerMessage\n[Catatan Sistem: Pembeli HANYA mengirim pesan teks, TIDAK melampirkan foto/gambar/struk bukti]"
        }

        // 7. Request ke AI Engine dengan konteks riwayat percakapan
        val aiReply = aiReplyEngine.callAiEngine(settings, systemPrompt, historyLogs, promptForAi)

        // 8. Jika AI berhasil atau fallback jika error/offline
        val finalReply = if (aiReply.isNotBlank()) {
            aiReply
        } else {
            fallbackMsg
        }

        // 9. Kirim balasan via RemoteInput (100% Native Background)
        WhatsAppReplyDispatcher.sendReply(context, replyAction, finalReply) {
            val cleanReply = finalReply.trim()
            recentSentReplies[cleanReply] = System.currentTimeMillis()

            // Bersihkan entri lama di recentSentReplies (> 60 detik)
            val cutoff = System.currentTimeMillis() - 60_000
            recentSentReplies.entries.removeIf { it.value < cutoff }
        }

        // 10. Deteksi apakah pesan mengindikasikan klaim pembayaran / bukti transfer
        val lowerMsg = customerMessage.lowercase()
        val isPaymentNotice = hasAttachment ||
            lowerMsg.contains("sudah tf") ||
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
                ChatLogEntity(
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

    companion object {
        private const val TAG = "MbakAdminListener"
    }
}
