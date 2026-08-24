package com.electream.cryptowidget.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.electream.cryptowidget.data.model.WidgetData
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
        // ── Schema versioning ──────────────────────────────────────────────────
        /** Bumped by [WidgetPreferencesBootstrap] after migrations run. */
        val SCHEMA_VERSION        = intPreferencesKey("schema_version")

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

        // ── Debug log (newline-separated TSV: time\tlevel\tsource\tmessage) ─────
        val DEBUG_LOG = stringPreferencesKey("debug_log")

        // ── Per-coin dynamic keys ───────────────────────────────────────────────
        fun walletAddressFor(coinId: String)  = stringPreferencesKey("wallet_address_$coinId")
        fun priceUsdFor(coinId: String)       = doublePreferencesKey("price_usd_$coinId")
        fun changePctFor(coinId: String)      = doublePreferencesKey("change_pct_$coinId")
        fun balanceFor(coinId: String)        = doublePreferencesKey("balance_$coinId")
        fun updatedMsFor(coinId: String)      = longPreferencesKey("updated_ms_$coinId")
        fun sparklineFor(coinId: String)      = stringPreferencesKey("sparkline_$coinId")
        fun sparklineTsFor(coinId: String)    = stringPreferencesKey("sparkline_ts_$coinId")
        fun lastFetchedMsFor(coinId: String)  = longPreferencesKey("last_fetched_$coinId")
        fun lastFetchErrorFor(coinId: String) = stringPreferencesKey("last_fetch_error_$coinId")
    }

    // ── Single-coin compat ──────────────────────────────────────────────────────

    /**
     * Active widget coin — empty string when the user hasn't picked one yet.
     * Callers MUST treat empty as a real "no coin chosen" state, not as a fallback to
     * any default coin. See [WidgetPreferencesBootstrap] for the migration that seeds this.
     */
    val coinId: Flow<String> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.COIN_ID] ?: "" }

    fun walletAddressFor(coinId: String): Flow<String> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            // Legacy single-wallet key only applies to the coin that was the active coin
            // at the time of upgrade — never to "the default coin" generally.
            prefs[Keys.walletAddressFor(coinId)]
                ?: if (coinId.isNotEmpty() && prefs[Keys.COIN_ID] == coinId)
                    prefs[Keys.WALLET_ADDRESS_LEGACY] ?: ""
                else ""
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
     * Ordered list of all followed coin IDs. Empty when the user hasn't picked any yet.
     * The worker is a no-op while this is empty; the widget renders the "tap to choose
     * coins" CTA. See [WidgetPreferencesBootstrap].
     */
    val followedCoinIds: Flow<List<String>> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.FOLLOWED_COIN_IDS]
                ?.split(",")?.filter { it.isNotBlank() }
                ?: emptyList()
        }

    /**
     * Ordered list of coin IDs shown as tabs on the widget (max 5). Empty when the user
     * hasn't picked any yet.
     */
    val widgetCoinIds: Flow<List<String>> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.WIDGET_COIN_IDS]
                ?.split(",")?.filter { it.isNotBlank() }?.take(5)
                ?: emptyList()
        }

    // ── Per-coin cache reads ────────────────────────────────────────────────────

    /**
     * Legacy single-coin cache keys are only meaningful if the stored active coin id matches
     * the coin we're reading. They were written by an older build that only knew about XRP,
     * so the fallback must consult the persisted active coinId — never an implicit default.
     * Returns false when no active coin is set (fresh install).
     */
    private fun isLegacyActive(prefs: Preferences, coinId: String): Boolean =
        coinId.isNotEmpty() && prefs[Keys.COIN_ID] == coinId

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

    fun lastFetchErrorFor(coinId: String): Flow<String?> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.lastFetchErrorFor(coinId)] }

    suspend fun setLastFetchError(coinId: String, error: String?) = ds.edit { prefs ->
        if (error == null) prefs.remove(Keys.lastFetchErrorFor(coinId))
        else prefs[Keys.lastFetchErrorFor(coinId)] = error
    }

    // ── Debug log ──────────────────────────────────────────────────────────────

    /** Loads the persisted debug-log entries (newest-last). */
    val debugLog: Flow<List<com.electream.cryptowidget.data.local.DebugEntry>> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> deserializeDebugLog(prefs[Keys.DEBUG_LOG]) }

    suspend fun persistDebugLog(entries: List<com.electream.cryptowidget.data.local.DebugEntry>) =
        ds.edit { it[Keys.DEBUG_LOG] = serializeDebugLog(entries) }

    private fun serializeDebugLog(entries: List<com.electream.cryptowidget.data.local.DebugEntry>): String =
        entries.joinToString("\n") { entry ->
            // tab-separated; replace embedded tabs/newlines in message so the split is unambiguous
            val safeMessage = entry.message.replace('\t', ' ').replace('\n', ' ')
            val safeSource  = entry.source.replace('\t', ' ').replace('\n', ' ')
            "${entry.timeMs}\t${entry.level.name}\t${safeSource}\t${safeMessage}"
        }

    private fun deserializeDebugLog(raw: String?): List<com.electream.cryptowidget.data.local.DebugEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split('\n').mapNotNull { line ->
            val parts = line.split('\t', limit = 4)
            if (parts.size != 4) return@mapNotNull null
            runCatching {
                com.electream.cryptowidget.data.local.DebugEntry(
                    timeMs  = parts[0].toLong(),
                    level   = com.electream.cryptowidget.data.local.DebugLevel.valueOf(parts[1]),
                    source  = parts[2],
                    message = parts[3]
                )
            }.getOrNull()
        }
    }

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

        // Keep legacy keys in sync only when the active coin is set AND matches this write.
        // Never bumps legacy keys for a non-active coin or when no active coin is chosen.
        if (coinId.isNotEmpty() && prefs[Keys.COIN_ID] == coinId) {
            prefs[Keys.CACHED_PRICE_USD]    = priceUsd
            prefs[Keys.CACHED_CHANGE_PCT]   = changePct
            prefs[Keys.CACHED_BALANCE]      = balance
            prefs[Keys.CACHED_UPDATED_MS]   = now
            prefs[Keys.CACHED_SPARKLINE]    = sparkline.joinToString(",")
            prefs[Keys.CACHED_SPARKLINE_TS] = sparklineTimestamps.joinToString(",")
        }
    }

    suspend fun recordWorkerResult(errorMsg: String?) = ds.edit {
        it[Keys.LAST_WORKER_RUN_MS] = System.currentTimeMillis()
        if (errorMsg != null) {
            it[Keys.LAST_ERROR_MSG] = errorMsg
        } else {
            it.remove(Keys.LAST_ERROR_MSG)
        }
    }

    // ── Bootstrap / migration hooks ────────────────────────────────────────────

    /**
     * One-shot atomic edit primitive used by [WidgetPreferencesBootstrap] to read and
     * mutate the underlying [Preferences] in a single transaction. Package-internal so
     * the bootstrap can sit in the same package without leaking the DataStore handle.
     */
    internal suspend fun editPrefs(block: (MutablePreferences) -> Unit) = ds.edit(block)

    /**
     * One-shot read of the entire preferences snapshot. Used by the widget receiver's
     * goAsync coroutine to minimize the number of independent ds.data subscriptions
     * inside the broadcast's time budget.
     */
    internal suspend fun snapshotPrefs(): Preferences =
        ds.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.first()

    /**
     * Snapshot the per-coin cache into a [WidgetData] suitable for the renderer.
     * Returns a `WidgetData` with the coin's symbol resolved via [com.electream.cryptowidget.data.model.CoinRegistry];
     * never throws (missing keys read as zero/empty). For a fresh, never-fetched coin this
     * returns a zero-filled snapshot — callers should branch on `priceUsd > 0.0` if they
     * need to distinguish "no data yet" from "real zero".
     */
    suspend fun snapshotFor(coinId: String): WidgetData {
        val symbol = com.electream.cryptowidget.data.model.CoinRegistry.byId(coinId).symbol
        val price       = priceUsdFor(coinId).first()
        val change      = changePctFor(coinId).first()
        val balance     = balanceFor(coinId).first()
        val updated     = updatedMsFor(coinId).first()
        val sparkline   = sparklineFor(coinId).first()
        val sparklineTs = sparklineTsFor(coinId).first()
        return WidgetData(
            coinId              = coinId,
            symbol              = symbol,
            priceUsd            = price,
            change24hPct        = change,
            walletBalance       = balance,
            walletValueUsd      = balance * price,
            lastUpdatedMs       = updated,
            sparklinePrices     = sparkline,
            sparklineTimestamps = sparklineTs
        )
    }
}
