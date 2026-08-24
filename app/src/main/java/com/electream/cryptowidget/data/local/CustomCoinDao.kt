package com.electream.cryptowidget.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCoinDao {

    @Query("SELECT * FROM custom_coins ORDER BY createdAtMs ASC")
    fun getAllCustomCoins(): Flow<List<CustomCoinEntity>>

    @Query("SELECT * FROM custom_coins WHERE id = :id LIMIT 1")
    suspend fun getCustomCoinById(id: String): CustomCoinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCoin(coin: CustomCoinEntity)

    @Query("DELETE FROM custom_coins WHERE id = :id")
    suspend fun deleteCustomCoinById(id: String)
}
