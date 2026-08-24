package com.electream.cryptowidget.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM price_alerts ORDER BY createdAtMs DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM price_alerts WHERE coinId = :coinId AND isEnabled = 1")
    suspend fun getEnabledAlertsForCoin(coinId: String): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Update
    suspend fun updateAlert(alert: AlertEntity)

    @Delete
    suspend fun deleteAlert(alert: AlertEntity)

    @Query("UPDATE price_alerts SET firedAtMs = :firedAtMs WHERE id = :id")
    suspend fun markAlertFired(id: Int, firedAtMs: Long)

    @Query("UPDATE price_alerts SET isEnabled = :enabled WHERE id = :id")
    suspend fun setAlertEnabled(id: Int, enabled: Boolean)

    @Query("UPDATE price_alerts SET lastKnownSide = :side WHERE id = :id")
    suspend fun updateLastKnownSide(id: Int, side: String)
}
