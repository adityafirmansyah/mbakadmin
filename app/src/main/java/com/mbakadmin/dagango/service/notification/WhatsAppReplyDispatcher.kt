package com.mbakadmin.dagango.service.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

object WhatsAppReplyDispatcher {

    private const val TAG = "MbakAdminDispatcher"

    /**
     * Memeriksa apakah pengirim pesan WhatsApp merupakan nomor telepon baru/tidak dikenal.
     * Di WhatsApp, nomor yang BELUM disimpan di kontak HP akan tampil sebagai nomor telepon (misal: +62 812-xxx atau 0812xxx),
     * sedangkan nomor yang SUDAH disimpan di kontak HP akan tampil sebagai nama kontak (misal: "Budi", "Mamah", "Istri").
     */
    fun isUnknownPhoneNumber(sender: String): Boolean {
        val trimmed = sender.trim()
        if (trimmed.isBlank()) return false
        val startsWithPhone = trimmed.startsWith("+") || trimmed.startsWith("08") || trimmed.startsWith("62")
        val digitCount = trimmed.count { it.isDigit() }
        val nonPhoneChars = trimmed.count { !it.isDigit() && it != '+' && it != '-' && it != ' ' && it != '(' && it != ')' }
        return (startsWithPhone && digitCount >= 8 && nonPhoneChars == 0) || (digitCount >= 9 && nonPhoneChars <= 1)
    }

    fun extractReplyAction(notification: Notification): NotificationCompat.Action? {
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

    fun hasRemoteInput(action: NotificationCompat.Action): Boolean {
        return action.remoteInputs?.any { it.allowFreeFormInput } == true
    }

    fun sendReply(
        context: Context,
        action: NotificationCompat.Action,
        replyText: String,
        onSuccess: (() -> Unit)? = null
    ) {
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
            onSuccess?.invoke()
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "Gagal mengirim PendingIntent: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saat eksekusi reply PendingIntent: ${e.message}")
        }
    }
}
