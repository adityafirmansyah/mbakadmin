package com.mbakadmin.dagango.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "Toko Saya",
    val adminName: String = "Mbak Admin",
    val storeGreeting: String = "Halo kak! Selamat datang di toko kami.",
    val paymentInfo: String = "BCA: 1234567890 a/n Toko Saya\nMandiri: 9876543210 a/n Toko Saya",
    val shippingInfo: String = "Pengiriman dari Jakarta Pusat via JNE, J&T, SiCepat.",
    val botTone: String = "ramah, sopan, persuasif ala admin olshop indonesia, gunakan panggilan 'kak'",
    val isAutoReplyActive: Boolean = true,
    val aiApiBaseUrl: String = DEFAULT_AI_BASE_URL,
    val aiApiKey: String = DEFAULT_AI_API_KEY,
    val aiModel: String = DEFAULT_AI_MODEL,
    val aiTimeoutSeconds: Int = 60,
    val fallbackReplyMessage: String = "Halo kak! Pesan sudah {admin} terima ya, mohon ditunggu sebentar biar segera dicek manual oleh tim kami 🙏",
    val defaultShippingFee: Double = 15000.0,
    val orderFormatTemplate: String = "Nama Penerima:\nNo. HP/WA:\nPesanan & Varian:\nJumlah:\nAlamat Lengkap:\nKecamatan & Kota:\nKode Pos:",
    val targetWhatsAppPackage: String = "ALL", // "ALL", "WHATSAPP_BUSINESS", "WHATSAPP_STANDARD"
    val onlyReplyUnknownContacts: Boolean = false
) {
    companion object {
        const val DEFAULT_AI_BASE_URL = "https://route.aditf.com/v1"
        const val DEFAULT_AI_API_KEY = "sk-45c8c821115179e9-eeg53l-9e20414c"
        const val DEFAULT_AI_MODEL = "hermes-combo"
    }
}
