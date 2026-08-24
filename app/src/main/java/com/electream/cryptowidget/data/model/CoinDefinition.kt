package com.electream.cryptowidget.data.model

/**
 * The data source used to fetch price, 24h change, and sparkline for a coin.
 * Built-in coins use [Kraken]. User-added (BYOC) coins may use [Kraken] (with a user-supplied
 * pair string) or [GenericRest] for exchanges not on Kraken.
 */
sealed class PriceSource {
    /** Uses the built-in KrakenService. Supports price, 24h change, and OHLC sparkline. */
    data class Kraken(val pair: String) : PriceSource()

    /**
     * Generic REST endpoint with dot-notation JSON path extraction.
     * e.g., priceUrl = "https://api.binance.com/api/v3/ticker/price?symbol=LINKUSDT"
     *       priceJsonPath = "price"
     * Change and sparkline sources are optional; omitting them disables those features.
     */
    data class GenericRest(
        val priceUrl: String,
        val priceJsonPath: String,
        val changeUrl: String? = null,
        val changeJsonPath: String? = null,
        val sparklineUrl: String? = null,
        val sparklineJsonPath: String? = null,
    ) : PriceSource()
}

/**
 * The wallet balance source for a coin. Use [None] if balance tracking is unsupported.
 */
sealed class WalletConfig {
    /** No wallet balance tracking. */
    object None : WalletConfig()
    /** XRP Ledger — address starts with 'r', 25–34 chars, Base58Check. */
    object Xrpl : WalletConfig()
    /** Bitcoin — Blockstream.info API. Address: 1/3/bc1 prefix. */
    object Bitcoin : WalletConfig()
    /** Ethereum — ethereum-rpc.publicnode.com JSON-RPC (eth_getBalance). Address: 0x + 40 hex chars. */
    object Ethereum : WalletConfig()
    /** Solana — mainnet-beta.solana.com JSON-RPC (getBalance). Address: Base58, 32–44 chars. */
    object Solana : WalletConfig()
    /**
     * Custom REST wallet for BYOC coins.
     * [balanceUrlTemplate] contains `{address}` which is substituted at runtime.
     * [balanceJsonPath] is dot-notation to the balance field.
     * [divisor] converts from smallest unit to whole coins (e.g., 1e18 for wei → ETH).
     */
    data class GenericRest(
        val balanceUrlTemplate: String,
        val balanceJsonPath: String,
        val divisor: Double = 1.0
    ) : WalletConfig()
}

/**
 * Describes a supported cryptocurrency and how to fetch and display it.
 *
 * Built-in coins are registered in [CoinRegistry.all]. User-added (BYOC) coins are stored in
 * Room (CustomCoinEntity) and merged into the available list at runtime via CoinRepository.
 *
 * Adding a built-in coin: add one entry to [CoinRegistry.all] — no other file needs changing
 * for price and sparkline fetching. Wallet support is configured via [walletConfig].
 */
data class CoinDefinition(
    /** Stable internal ID (lowercase). Stored in DataStore and Room. Never change after first use. */
    val id: String,
    /** Display ticker shown in the widget and UI, e.g. "BTC". */
    val symbol: String,
    /** Full display name shown in settings, e.g. "Bitcoin". */
    val displayName: String,
    /** How to fetch price, 24h change, and sparkline data. */
    val priceSource: PriceSource,
    /** How to fetch wallet balance, or [WalletConfig.None] if unsupported. */
    val walletConfig: WalletConfig = WalletConfig.None
) {
    init {
        require(id.matches(ID_REGEX)) {
            "Coin id must be lowercase kebab/snake-case (matched ${ID_REGEX.pattern}): '$id'"
        }
        require(symbol.isNotBlank()) { "symbol blank for coin id '$id'" }
        require(displayName.isNotBlank()) { "displayName blank for coin id '$id'" }
    }

    companion object {
        private val ID_REGEX = Regex("^[a-z0-9_-]+$")
    }
}

object CoinRegistry {
    val all: List<CoinDefinition> = listOf(
        CoinDefinition(
            id           = "bitcoin",
            symbol       = "BTC",
            displayName  = "Bitcoin",
            priceSource  = PriceSource.Kraken("XBTUSD"),
            walletConfig = WalletConfig.Bitcoin
        ),
        CoinDefinition(
            id           = "ethereum",
            symbol       = "ETH",
            displayName  = "Ethereum",
            priceSource  = PriceSource.Kraken("ETHUSD"),
            walletConfig = WalletConfig.Ethereum
        ),
        CoinDefinition(
            id           = "solana",
            symbol       = "SOL",
            displayName  = "Solana",
            priceSource  = PriceSource.Kraken("SOLUSD"),
            walletConfig = WalletConfig.Solana
        ),
        CoinDefinition(
            id           = "ripple",
            symbol       = "XRP",
            displayName  = "XRP",
            priceSource  = PriceSource.Kraken("XRPUSD"),
            walletConfig = WalletConfig.Xrpl
        ),
    )

    init {
        val ids = all.map { it.id }
        require(ids.distinct().size == ids.size) {
            "Duplicate coin ids in CoinRegistry: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}"
        }
    }

    private val byIdMap: Map<String, CoinDefinition> = all.associateBy { it.id }

    /**
     * Resolves [id] to a [CoinDefinition].
     *
     * Falls back to [firstRegistered] if the id is unknown — a recovery aid for stale
     * preferences pointing at a removed coin, NOT a user-facing default. Code that needs
     * to know "did the user pick a coin?" should check `prefs.coinId.isEmpty()` instead
     * of comparing to this fallback.
     */
    fun byId(id: String): CoinDefinition = byIdMap[id] ?: firstRegistered

    /** Last-resort fallback when an id can't be resolved. Not a user default — see [byId] kdoc. */
    internal val firstRegistered: CoinDefinition = all.first()
}
