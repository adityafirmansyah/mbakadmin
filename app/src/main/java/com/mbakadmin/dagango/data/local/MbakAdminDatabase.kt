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
    version = 9,
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

        /**
         * Migrasi aman dari Versi 3 ke Versi 4:
         * Menambahkan kolom fallbackReplyMessage untuk kustomisasi pesan balasan cadangan toko.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL(
                        "ALTER TABLE store_settings ADD COLUMN fallbackReplyMessage TEXT NOT NULL DEFAULT 'Halo kak! Pesan sudah {admin} terima ya, mohon ditunggu sebentar biar segera dicek manual oleh tim kami 🙏'"
                    )
                } catch (_: Exception) {
                    // Kolom sudah ada
                }
            }
        }

        /**
         * Migrasi aman dari Versi 4 ke Versi 5:
         * Menambahkan defaultShippingFee, orderFormatTemplate, isPaymentClaimed, isVerifiedByHuman.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE store_settings ADD COLUMN defaultShippingFee REAL NOT NULL DEFAULT 15000.0")
                } catch (_: Exception) {}
                try {
                    db.execSQL(
                        "ALTER TABLE store_settings ADD COLUMN orderFormatTemplate TEXT NOT NULL DEFAULT 'Nama Penerima:\nNo. HP/WA:\nAlamat Lengkap:\nKecamatan & Kota:\nPesanan & Varian:\nJumlah:'"
                    )
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE chat_logs ADD COLUMN isPaymentClaimed INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE chat_logs ADD COLUMN isVerifiedByHuman INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        /**
         * Migrasi aman dari Versi 5 ke Versi 6:
         * Menambahkan trackingNumber, courierName, orderStatus, customerPhone untuk fulfillment.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE chat_logs ADD COLUMN trackingNumber TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE chat_logs ADD COLUMN courierName TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE chat_logs ADD COLUMN orderStatus TEXT NOT NULL DEFAULT 'PENDING'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE chat_logs ADD COLUMN customerPhone TEXT NOT NULL DEFAULT ''")
                } catch (_: Exception) {}
            }
        }

        /**
         * Migrasi aman dari Versi 6 ke Versi 7:
         * Menambahkan targetWhatsAppPackage dan onlyReplyUnknownContacts untuk filter privasi WhatsApp.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE store_settings ADD COLUMN targetWhatsAppPackage TEXT NOT NULL DEFAULT 'ALL'")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE store_settings ADD COLUMN onlyReplyUnknownContacts INTEGER NOT NULL DEFAULT 0")
                } catch (_: Exception) {}
            }
        }

        /**
         * Migrasi aman dari Versi 7 ke Versi 8:
         * Memindahkan 'Alamat Lengkap' ke baris terakhir pada template format pemesanan default toko.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL(
                        """
                        UPDATE store_settings 
                        SET orderFormatTemplate = 'Nama Penerima:\nNo. HP/WA:\nKecamatan & Kota:\nPesanan & Varian:\nJumlah:\nAlamat Lengkap:'
                        WHERE orderFormatTemplate = 'Nama Penerima:\nNo. HP/WA:\nAlamat Lengkap:\nKecamatan & Kota:\nPesanan & Varian:\nJumlah:'
                        """.trimIndent()
                    )
                } catch (_: Exception) {}
            }
        }

        /**
         * Migrasi aman dari Versi 8 ke Versi 9:
         * Menempatkan 'Kecamatan & Kota' setelah 'Alamat Lengkap' pada template pemesanan toko.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL(
                        """
                        UPDATE store_settings 
                        SET orderFormatTemplate = 'Nama Penerima:\nNo. HP/WA:\nPesanan & Varian:\nJumlah:\nAlamat Lengkap:\nKecamatan & Kota:'
                        WHERE orderFormatTemplate = 'Nama Penerima:\nNo. HP/WA:\nKecamatan & Kota:\nPesanan & Varian:\nJumlah:\nAlamat Lengkap:'
                           OR orderFormatTemplate = 'Nama Penerima:\nNo. HP/WA:\nAlamat Lengkap:\nKecamatan & Kota:\nPesanan & Varian:\nJumlah:'
                        """.trimIndent()
                    )
                } catch (_: Exception) {}
            }
        }

        fun getInstance(context: Context): MbakAdminDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MbakAdminDatabase::class.java,
                    "mbakadmin.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
