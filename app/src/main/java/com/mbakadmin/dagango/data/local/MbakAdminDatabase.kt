package com.mbakadmin.dagango.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mbakadmin.dagango.data.local.dao.ChatLogDao
import com.mbakadmin.dagango.data.local.dao.ProductDao
import com.mbakadmin.dagango.data.local.dao.StoreSettingsDao
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity

@Database(
    entities = [ProductEntity::class, StoreSettingsEntity::class, ChatLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MbakAdminDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun storeSettingsDao(): StoreSettingsDao
    abstract fun chatLogDao(): ChatLogDao

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
