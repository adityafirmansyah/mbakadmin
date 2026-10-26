package com.mbakadmin.dagango.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.mbakadmin.dagango.ui.MainActivity

/**
 * Foreground Service persisten untuk memastikan proses MbakAdmin
 * memiliki prioritas eksekusi tinggi dan tidak dibunuh oleh OS saat low memory.
 */
class MbakAdminGuardianService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notification = buildPersistentNotification()
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            com.mbakadmin.dagango.util.CrashLogger.logHandledException(
                this,
                "MbakAdminGuardianService",
                "Fatal error in onStartCommand startForeground: ${e.message}",
                e
            )
        }

        // START_STICKY: meminta OS untuk me-restart service jika terpaksa di-kill
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildPersistentNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MbakAdmin Sedang Berjaga 🤖")
            .setContentText("Asisten AI aktif memantau & membalas chat WhatsApp toko")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MbakAdmin Status Berjaga",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status berjalan latar belakang MbakAdmin"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "mbakadmin_guardian_channel"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            try {
                val intent = Intent(context, MbakAdminGuardianService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                com.mbakadmin.dagango.util.CrashLogger.logHandledException(
                    context,
                    "MbakAdminGuardianService",
                    "Gagal memulai Guardian Foreground Service: ${e.message}",
                    e
                )
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, MbakAdminGuardianService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                com.mbakadmin.dagango.util.CrashLogger.logHandledException(
                    context,
                    "MbakAdminGuardianService",
                    "Gagal menghentikan Guardian Service: ${e.message}",
                    e
                )
            }
        }
    }
}
