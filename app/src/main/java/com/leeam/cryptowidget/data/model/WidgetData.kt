package com.leeam.cryptowidget.data.model

data class CryptoWidgetData(
    val coinId: String = "ripple",
    val symbol: String = "XRP",
    val priceUsd: Double = 0.0,
    val change24hPct: Double = 0.0,
    val sparklinePrices: List<Double> = emptyList(),
    val walletAddress: String = "",
    val walletBalanceXrp: Double = 0.0,
    val walletValueUsd: Double = 0.0,
    val lastUpdatedMs: Long = 0L,
    val errorMessage: String? = null
)
