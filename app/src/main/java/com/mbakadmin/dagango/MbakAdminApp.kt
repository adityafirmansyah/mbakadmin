package com.mbakadmin.dagango

import android.app.Application
import com.mbakadmin.dagango.data.local.MbakAdminDatabase

class MbakAdminApp : Application() {
    val database: MbakAdminDatabase by lazy {
        MbakAdminDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Otomatis aktifkan Guardian Foreground Service jika izin notification listener sudah diberikan
        if (com.mbakadmin.dagango.ui.isNotificationServiceEnabled(this)) {
            com.mbakadmin.dagango.service.MbakAdminGuardianService.startService(this)
        }
    }
}
