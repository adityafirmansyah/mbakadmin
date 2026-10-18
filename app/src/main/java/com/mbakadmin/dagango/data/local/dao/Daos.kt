package com.mbakadmin.dagango.data.local.dao

import androidx.room.*
import com.mbakadmin.dagango.data.local.entity.ProductEntity
import com.mbakadmin.dagango.data.local.entity.StoreSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1")
    suspend fun getActiveProducts(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)
}

@Dao
interface StoreSettingsDao {
    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<StoreSettingsEntity?>

    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): StoreSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: StoreSettingsEntity)
}

@Dao
interface ChatLogDao {
    @Query("SELECT * FROM chat_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentChatLogs(): Flow<List<com.mbakadmin.dagango.data.local.entity.ChatLogEntity>>

    @Query("SELECT * FROM chat_logs WHERE senderName = :senderName ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessagesForSender(senderName: String, limit: Int = 6): List<com.mbakadmin.dagango.data.local.entity.ChatLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatLog(chatLog: com.mbakadmin.dagango.data.local.entity.ChatLogEntity): Long

    @Query("UPDATE chat_logs SET isVerifiedByHuman = :isVerified WHERE id = :id")
    suspend fun updateVerificationStatus(id: Long, isVerified: Boolean)

    @Query("DELETE FROM chat_logs")
    suspend fun clearChatLogs()
}
