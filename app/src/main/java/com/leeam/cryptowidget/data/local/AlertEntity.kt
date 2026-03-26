package com.leeam.cryptowidget.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "price_alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinId: String,
    val symbol: String,
    val direction: AlertDirection,
    val thresholdUsd: Double,
    val isEnabled: Boolean = true,
    val createdAtMs: Long = System.currentTimeMillis(),
    val firedAtMs: Long? = null
)
