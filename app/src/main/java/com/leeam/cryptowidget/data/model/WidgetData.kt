package com.leeam.cryptowidget.data.model

data class WidgetData(
    val coinId: String = "ripple",
    val symbol: String = "",
    val priceUsd: Double = 0.0,
    val change24hPct: Double = 0.0,
    val sparklinePrices: List<Double> = emptyList(),
    val sparklineTimestamps: List<Long> = emptyList(),
    val walletAddress: String = "",
    val walletBalance: Double = 0.0,
    val walletValueUsd: Double = 0.0,
    val lastUpdatedMs: Long = 0L,
    val errorMessage: String? = null
)
