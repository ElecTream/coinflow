package com.leeam.cryptowidget.data.repository

import com.leeam.cryptowidget.data.model.CryptoWidgetData
import com.leeam.cryptowidget.data.remote.CoinGeckoService
import com.leeam.cryptowidget.data.remote.XrpScanService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepositoryImpl @Inject constructor(
    private val coinGeckoService: CoinGeckoService,
    private val xrpScanService: XrpScanService
) : CryptoRepository {

    override suspend fun fetchWidgetData(
        coinId: String,
        walletAddress: String
    ): Result<CryptoWidgetData> = runCatching {
        // 1. Price + 24h change
        val priceResponse = coinGeckoService.getSimplePrice(
            ids = coinId,
            vsCurrencies = "usd",
            include24hrChange = true
        )
        val priceUsd = priceResponse.ripple?.usd ?: 0.0
        val change = priceResponse.ripple?.usd24hChange ?: 0.0

        // 2. Sparkline — failure is isolated; doesn't block price display
        val sparkline = runCatching {
            coinGeckoService.getMarketChart(coinId, "usd", days = 1, interval = "hourly")
                .prices
                .map { it[1] }
        }.getOrDefault(emptyList())

        // 3. Wallet balance — skip if address is blank
        val (balance, valueUsd) = if (walletAddress.isNotBlank()) {
            runCatching {
                val resp = xrpScanService.getAccount(walletAddress)
                if (resp.error != null) throw RuntimeException("XRPScan error: ${resp.error}")
                val bal = resp.xrpBalance.toDoubleOrNull() ?: 0.0
                Pair(bal, bal * priceUsd)
            }.getOrDefault(Pair(0.0, 0.0))
        } else {
            Pair(0.0, 0.0)
        }

        CryptoWidgetData(
            coinId = coinId,
            symbol = coinId.uppercase().let { if (it == "RIPPLE") "XRP" else it },
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
        coinGeckoService.getMarketChart(coinId, "usd", days = 1, interval = "hourly")
            .prices.map { it[1] }
    }

    override suspend fun fetchWalletBalance(address: String): Result<Double> = runCatching {
        val resp = xrpScanService.getAccount(address)
        if (resp.error != null) throw RuntimeException(resp.error)
        resp.xrpBalance.toDoubleOrNull() ?: 0.0
    }
}
