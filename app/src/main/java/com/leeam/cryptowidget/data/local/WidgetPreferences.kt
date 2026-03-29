package com.leeam.cryptowidget.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.leeam.cryptowidget.data.local.AppTheme
import com.leeam.cryptowidget.data.local.ChartStyle
import com.leeam.cryptowidget.data.model.CoinRegistry
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
        val COIN_ID               = stringPreferencesKey("coin_id")
        // Legacy single-address key — migrated to per-coin on first read
        val WALLET_ADDRESS_LEGACY = stringPreferencesKey("wallet_address")
        val REFRESH_INTERVAL_MIN  = intPreferencesKey("refresh_interval_min")
        val SHOW_SPARKLINE        = booleanPreferencesKey("show_sparkline")
        val SPARKLINE_DAYS        = intPreferencesKey("sparkline_days")
        val CHART_STYLE           = stringPreferencesKey("chart_style")
        val APP_THEME             = stringPreferencesKey("app_theme")
        val LAST_WORKER_RUN_MS    = longPreferencesKey("last_worker_run_ms")
        val LAST_ERROR_MSG        = stringPreferencesKey("last_error_msg")
        val CACHED_PRICE_USD      = doublePreferencesKey("cached_price_usd")
        val CACHED_CHANGE_PCT     = doublePreferencesKey("cached_change_pct")
        val CACHED_BALANCE        = doublePreferencesKey("cached_balance")
        val CACHED_UPDATED_MS     = longPreferencesKey("cached_updated_ms")
        val CACHED_SPARKLINE      = stringPreferencesKey("cached_sparkline")
        val CACHED_SPARKLINE_TS   = stringPreferencesKey("cached_sparkline_ts")

        /** Per-coin wallet address key. One key per coin ID. */
        fun walletAddressFor(coinId: String) = stringPreferencesKey("wallet_address_$coinId")
    }

    val coinId: Flow<String> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.COIN_ID] ?: CoinRegistry.default.id }

    /** Returns the stored wallet address for a specific coin. */
    fun walletAddressFor(coinId: String): Flow<String> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val perCoinKey = Keys.walletAddressFor(coinId)
            prefs[perCoinKey]
                // Migrate legacy WALLET_ADDRESS (XRP) on first access for ripple
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
                ?.split(",")
                ?.mapNotNull { it.toDoubleOrNull() }
                ?: emptyList()
        }

    val cachedSparklineTimestamps: Flow<List<Long>> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[Keys.CACHED_SPARKLINE_TS]
                ?.split(",")
                ?.mapNotNull { it.toLongOrNull() }
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

    suspend fun setCoinId(id: String) = ds.edit { it[Keys.COIN_ID] = id }

    suspend fun setWalletAddress(coinId: String, addr: String) =
        ds.edit { it[Keys.walletAddressFor(coinId)] = addr.trim() }

    suspend fun setRefreshInterval(minutes: Int) =
        ds.edit { it[Keys.REFRESH_INTERVAL_MIN] = minutes.coerceAtLeast(15) }
    suspend fun setShowSparkline(show: Boolean) = ds.edit { it[Keys.SHOW_SPARKLINE] = show }
    suspend fun setSparklineDays(days: Int) = ds.edit { it[Keys.SPARKLINE_DAYS] = days }
    suspend fun setChartStyle(style: ChartStyle) = ds.edit { it[Keys.CHART_STYLE] = style.name }
    suspend fun setAppTheme(theme: AppTheme) = ds.edit { it[Keys.APP_THEME] = theme.name }

    suspend fun cacheWidgetData(
        priceUsd: Double,
        changePct: Double,
        balance: Double,
        sparkline: List<Double> = emptyList(),
        sparklineTimestamps: List<Long> = emptyList()
    ) = ds.edit {
        it[Keys.CACHED_PRICE_USD]    = priceUsd
        it[Keys.CACHED_CHANGE_PCT]   = changePct
        it[Keys.CACHED_BALANCE]      = balance
        it[Keys.CACHED_UPDATED_MS]   = System.currentTimeMillis()
        it[Keys.CACHED_SPARKLINE]    = sparkline.joinToString(",")
        it[Keys.CACHED_SPARKLINE_TS] = sparklineTimestamps.joinToString(",")
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
