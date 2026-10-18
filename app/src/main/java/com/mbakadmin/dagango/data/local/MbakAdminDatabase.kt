package com.mbakadmin.dagango.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mbakadmin.dagango.data.local.dao.ChatLogDao
import com.mbakadmin.dagango.data.local.dao.ProductDao
import com.mbakadmin.dagango.data.local.dao.StoreSettingsDao
import com.mbakadmin.dagango.data.local.entity.ChatLogEntity
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity

@Database(
    entities = [ProductEntity::class, StoreSettingsEntity::class, ChatLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class MbakAdminDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun storeSettingsDao(): StoreSettingsDao
    abstract fun chatLogDao(): ChatLogDao

    companion object {
        @Volatile
        private var INSTANCE: MbakAdminDatabase? = null

        /**
         * Migrasi aman dari Versi 1 ke Versi 2:
         * Menambahkan kolom baru tanpa menghapus data produk, harga, varian, atau chat log pengguna.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Pastikan tabel chat_logs ada jika upgrade dari versi awal
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `incomingMessage` TEXT NOT NULL,
                        `aiReply` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // Cek kolom adminName sebelum menambah agar idempotent
                try {
                    db.execSQL("ALTER TABLE store_settings ADD COLUMN adminName TEXT NOT NULL DEFAULT 'Mbak Admin'")
                } catch (_: Exception) {
                    // Kolom sudah ada
                }

                // Cek kolom aiApiBaseUrl sebelum menambah agar idempotent
                try {
                    db.execSQL("ALTER TABLE store_settings ADD COLUMN aiApiBaseUrl TEXT NOT NULL DEFAULT 'https://api.openai.com/v1'")
                } catch (_: Exception) {
                    // Kolom sudah ada
                }
            }
        }

        /**
         * Migrasi aman dari Versi 2 ke Versi 3:
         * Menambahkan kolom aiTimeoutSeconds.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE store_settings ADD COLUMN aiTimeoutSeconds INTEGER NOT NULL DEFAULT 60")
                } catch (_: Exception) {
                    // Kolom sudah ada
                }
            }
        }

        fun getInstance(context: Context): MbakAdminDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MbakAdminDatabase::class.java,
                    "mbakadmin.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
