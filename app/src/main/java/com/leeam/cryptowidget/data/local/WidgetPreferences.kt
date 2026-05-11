package com.leeam.cryptowidget.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.leeam.cryptowidget.data.model.CoinRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetDataStore by preferencesDataStore(name = "crypto_widget_prefs")

@Singleton
class WidgetPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.widgetDataStore

    object Keys {
        // ── Legacy / single-coin ────────────────────────────────────────────────
        val COIN_ID               = stringPreferencesKey("coin_id")
        val WALLET_ADDRESS_LEGACY = stringPreferencesKey("wallet_address")
        val REFRESH_INTERVAL_MIN  = intPreferencesKey("refresh_interval_min")
        val SHOW_SPARKLINE        = booleanPreferencesKey("show_sparkline")
        val SPARKLINE_DAYS        = intPreferencesKey("sparkline_days")
        val CHART_STYLE           = stringPreferencesKey("chart_style")
        val APP_THEME             = stringPreferencesKey("app_theme")
        val LAST_WORKER_RUN_MS    = longPreferencesKey("last_worker_run_ms")
        val LAST_ERROR_MSG        = stringPreferencesKey("last_error_msg")

        // ── Legacy single-coin cache (kept for backward compat on first upgrade) ─
        val CACHED_PRICE_USD      = doublePreferencesKey("cached_price_usd")
        val CACHED_CHANGE_PCT     = doublePreferencesKey("cached_change_pct")
        val CACHED_BALANCE        = doublePreferencesKey("cached_balance")
        val CACHED_UPDATED_MS     = longPreferencesKey("cached_updated_ms")
        val CACHED_SPARKLINE      = stringPreferencesKey("cached_sparkline")
        val CACHED_SPARKLINE_TS   = stringPreferencesKey("cached_sparkline_ts")

        // ── Multi-coin ──────────────────────────────────────────────────────────
        /** Comma-separated ordered list of all followed coin IDs. */
        val FOLLOWED_COIN_IDS     = stringPreferencesKey("followed_coin_ids")
        /** Comma-separated ordered list of coin IDs shown as widget tabs (max 5). */
        val WIDGET_COIN_IDS       = stringPreferencesKey("widget_coin_ids")

        val CUSTOM_ACCENT_ARGB    = intPreferencesKey("custom_accent_argb")
        val CUSTOM_SECONDARY_ARGB = intPreferencesKey("custom_secondary_argb")

        // ── Per-coin dynamic keys ───────────────────────────────────────────────
        fun walletAddressFor(coinId: String)  = stringPreferencesKey("wallet_address_$coinId")
        fun priceUsdFor(coinId: String)       = doublePreferencesKey("price_usd_$coinId")
        fun changePctFor(coinId: String)      = doublePreferencesKey("change_pct_$coinId")
        fun balanceFor(coinId: String)        = doublePreferencesKey("balance_$coinId")
        fun updatedMsFor(coinId: String)      = longPreferencesKey("updated_ms_$coinId")
        fun sparklineFor(coinId: String)      = stringPreferencesKey("sparkline_$coinId")
        fun sparklineTsFor(coinId: String)    = stringPreferencesKey("sparkline_ts_$coinId")
        fun lastFetchedMsFor(coinId: String)  = longPreferencesKey("last_fetched_$coinId")
    }

    // ── Single-coin compat ──────────────────────────────────────────────────────

    /** Active widget coin — maps to the legacy coin_id key for full backward compat. */
    val coinId: Flow<String> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.COIN_ID] ?: CoinRegistry.default.id }

    fun walletAddressFor(coinId: String): Flow<String> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.walletAddressFor(coinId)]
                ?: if (coinId == CoinRegistry.default.id) prefs[Keys.WALLET_ADDRESS_LEGACY] ?: "" else ""
        }

    val refreshIntervalMin: Flow<Int> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { (it[Keys.REFRESH_INTERVAL_MIN] ?: 15).coerceAtLeast(15) }

    val showSparkline: Flow<Boolean> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.SHOW_SPARKLINE] ?: true }

    val sparklineDays: Flow<Int> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.SPARKLINE_DAYS] ?: 1 }

    val chartStyle: Flow<ChartStyle> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            try { ChartStyle.valueOf(it[Keys.CHART_STYLE] ?: "AREA") }
            catch (_: Exception) { ChartStyle.AREA }
        }

    val appTheme: Flow<AppTheme> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map {
            try { AppTheme.valueOf(it[Keys.APP_THEME] ?: "CYBER") }
            catch (_: Exception) { AppTheme.CYBER }
        }

    val customAccentArgb: Flow<Int> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CUSTOM_ACCENT_ARGB] ?: 0xFF00D4FF.toInt() }

    val customSecondaryArgb: Flow<Int> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CUSTOM_SECONDARY_ARGB] ?: 0xFF7B2FFF.toInt() }

    // ── Legacy single-coin cache reads (backward compat) ───────────────────────

    val cachedPriceUsd: Flow<Double> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CACHED_PRICE_USD] ?: 0.0 }

    val cachedChangePct: Flow<Double> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CACHED_CHANGE_PCT] ?: 0.0 }

    val cachedBalance: Flow<Double> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CACHED_BALANCE] ?: 0.0 }

    val cachedSparkline: Flow<List<Double>> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.CACHED_SPARKLINE]
                ?.split(",")?.mapNotNull { it.toDoubleOrNull() }
                ?: emptyList()
        }

    val cachedSparklineTimestamps: Flow<List<Long>> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.CACHED_SPARKLINE_TS]
                ?.split(",")?.mapNotNull { it.toLongOrNull() }
                ?: emptyList()
        }

    val cachedUpdatedMs: Flow<Long> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CACHED_UPDATED_MS] ?: 0L }

    val lastWorkerRunMs: Flow<Long> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.LAST_WORKER_RUN_MS] ?: 0L }

    val lastErrorMsg: Flow<String?> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.LAST_ERROR_MSG] }

    // ── Multi-coin flows ────────────────────────────────────────────────────────

    /**
     * Ordered list of all followed coin IDs.
     * Defaults to all built-in coins on first install.
     */
    val followedCoinIds: Flow<List<String>> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.FOLLOWED_COIN_IDS]
                ?.split(",")?.filter { it.isNotBlank() }
                ?: CoinRegistry.all.map { it.id }
        }

    /**
     * Ordered list of coin IDs shown as tabs on the widget (max 5).
     * Defaults to just the active coin on first install.
     */
    val widgetCoinIds: Flow<List<String>> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.WIDGET_COIN_IDS]
                ?.split(",")?.filter { it.isNotBlank() }?.take(5)
                ?: listOf(prefs[Keys.COIN_ID] ?: CoinRegistry.default.id)
        }

    // ── Per-coin cache reads ────────────────────────────────────────────────────

    /**
     * Legacy single-coin cache keys are only meaningful if the stored active coin id matches
     * the coin we're reading. They were written by an older build that only knew about XRP,
     * so the fallback must consult the persisted active coinId rather than the moving default.
     */
    private fun isLegacyActive(prefs: Preferences, coinId: String): Boolean =
        coinId == (prefs[Keys.COIN_ID] ?: CoinRegistry.default.id)

    fun priceUsdFor(coinId: String): Flow<Double> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.priceUsdFor(coinId)]
                ?: if (isLegacyActive(prefs, coinId)) prefs[Keys.CACHED_PRICE_USD] ?: 0.0 else 0.0
        }

    fun changePctFor(coinId: String): Flow<Double> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.changePctFor(coinId)]
                ?: if (isLegacyActive(prefs, coinId)) prefs[Keys.CACHED_CHANGE_PCT] ?: 0.0 else 0.0
        }

    fun balanceFor(coinId: String): Flow<Double> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.balanceFor(coinId)]
                ?: if (isLegacyActive(prefs, coinId)) prefs[Keys.CACHED_BALANCE] ?: 0.0 else 0.0
        }

    fun sparklineFor(coinId: String): Flow<List<Double>> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val raw = prefs[Keys.sparklineFor(coinId)]
                ?: if (isLegacyActive(prefs, coinId)) prefs[Keys.CACHED_SPARKLINE] else null
            raw?.split(",")?.mapNotNull { it.toDoubleOrNull() } ?: emptyList()
        }

    fun sparklineTsFor(coinId: String): Flow<List<Long>> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val raw = prefs[Keys.sparklineTsFor(coinId)]
                ?: if (isLegacyActive(prefs, coinId)) prefs[Keys.CACHED_SPARKLINE_TS] else null
            raw?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        }

    fun updatedMsFor(coinId: String): Flow<Long> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.updatedMsFor(coinId)]
                ?: if (isLegacyActive(prefs, coinId)) prefs[Keys.CACHED_UPDATED_MS] ?: 0L else 0L
        }

    fun lastFetchedMsFor(coinId: String): Flow<Long> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.lastFetchedMsFor(coinId)] ?: 0L }

    // ── Setters ─────────────────────────────────────────────────────────────────

    suspend fun setCoinId(id: String) = ds.edit { it[Keys.COIN_ID] = id }

    suspend fun setWalletAddress(coinId: String, addr: String) =
        ds.edit { it[Keys.walletAddressFor(coinId)] = addr.trim() }

    suspend fun setRefreshInterval(minutes: Int) =
        ds.edit { it[Keys.REFRESH_INTERVAL_MIN] = minutes.coerceAtLeast(15) }

    suspend fun setShowSparkline(show: Boolean)  = ds.edit { it[Keys.SHOW_SPARKLINE] = show }
    suspend fun setSparklineDays(days: Int)       = ds.edit { it[Keys.SPARKLINE_DAYS] = days }
    suspend fun setChartStyle(style: ChartStyle)  = ds.edit { it[Keys.CHART_STYLE] = style.name }
    suspend fun setAppTheme(theme: AppTheme)       = ds.edit { it[Keys.APP_THEME] = theme.name }
    suspend fun setCustomAccentArgb(argb: Int) = ds.edit { it[Keys.CUSTOM_ACCENT_ARGB] = argb }
    suspend fun setCustomSecondaryArgb(argb: Int) = ds.edit { it[Keys.CUSTOM_SECONDARY_ARGB] = argb }

    suspend fun setFollowedCoinIds(ids: List<String>) =
        ds.edit { it[Keys.FOLLOWED_COIN_IDS] = ids.joinToString(",") }

    suspend fun setWidgetCoinIds(ids: List<String>) =
        ds.edit { it[Keys.WIDGET_COIN_IDS] = ids.take(5).joinToString(",") }

    /** Saves per-coin price data; also bumps the legacy keys when this is the active coin. */
    suspend fun cacheCoinData(
        coinId: String,
        priceUsd: Double,
        changePct: Double,
        balance: Double,
        sparkline: List<Double> = emptyList(),
        sparklineTimestamps: List<Long> = emptyList()
    ) = ds.edit { prefs ->
        val now = System.currentTimeMillis()
        prefs[Keys.priceUsdFor(coinId)]      = priceUsd
        prefs[Keys.changePctFor(coinId)]     = changePct
        prefs[Keys.balanceFor(coinId)]       = balance
        prefs[Keys.updatedMsFor(coinId)]     = now
        prefs[Keys.sparklineFor(coinId)]     = sparkline.joinToString(",")
        prefs[Keys.sparklineTsFor(coinId)]   = sparklineTimestamps.joinToString(",")
        prefs[Keys.lastFetchedMsFor(coinId)] = now

        // Keep legacy keys in sync for the active widget coin
        if (coinId == (prefs[Keys.COIN_ID] ?: CoinRegistry.default.id)) {
            prefs[Keys.CACHED_PRICE_USD]    = priceUsd
            prefs[Keys.CACHED_CHANGE_PCT]   = changePct
            prefs[Keys.CACHED_BALANCE]      = balance
            prefs[Keys.CACHED_UPDATED_MS]   = now
            prefs[Keys.CACHED_SPARKLINE]    = sparkline.joinToString(",")
            prefs[Keys.CACHED_SPARKLINE_TS] = sparklineTimestamps.joinToString(",")
        }
    }

    /** Legacy alias — writes to the currently active coin's cache slot. */
    suspend fun cacheWidgetData(
        priceUsd: Double,
        changePct: Double,
        balance: Double,
        sparkline: List<Double> = emptyList(),
        sparklineTimestamps: List<Long> = emptyList()
    ) {
        val activeCoinId = coinId.first()
        cacheCoinData(activeCoinId, priceUsd, changePct, balance, sparkline, sparklineTimestamps)
    }

    suspend fun recordWorkerResult(errorMsg: String?) = ds.edit {
        it[Keys.LAST_WORKER_RUN_MS] = System.currentTimeMillis()
        if (errorMsg != null) {
            it[Keys.LAST_ERROR_MSG] = errorMsg
        } else {
            it.remove(Keys.LAST_ERROR_MSG)
        }
    }
}
