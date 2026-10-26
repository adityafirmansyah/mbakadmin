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
        // Inisialisasi crash logger global sedini mungkin agar setiap crash fatal
        // (termasuk yang terjadi selama app init) tertulis ke disk.
        //
        // PENTING: JANGAN memulai MbakAdminGuardianService (foreground service)
        // dari Application.onCreate. Proses app bisa di-bind oleh OS dari background
        // (cold start saat boot, notification listener bind, dll) sehingga Android 12+
        // melempar ForegroundServiceStartNotAllowedException (mAllowStartForeground=false).
        // Guardian Service justru di-start dari MainActivity ON_RESUME saat activity
        // benar-benar terlihat (visibilitas window = start diizinkan).
        CrashLogger.init(this)
    }
}
