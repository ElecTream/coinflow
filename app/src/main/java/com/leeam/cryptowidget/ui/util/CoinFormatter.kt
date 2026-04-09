package com.leeam.cryptowidget.ui.util

import java.util.Locale
import kotlin.math.abs

/**
 * Centralised formatting for all coin price, amount, and value display.
 *
 * The guiding principle: show enough decimal places that a $0.01 change in the
 * USD value of a holding is visible, and that prices always read as meaningful
 * numbers regardless of coin tier.
 *
 * Usage:
 *   CoinFormatter.formatPrice(data.priceUsd)          → "$1.2345"  /  "$103,456.78"
 *   CoinFormatter.formatAmount(data.walletBalance, price) → "0.00094832"  /  "1234.567"
 *   CoinFormatter.formatValueUsd(data.walletValueUsd)  → "$94.83"
 *   CoinFormatter.formatChangePct(data.change24hPct)   → "+3.45%"
 */
object CoinFormatter {

    // ── Price of 1 unit in USD ────────────────────────────────────────────────

    /**
     * Formats the USD price of one unit of a coin.
     *
     * | Price tier   | Example output   |
     * |--------------|------------------|
     * | ≥ $10,000    | $103,456.78      |
     * | ≥ $1,000     | $3,456.78        |
     * | ≥ $1         | $1.2345          |
     * | ≥ $0.01      | $0.123456        |
     * | < $0.01      | $0.00012345      |
     */
    fun formatPrice(priceUsd: Double): String {
        if (priceUsd <= 0.0) return "$0.00"
        return when {
            priceUsd >= 10_000 -> "$%,.2f".format(priceUsd)
            priceUsd >= 1_000  -> "$%,.2f".format(priceUsd)
            priceUsd >= 1      -> "$%.4f".format(priceUsd)
            priceUsd >= 0.01   -> "$%.6f".format(priceUsd)
            else               -> "$%.8f".format(priceUsd)
        }
    }

    // ── Coin amount held (wallet balance display) ─────────────────────────────

    /**
     * Formats how many units of a coin are held, scaled so that a $0.01 move
     * in portfolio value is always visible.
     *
     * | Price tier   | Decimals | Example (0.001 BTC @ $100k) |
     * |--------------|----------|-----------------------------|
     * | ≥ $100,000   | 8        | 0.00100000                  |
     * | ≥ $10,000    | 6        | 0.001000                    |
     * | ≥ $1,000     | 5        | 0.00100                     |
     * | ≥ $100       | 4        | 0.0010                      |
     * | ≥ $1         | 3        | 1234.567                    |
     * | < $1         | 2        | 12345.67                    |
     */
    fun formatAmount(amount: Double, priceUsd: Double): String {
        val decimals = decimalsForAmount(priceUsd)
        return "%.${decimals}f".format(amount)
    }

    fun decimalsForAmount(priceUsd: Double): Int = when {
        priceUsd >= 100_000 -> 8
        priceUsd >= 10_000  -> 6
        priceUsd >= 1_000   -> 5
        priceUsd >= 100     -> 4
        priceUsd >= 1       -> 3
        else                -> 2
    }

    // ── USD portfolio / wallet value ──────────────────────────────────────────

    /**
     * Formats a USD value (portfolio total, wallet value, etc.).
     * Always shows cents for values ≥ $1; shows more precision for tiny values.
     */
    fun formatValueUsd(valueUsd: Double): String = when {
        valueUsd >= 1_000 -> "$%,.2f".format(valueUsd)
        valueUsd >= 1     -> "$%.2f".format(valueUsd)
        valueUsd >= 0.001 -> "$%.4f".format(valueUsd)
        else              -> "$%.6f".format(valueUsd)
    }

    // ── 24h change ────────────────────────────────────────────────────────────

    /** "+3.45%"  or  "-1.23%" */
    fun formatChangePct(pct: Double): String {
        val sign = if (pct >= 0) "+" else ""
        return "${sign}%.2f%%".format(pct)
    }

    // ── Widget-specific compact formats ───────────────────────────────────────

    /**
     * A shorter price for tight widget display.
     * Drops the thousands separator; caps at 6 significant characters after the "$".
     */
    fun formatPriceCompact(priceUsd: Double): String {
        if (priceUsd <= 0.0) return "$0.00"
        return when {
            priceUsd >= 10_000 -> "$%.0f".format(priceUsd)
            priceUsd >= 1_000  -> "$%.1f".format(priceUsd)
            priceUsd >= 1      -> "$%.4f".format(priceUsd)
            priceUsd >= 0.01   -> "$%.6f".format(priceUsd)
            else               -> "$%.8f".format(priceUsd)
        }
    }

    /**
     * Formats a balance + symbol for the widget's wallet row.
     * e.g. "0.00094832 BTC"  or  "1,234.567 XRP"
     */
    fun formatBalanceWithSymbol(amount: Double, priceUsd: Double, symbol: String): String {
        val formatted = formatAmount(amount, priceUsd)
        return "$formatted $symbol"
    }

    /**
     * Returns true if [priceUsd] is non-zero, used to guard display logic.
     */
    fun hasValidPrice(priceUsd: Double): Boolean = priceUsd > 0.0
}
