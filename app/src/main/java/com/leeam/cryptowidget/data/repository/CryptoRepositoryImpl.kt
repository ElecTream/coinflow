package com.leeam.cryptowidget.data.repository

import com.leeam.cryptowidget.data.model.CryptoWidgetData
import com.leeam.cryptowidget.data.model.XrplAccountParam
import com.leeam.cryptowidget.data.model.XrplRequest
import com.leeam.cryptowidget.data.remote.KrakenService
import com.leeam.cryptowidget.data.remote.XrplService
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepositoryImpl @Inject constructor(
    private val krakenService: KrakenService,
    private val xrplService: XrplService
) : CryptoRepository {

    /** Map internal coin IDs to Kraken trading pair symbols */
    private fun toKrakenPair(coinId: String): String = when (coinId.lowercase()) {
        "ripple", "xrp" -> "XRPUSD"
        "bitcoin", "btc" -> "XBTUSD"
        "ethereum", "eth" -> "ETHUSD"
        else -> "${coinId.uppercase()}USD"
    }

    /** Map internal coin IDs to display symbols */
    private fun toDisplaySymbol(coinId: String): String = when (coinId.lowercase()) {
        "ripple" -> "XRP"
        "bitcoin" -> "BTC"
        "ethereum" -> "ETH"
        else -> coinId.uppercase()
    }

    override suspend fun fetchWidgetData(
        coinId: String,
        walletAddress: String
    ): Result<CryptoWidgetData> = runCatching {
        val pair = toKrakenPair(coinId)

        // 1. Price + 24h change from Kraken
        val tickerResp = krakenService.getTicker(pair)
        if (tickerResp.error.isNotEmpty()) {
            throw RuntimeException("Kraken error: ${tickerResp.error.joinToString()}")
        }
        val tickerData = tickerResp.result.values.firstOrNull()
            ?: throw RuntimeException("No ticker data for $pair")

        val priceUsd = tickerData.c.firstOrNull()?.toDoubleOrNull() ?: 0.0
        val openPrice = tickerData.o.toDoubleOrNull() ?: 0.0
        val change = if (openPrice > 0.0) ((priceUsd - openPrice) / openPrice) * 100.0 else 0.0

        // 2. Sparkline — failure is isolated; doesn't block price display
        val sparkline = runCatching {
            fetchSparklineInternal(pair)
        }.getOrDefault(emptyList())

        // 3. Wallet balance via XRPL — skip if address is blank
        val (balance, valueUsd) = if (walletAddress.isNotBlank()) {
            runCatching {
                val bal = fetchWalletBalanceInternal(walletAddress)
                Pair(bal, bal * priceUsd)
            }.getOrDefault(Pair(0.0, 0.0))
        } else {
            Pair(0.0, 0.0)
        }

        CryptoWidgetData(
            coinId = coinId,
            symbol = toDisplaySymbol(coinId),
            priceUsd = priceUsd,
            change24hPct = change,
            sparklinePrices = sparkline,
            walletAddress = walletAddress,
            walletBalanceXrp = balance,
            walletValueUsd = valueUsd,
            lastUpdatedMs = System.currentTimeMillis(),
            errorMessage = null
        )
    }

    override suspend fun fetchSparkline(coinId: String): Result<List<Double>> = runCatching {
        fetchSparklineInternal(toKrakenPair(coinId))
    }

    private suspend fun fetchSparklineInternal(pair: String): List<Double> {
        val ohlcResp = krakenService.getOhlc(pair, interval = 60)
        if (ohlcResp.error.isNotEmpty()) {
            throw RuntimeException("Kraken OHLC error: ${ohlcResp.error.joinToString()}")
        }
        // result contains the pair key (e.g. "XXRPZUSD") and "last" — filter to the array
        val ohlcData = ohlcResp.result.entries
            .firstOrNull { it.key != "last" }
            ?.value?.jsonArray
            ?: return emptyList()

        // Each entry: [timestamp, open, high, low, close, vwap, volume, count]
        // Take last 24 entries, extract close price (index 4)
        return ohlcData
            .takeLast(24)
            .map { entry ->
                val candle = entry.jsonArray
                candle[4].jsonPrimitive.content.toDouble()
            }
    }

    override suspend fun fetchWalletBalance(address: String): Result<Double> = runCatching {
        fetchWalletBalanceInternal(address)
    }

    private suspend fun fetchWalletBalanceInternal(address: String): Double {
        val trimmed = address.trim()
        val request = XrplRequest(
            method = "account_info",
            params = listOf(XrplAccountParam(account = trimmed))
        )
        val resp = try {
            xrplService.accountInfo(request)
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()?.take(300) ?: ""
            throw RuntimeException("XRPL HTTP ${e.code()}: $body")
        }
        if (resp.result.error != null) {
            val msg = when (resp.result.error) {
                "actNotFound" -> "Account not found — wallet may need 10 XRP to activate"
                "invalidParams" -> "Invalid address format"
                else -> resp.result.errorMessage ?: resp.result.error
            }
            throw RuntimeException(msg)
        }
        val drops = resp.result.accountData?.balance?.toLongOrNull() ?: 0L
        return drops / 1_000_000.0
    }
}
