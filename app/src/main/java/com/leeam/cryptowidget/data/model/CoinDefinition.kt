package com.leeam.cryptowidget.data.model

/**
 * Describes a supported cryptocurrency and how to fetch/display it.
 *
 * To add a new coin: add one entry to [CoinRegistry.all]. No other file needs to change
 * for price fetching. If the coin has a wallet (non-NONE WalletType), also add a provider
 * branch in CryptoRepositoryImpl.fetchWalletBalance().
 */
data class CoinDefinition(
    /** Stable internal ID (lowercase). Stored in DataStore and Room. */
    val id: String,
    /** Display ticker shown in the widget and UI. */
    val symbol: String,
    /** Kraken trading pair for ticker + OHLC calls. */
    val krakenPair: String,
    /** Which blockchain wallet this coin uses, or NONE if balance tracking is unsupported. */
    val walletType: WalletType
)

enum class WalletType {
    /** XRP Ledger — uses XrplService. Address format: starts with 'r', 25–34 chars, Base58Check. */
    XRPL,
    /** No wallet balance tracking for this coin. */
    NONE
    // Future: ETHEREUM, SOLANA, BITCOIN, etc.
}

object CoinRegistry {
    val all: List<CoinDefinition> = listOf(
        CoinDefinition(id = "ripple",   symbol = "XRP",  krakenPair = "XRPUSD",  walletType = WalletType.XRPL),
        CoinDefinition(id = "bitcoin",  symbol = "BTC",  krakenPair = "XBTUSD",  walletType = WalletType.NONE),
        CoinDefinition(id = "ethereum", symbol = "ETH",  krakenPair = "ETHUSD",  walletType = WalletType.NONE),
        CoinDefinition(id = "solana",   symbol = "SOL",  krakenPair = "SOLUSD",  walletType = WalletType.NONE),
    )

    private val byIdMap: Map<String, CoinDefinition> = all.associateBy { it.id }

    /** Returns the CoinDefinition for [id], or XRP as a safe fallback. */
    fun byId(id: String): CoinDefinition = byIdMap[id] ?: all.first()

    /** Default coin shown on first install. */
    val default: CoinDefinition = all.first()
}
