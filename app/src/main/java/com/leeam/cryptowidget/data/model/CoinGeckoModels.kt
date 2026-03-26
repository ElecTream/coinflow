package com.leeam.cryptowidget.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Response from: GET /api/v3/simple/price?ids=ripple&vs_currencies=usd&include_24hr_change=true
// Shape: { "ripple": { "usd": 2.31, "usd_24h_change": -1.45 } }
@Serializable
data class SimplePriceResponse(
    val ripple: RipplePriceData? = null
)

@Serializable
data class RipplePriceData(
    val usd: Double = 0.0,
    @SerialName("usd_24h_change") val usd24hChange: Double = 0.0
)

// Response from: GET /api/v3/coins/ripple/market_chart?vs_currency=usd&days=1&interval=hourly
// Shape: { "prices": [[timestamp_ms, price_usd], ...] }
@Serializable
data class MarketChartResponse(
    val prices: List<List<Double>> = emptyList(),
    @SerialName("market_caps") val marketCaps: List<List<Double>> = emptyList(),
    @SerialName("total_volumes") val totalVolumes: List<List<Double>> = emptyList()
)

@Serializable
data class CoinListItem(
    val id: String,
    val symbol: String,
    val name: String
)
