package id.mbakadmin.app.data.local.dao

import androidx.room.*
import id.mbakadmin.app.data.local.entity.ProductEntity
import id.mbakadmin.app.data.local.entity.StoreSettingsEntity
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
    fun getRecentChatLogs(): Flow<List<id.mbakadmin.app.data.local.entity.ChatLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatLog(chatLog: id.mbakadmin.app.data.local.entity.ChatLogEntity): Long

    @Query("DELETE FROM chat_logs")
    suspend fun clearChatLogs()
}
