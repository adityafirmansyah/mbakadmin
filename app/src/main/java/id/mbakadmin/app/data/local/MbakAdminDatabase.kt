package id.mbakadmin.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import id.mbakadmin.app.data.local.dao.ProductDao
import id.mbakadmin.app.data.local.dao.StoreSettingsDao
import id.mbakadmin.app.data.local.entity.ProductEntity
import id.mbakadmin.app.data.local.entity.StoreSettingsEntity

@Database(
    entities = [ProductEntity::class, StoreSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MbakAdminDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun storeSettingsDao(): StoreSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: MbakAdminDatabase? = null

        fun getInstance(context: Context): MbakAdminDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MbakAdminDatabase::class.java,
                    "mbakadmin.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
