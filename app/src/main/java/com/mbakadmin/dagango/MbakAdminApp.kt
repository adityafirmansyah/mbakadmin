package com.mbakadmin.dagango

import android.app.Application
import com.mbakadmin.dagango.data.local.MbakAdminDatabase
import com.mbakadmin.dagango.util.CrashLogger

class MbakAdminApp : Application() {
    val database: MbakAdminDatabase by lazy {
        MbakAdminDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        // 1. Inisialisasi crash logger global agar setiap crash fatal tertulis ke disk
        CrashLogger.init(this)

        // 2. Aktifkan Guardian Foreground Service jika izin notification listener sudah diberikan
        try {
            if (com.mbakadmin.dagango.ui.isNotificationServiceEnabled(this)) {
                com.mbakadmin.dagango.service.MbakAdminGuardianService.startService(this)
            }
        } catch (e: Exception) {
            CrashLogger.logHandledException(this, "MbakAdminApp", "Gagal start GuardianService saat app init", e)
        }
    }
}
