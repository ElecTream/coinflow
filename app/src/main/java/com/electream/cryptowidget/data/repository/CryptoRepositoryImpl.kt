package com.electream.cryptowidget.data.repository

import com.electream.cryptowidget.data.model.CoinDefinition
import com.electream.cryptowidget.data.model.CoinRegistry
import com.electream.cryptowidget.data.model.PriceSource
import com.electream.cryptowidget.data.model.WalletConfig
import com.electream.cryptowidget.data.model.WidgetData
import com.electream.cryptowidget.data.model.XrplAccountParam
import com.electream.cryptowidget.data.model.XrplRequest
import com.electream.cryptowidget.data.remote.BitcoinService
import com.electream.cryptowidget.data.remote.EthereumService
import com.electream.cryptowidget.data.remote.EthRpcRequest
import com.electream.cryptowidget.data.remote.GenericRestService
import com.electream.cryptowidget.data.remote.KrakenService
import com.electream.cryptowidget.data.remote.SolanaService
import com.electream.cryptowidget.data.remote.SolRpcRequest
import com.electream.cryptowidget.data.remote.XrplService
import com.electream.cryptowidget.data.remote.btcBalance
import com.electream.cryptowidget.data.remote.ethBalance
import com.electream.cryptowidget.data.remote.solBalance
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepositoryImpl @Inject constructor(
    private val krakenService: KrakenService,
    private val xrplService: XrplService,
    private val bitcoinService: BitcoinService,
    private val ethereumService: EthereumService,
    private val solanaService: SolanaService,
    private val genericRestService: GenericRestService,
    private val coinRepository: CoinRepository
) : CryptoRepository {

    override suspend fun fetchWidgetData(
        coinId: String,
        walletAddress: String
    ): Result<WidgetData> = runCatching {
        val coin = coinRepository.coinById(coinId) ?: CoinRegistry.byId(coinId)

        // 1. Price + 24h change — dispatches on PriceSource type
        val (priceUsd, change) = fetchPrice(coin)

        // 2. Sparkline — failure is isolated; doesn't block price display
        val (sparkline, sparklineTs) = runCatching {
            fetchSparklineForCoin(coin)
        }.getOrDefault(Pair(emptyList(), emptyList()))

        // 3. Wallet balance — only fetched if coin supports it and address is provided
        val (balance, valueUsd) = if (coin.walletConfig !is WalletConfig.None && walletAddress.isNotBlank()) {
            runCatching {
                val bal = fetchWalletBalanceInternal(coin.walletConfig, walletAddress)
                Pair(bal, bal * priceUsd)
            }.getOrDefault(Pair(0.0, 0.0))
        } else {
            Pair(0.0, 0.0)
        }

        WidgetData(
            coinId              = coinId,
            symbol              = coin.symbol,
            priceUsd            = priceUsd,
            change24hPct        = change,
            sparklinePrices     = sparkline,
            sparklineTimestamps = sparklineTs,
            walletAddress       = walletAddress,
            walletBalance       = balance,
            walletValueUsd      = valueUsd,
            lastUpdatedMs       = System.currentTimeMillis(),
            errorMessage        = null
        )
    }

    override suspend fun fetchSparkline(coinId: String): Result<List<Double>> = runCatching {
        val coin = coinRepository.coinById(coinId) ?: CoinRegistry.byId(coinId)
        fetchSparklineForCoin(coin).first
    }

    override suspend fun fetchWalletBalance(coinId: String, address: String): Result<Double> = runCatching {
        val coin = coinRepository.coinById(coinId) ?: CoinRegistry.byId(coinId)
        fetchWalletBalanceInternal(coin.walletConfig, address)
    }

    // ── Price dispatch ────────────────────────────────────────────────────────

    private suspend fun fetchPrice(coin: CoinDefinition): Pair<Double, Double> {
        return when (val src = coin.priceSource) {
            is PriceSource.Kraken      -> fetchKrakenTicker(src.pair)
            is PriceSource.GenericRest -> fetchGenericPrice(src)
        }
    }

    private suspend fun fetchKrakenTicker(pair: String): Pair<Double, Double> {
        val tickerResp = krakenService.getTicker(pair)
        if (tickerResp.error.isNotEmpty()) {
            throw RuntimeException("Kraken error: ${tickerResp.error.joinToString()}")
        }
        val tickerData = tickerResp.result.values.firstOrNull()
            ?: throw RuntimeException("No ticker data for $pair")
        val priceUsd = tickerData.c.firstOrNull()?.toDoubleOrNull() ?: 0.0
        val openPrice = tickerData.o.toDoubleOrNull() ?: 0.0
        val change = if (openPrice > 0.0) ((priceUsd - openPrice) / openPrice) * 100.0 else 0.0
        return Pair(priceUsd, change)
    }

    private suspend fun fetchGenericPrice(src: PriceSource.GenericRest): Pair<Double, Double> {
        val price = genericRestService.fetchDouble(src.priceUrl, src.priceJsonPath)
        val change = if (src.changeUrl != null && src.changeJsonPath != null) {
            runCatching {
                genericRestService.fetchDouble(src.changeUrl, src.changeJsonPath)
            }.getOrDefault(0.0)
        } else 0.0
        return Pair(price, change)
    }

    // ── Sparkline dispatch ────────────────────────────────────────────────────

    private suspend fun fetchSparklineForCoin(coin: CoinDefinition): Pair<List<Double>, List<Long>> {
        return when (val src = coin.priceSource) {
            is PriceSource.Kraken      -> fetchSparklineInternal(src.pair)
            is PriceSource.GenericRest -> fetchGenericSparkline(src)
        }
    }

    private suspend fun fetchSparklineInternal(krakenPair: String): Pair<List<Double>, List<Long>> {
        val ohlcResp = krakenService.getOhlc(krakenPair, interval = 60)
        if (ohlcResp.error.isNotEmpty()) {
            throw RuntimeException("Kraken OHLC error: ${ohlcResp.error.joinToString()}")
        }
        val ohlcData = ohlcResp.result.entries
            .firstOrNull { it.key != "last" }
            ?.value?.jsonArray
            ?: return Pair(emptyList(), emptyList())
        val last24 = ohlcData.takeLast(24)
        val prices = last24.map { it.jsonArray[4].jsonPrimitive.content.toDouble() }
        val timestamps = last24.map { it.jsonArray[0].jsonPrimitive.content.toLong() }
        return Pair(prices, timestamps)
    }

    private suspend fun fetchGenericSparkline(src: PriceSource.GenericRest): Pair<List<Double>, List<Long>> {
        if (src.sparklineUrl == null || src.sparklineJsonPath == null) return Pair(emptyList(), emptyList())
        val prices = genericRestService.fetchDoubleList(src.sparklineUrl, src.sparklineJsonPath)
        return Pair(prices, emptyList()) // GenericRest sparklines have no timestamps
    }

    // ── Wallet balance dispatch ───────────────────────────────────────────────

    private suspend fun fetchWalletBalanceInternal(walletConfig: WalletConfig, address: String): Double {
        return when (walletConfig) {
            is WalletConfig.None        -> 0.0
            is WalletConfig.Xrpl        -> fetchXrplBalance(address)
            is WalletConfig.Bitcoin     -> fetchBitcoinBalance(address)
            is WalletConfig.Ethereum    -> fetchEthereumBalance(address)
            is WalletConfig.Solana      -> fetchSolanaBalance(address)
            is WalletConfig.GenericRest -> fetchGenericBalance(walletConfig, address)
        }
    }

    private suspend fun fetchXrplBalance(address: String): Double {
        val request = XrplRequest(
            method = "account_info",
            params = listOf(XrplAccountParam(account = address.trim()))
        )
        val resp = try {
            xrplService.accountInfo(request)
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()?.take(300) ?: ""
            throw RuntimeException("XRPL HTTP ${e.code()}: $body")
        }
        if (resp.result.error != null) {
            val msg = when (resp.result.error) {
                "actNotFound"   -> "Account not found — wallet may need a minimum balance to activate"
                "invalidParams" -> "Invalid address format"
                else -> resp.result.errorMessage ?: resp.result.error
            }
            throw RuntimeException(msg)
        }
        val drops = resp.result.accountData?.balance?.toLongOrNull() ?: 0L
        return drops / 1_000_000.0
    }

    private suspend fun fetchBitcoinBalance(address: String): Double =
        bitcoinService.getAddressInfo(address.trim()).btcBalance()

    private suspend fun fetchEthereumBalance(address: String): Double {
        val request = EthRpcRequest(params = listOf(address.trim(), "latest"))
        val response = ethereumService.getBalance(request)
        if (response.error != null) throw RuntimeException("ETH RPC error: ${response.error.message}")
        return response.ethBalance()
    }

    private suspend fun fetchSolanaBalance(address: String): Double {
        val request = SolRpcRequest(params = listOf(address.trim()))
        val response = solanaService.getBalance(request)
        if (response.error != null) throw RuntimeException("Solana RPC error")
        return response.solBalance()
    }

    private suspend fun fetchGenericBalance(config: WalletConfig.GenericRest, address: String): Double {
        val url = config.balanceUrlTemplate.replace("{address}", address.trim())
        val raw = genericRestService.fetchDouble(url, config.balanceJsonPath)
        return if (config.divisor != 0.0) raw / config.divisor else raw
    }
}
