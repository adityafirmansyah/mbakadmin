package com.mbakadmin.dagango.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "Umum",
    val price: Double,
    val stock: Int = 10,
    val description: String = "",
    val variants: String = "", // JSON atau comma-separated: e.g. "Merah, Navy, Hitam" / "S, M, L, XL"
    val isActive: Boolean = true
)
