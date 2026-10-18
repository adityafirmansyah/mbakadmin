package com.mbakadmin.dagango.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_logs")
data class ChatLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val incomingMessage: String,
    val aiReply: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPaymentClaimed: Boolean = false,
    val isVerifiedByHuman: Boolean = false,
    val trackingNumber: String = "",
    val courierName: String = "",
    val orderStatus: String = "PENDING", // PENDING, VERIFIED, SHIPPED
    val customerPhone: String = ""
)
