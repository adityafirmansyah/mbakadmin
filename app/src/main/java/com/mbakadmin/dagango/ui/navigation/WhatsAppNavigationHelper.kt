package com.mbakadmin.dagango.ui.navigation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import java.net.URLEncoder

object WhatsAppNavigationHelper {

    fun isNotificationServiceEnabled(context: Context): Boolean {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (flat != null && flat.isNotEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    return true
                }
            }
        }
        return false
    }

    fun openWhatsAppMessage(context: Context, phone: String, message: String, targetPackage: String = "ALL") {
        try {
            val encoded = URLEncoder.encode(message, "UTF-8")
            val cleanPhone = phone.filter { it.isDigit() }
            val finalPhone = if (cleanPhone.startsWith("0")) "62" + cleanPhone.substring(1) else cleanPhone
            val uri = if (finalPhone.isNotBlank()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$finalPhone&text=$encoded")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encoded")
            }

            val targetPkg = when (targetPackage) {
                "WHATSAPP_BUSINESS" -> "com.whatsapp.w4b"
                "WHATSAPP_STANDARD" -> "com.whatsapp"
                else -> {
                    // Jika terinstall WA Business, prioritaskan WA Business untuk toko
                    val pm = context.packageManager
                    val isW4bInstalled = try {
                        pm.getPackageInfo("com.whatsapp.w4b", 0)
                        true
                    } catch (_: Exception) {
                        false
                    }
                    if (isW4bInstalled) "com.whatsapp.w4b" else "com.whatsapp"
                }
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(targetPkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val encoded = URLEncoder.encode(message, "UTF-8")
                val fallbackUri = Uri.parse("https://api.whatsapp.com/send?text=$encoded")
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {
                Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
