package id.mbakadmin.app

import android.app.Application
import id.mbakadmin.app.data.local.MbakAdminDatabase

class MbakAdminApp : Application() {
    val database: MbakAdminDatabase by lazy {
        MbakAdminDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
