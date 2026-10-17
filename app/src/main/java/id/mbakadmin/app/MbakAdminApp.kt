package id.mbakadmin.app

import android.app.Application
import id.mbakadmin.app.data.local.MbakAdminDatabase

class MbakAdminApp : Application() {
    val database: MbakAdminDatabase by lazy {
        MbakAdminDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Otomatis aktifkan Guardian Foreground Service jika izin notification listener sudah diberikan
        if (id.mbakadmin.app.ui.isNotificationServiceEnabled(this)) {
            id.mbakadmin.app.service.MbakAdminGuardianService.startService(this)
        }
    }
}
