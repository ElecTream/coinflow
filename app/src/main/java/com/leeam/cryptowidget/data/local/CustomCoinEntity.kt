package com.leeam.cryptowidget.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.leeam.cryptowidget.data.model.CoinDefinition
import com.leeam.cryptowidget.data.model.PriceSource
import com.leeam.cryptowidget.data.model.WalletConfig

/**
 * Persists a user-added (BYOC) coin in Room.
 *
 * Price source logic:
 *  - [krakenPair] non-null → [PriceSource.Kraken] (user picked from Kraken pair search)
 *  - [krakenPair] null → [PriceSource.GenericRest] (custom URL)
 *
 * Wallet logic:
 *  - [walletBalanceUrlTemplate] + [walletBalanceJsonPath] both non-null → [WalletConfig.GenericRest]
 *  - otherwise → [WalletConfig.None]
 */
@Entity(tableName = "custom_coins")
data class CustomCoinEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val displayName: String,
    // ── Price source ──────────────────────────────────────────────────────────
    /** Set when user chose a Kraken pair (e.g. "LINKUSD"). Null for GenericRest. */
    val krakenPair: String? = null,
    val customPriceUrl: String? = null,
    val customPriceJsonPath: String? = null,
    val customChangeUrl: String? = null,
    val customChangeJsonPath: String? = null,
    val customSparklineUrl: String? = null,
    /** Dot-notation path to a JSON array of price floats. */
    val customSparklineJsonPath: String? = null,
    // ── Wallet (optional) ─────────────────────────────────────────────────────
    /** URL template; `{address}` is replaced at runtime. */
    val walletBalanceUrlTemplate: String? = null,
    val walletBalanceJsonPath: String? = null,
    /** Divisor to convert from smallest unit (e.g. 1e18 for wei → ETH). */
    val walletBalanceDivisor: Double = 1.0,
    val createdAtMs: Long = System.currentTimeMillis()
)

/** Maps a stored [CustomCoinEntity] back to a runtime [CoinDefinition]. */
fun CustomCoinEntity.toCoinDefinition(): CoinDefinition {
    val priceSource = if (krakenPair != null) {
        PriceSource.Kraken(krakenPair)
    } else {
        PriceSource.GenericRest(
            priceUrl          = customPriceUrl ?: "",
            priceJsonPath     = customPriceJsonPath ?: "",
            changeUrl         = customChangeUrl,
            changeJsonPath    = customChangeJsonPath,
            sparklineUrl      = customSparklineUrl,
            sparklineJsonPath = customSparklineJsonPath
        )
    }

    val walletConfig = if (
        walletBalanceUrlTemplate != null && walletBalanceJsonPath != null
    ) {
        WalletConfig.GenericRest(
            balanceUrlTemplate = walletBalanceUrlTemplate,
            balanceJsonPath    = walletBalanceJsonPath,
            divisor            = walletBalanceDivisor
        )
    } else {
        WalletConfig.None
    }

    return CoinDefinition(
        id          = id,
        symbol      = symbol,
        displayName = displayName,
        priceSource = priceSource,
        walletConfig = walletConfig
    )
}
