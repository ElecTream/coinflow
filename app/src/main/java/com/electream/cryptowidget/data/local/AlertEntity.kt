package com.electream.cryptowidget.data.local

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
    val firedAtMs: Long? = null,
    /** How this alert behaves after firing. Defaults to CROSSING. */
    val alertMode: AlertMode = AlertMode.CROSSING,
    /**
     * CROSSING mode: last observed price side relative to threshold ("ABOVE" / "BELOW").
     * Null on first check — alert won't fire until a second check establishes a crossing.
     */
    val lastKnownSide: String? = null,
    /** REPEATING mode only: minimum minutes between successive fires. */
    val cooldownMin: Int = 60
)
