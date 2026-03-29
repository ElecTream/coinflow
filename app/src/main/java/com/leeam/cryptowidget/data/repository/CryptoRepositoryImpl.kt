package com.leeam.cryptowidget.data.repository

import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.data.model.CryptoWidgetData
import com.leeam.cryptowidget.data.model.WalletType
import com.leeam.cryptowidget.data.model.XrplAccountParam
import com.leeam.cryptowidget.data.model.XrplRequest
import com.leeam.cryptowidget.data.remote.KrakenService
import com.leeam.cryptowidget.data.remote.XrplService
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

    override suspend fun fetchWidgetData(
        coinId: String,
        walletAddress: String
    ): Result<CryptoWidgetData> = runCatching {
        val coin = CoinRegistry.byId(coinId)

        // 1. Price + 24h change from Kraken
        val tickerResp = krakenService.getTicker(coin.krakenPair)
        if (tickerResp.error.isNotEmpty()) {
            throw RuntimeException("Kraken error: ${tickerResp.error.joinToString()}")
        }
        val tickerData = tickerResp.result.values.firstOrNull()
            ?: throw RuntimeException("No ticker data for ${coin.krakenPair}")

        val priceUsd = tickerData.c.firstOrNull()?.toDoubleOrNull() ?: 0.0
        val openPrice = tickerData.o.toDoubleOrNull() ?: 0.0
        val change = if (openPrice > 0.0) ((priceUsd - openPrice) / openPrice) * 100.0 else 0.0

        // 2. Sparkline — failure is isolated; doesn't block price display
        val (sparkline, sparklineTs) = runCatching {
            fetchSparklineInternal(coin.krakenPair)
        }.getOrDefault(Pair(emptyList(), emptyList()))

        // 3. Wallet balance — only fetched if the coin supports it and address is provided
        val (balance, valueUsd) = if (coin.walletType != WalletType.NONE && walletAddress.isNotBlank()) {
            runCatching {
                val bal = fetchWalletBalanceInternal(coin.walletType, walletAddress)
                Pair(bal, bal * priceUsd)
            }.getOrDefault(Pair(0.0, 0.0))
        } else {
            Pair(0.0, 0.0)
        }

        CryptoWidgetData(
            coinId = coinId,
            symbol = coin.symbol,
            priceUsd = priceUsd,
            change24hPct = change,
            sparklinePrices = sparkline,
            sparklineTimestamps = sparklineTs,
            walletAddress = walletAddress,
            walletBalance = balance,
            walletValueUsd = valueUsd,
            lastUpdatedMs = System.currentTimeMillis(),
            errorMessage = null
        )
    }

    override suspend fun fetchSparkline(coinId: String): Result<List<Double>> = runCatching {
        fetchSparklineInternal(CoinRegistry.byId(coinId).krakenPair).first
    }

    private suspend fun fetchSparklineInternal(krakenPair: String): Pair<List<Double>, List<Long>> {
        val ohlcResp = krakenService.getOhlc(krakenPair, interval = 60)
        if (ohlcResp.error.isNotEmpty()) {
            throw RuntimeException("Kraken OHLC error: ${ohlcResp.error.joinToString()}")
        }
        // result contains the pair key (e.g. "XXRPZUSD") and "last" — filter to the array
        val ohlcData = ohlcResp.result.entries
            .firstOrNull { it.key != "last" }
            ?.value?.jsonArray
            ?: return Pair(emptyList(), emptyList())

        // Each entry: [timestamp, open, high, low, close, vwap, volume, count]
        // Take last 24 entries, extract close price (index 4) and timestamp (index 0)
        val last24 = ohlcData.takeLast(24)
        val prices = last24.map { entry -> entry.jsonArray[4].jsonPrimitive.content.toDouble() }
        val timestamps = last24.map { entry -> entry.jsonArray[0].jsonPrimitive.content.toLong() }
        return Pair(prices, timestamps)
    }

    override suspend fun fetchWalletBalance(address: String): Result<Double> = runCatching {
        fetchWalletBalanceInternal(WalletType.XRPL, address)
    }

    private suspend fun fetchWalletBalanceInternal(walletType: WalletType, address: String): Double {
        return when (walletType) {
            WalletType.XRPL -> fetchXrplBalance(address)
            WalletType.NONE -> 0.0
        }
    }

    private suspend fun fetchXrplBalance(address: String): Double {
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
