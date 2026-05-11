package com.leeam.cryptowidget.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Response from: GET https://api.kraken.com/0/public/Ticker?pair=XBTUSD
// Shape: { "error": [], "result": { "XXBTZUSD": { "c": ["67200.0","0.05"], "o": "66800.0", ... } } }
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

// Response from: GET https://api.kraken.com/0/public/OHLC?pair=<PAIR>&interval=60
// Shape: { "error": [], "result": { "<KEY>": [[ts,"open","high","low","close",...], ...], "last": 123 } }
@Serializable
data class KrakenOhlcResponse(
    val error: List<String> = emptyList(),
    val result: Map<String, JsonElement> = emptyMap()
)

// Response from: GET https://api.kraken.com/0/public/AssetPairs
// Shape: { "error": [], "result": { "XBTUSD": { "altname": "XBTUSD", "wsname": "XBT/USD", ... }, ... } }
@Serializable
data class KrakenAssetPairsResponse(
    val error: List<String> = emptyList(),
    val result: Map<String, KrakenPairInfo> = emptyMap()
)

@Serializable
data class KrakenPairInfo(
    val altname: String = "",
    val wsname: String = "",
    val base: String = "",
    val quote: String = ""
)
