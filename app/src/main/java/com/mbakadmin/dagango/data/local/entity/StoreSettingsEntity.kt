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
    val aiApiBaseUrl: String = "https://api.openai.com/v1",
    val aiApiKey: String = "",
    val aiModel: String = "gpt-4o-mini"
)
