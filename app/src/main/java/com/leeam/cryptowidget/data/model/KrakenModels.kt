package com.leeam.cryptowidget.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Response from: GET https://api.kraken.com/0/public/Ticker?pair=XRPUSD
// Shape: { "error": [], "result": { "XXRPZUSD": { "c": ["1.3200","73.40"], "o": "1.3601", ... } } }
@Serializable
data class KrakenTickerResponse(
    val error: List<String> = emptyList(),
    val result: Map<String, KrakenTickerData> = emptyMap()
)

@Serializable
data class KrakenTickerData(
    val c: List<String> = emptyList(),   // last trade: [price, lot volume]
    val o: String = "0",                 // today's opening price
    val h: List<String> = emptyList(),   // high: [today, last 24h]
    val l: List<String> = emptyList()    // low: [today, last 24h]
)

// Response from: GET https://api.kraken.com/0/public/OHLC?pair=XRPUSD&interval=60
// Shape: { "error": [], "result": { "XXRPZUSD": [[ts,"open","high","low","close",...], ...], "last": 123 } }
@Serializable
data class KrakenOhlcResponse(
    val error: List<String> = emptyList(),
    val result: Map<String, JsonElement> = emptyMap()
)
