package com.leeam.cryptowidget.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
        val COIN_ID               = stringPreferencesKey("coin_id")
        val WALLET_ADDRESS        = stringPreferencesKey("wallet_address")
        val REFRESH_INTERVAL_MIN  = intPreferencesKey("refresh_interval_min")
        val SHOW_SPARKLINE        = booleanPreferencesKey("show_sparkline")
        val SPARKLINE_DAYS        = intPreferencesKey("sparkline_days")
        val LAST_WORKER_RUN_MS    = longPreferencesKey("last_worker_run_ms")
        val LAST_ERROR_MSG        = stringPreferencesKey("last_error_msg")
        val CACHED_PRICE_USD      = doublePreferencesKey("cached_price_usd")
        val CACHED_CHANGE_PCT     = doublePreferencesKey("cached_change_pct")
        val CACHED_BALANCE_XRP    = doublePreferencesKey("cached_balance_xrp")
        val CACHED_UPDATED_MS     = longPreferencesKey("cached_updated_ms")
    }

    val coinId: Flow<String> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.COIN_ID] ?: "ripple" }

    val walletAddress: Flow<String> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.WALLET_ADDRESS] ?: "" }

    val refreshIntervalMin: Flow<Int> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { (it[Keys.REFRESH_INTERVAL_MIN] ?: 15).coerceAtLeast(15) }

    val showSparkline: Flow<Boolean> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.SHOW_SPARKLINE] ?: true }

    val sparklineDays: Flow<Int> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.SPARKLINE_DAYS] ?: 1 }

    val cachedPriceUsd: Flow<Double> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CACHED_PRICE_USD] ?: 0.0 }

    val cachedChangePct: Flow<Double> = ds.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CACHED_CHANGE_PCT] ?: 0.0 }

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
    suspend fun setWalletAddress(addr: String) = ds.edit { it[Keys.WALLET_ADDRESS] = addr }
    suspend fun setRefreshInterval(minutes: Int) =
        ds.edit { it[Keys.REFRESH_INTERVAL_MIN] = minutes.coerceAtLeast(15) }
    suspend fun setShowSparkline(show: Boolean) = ds.edit { it[Keys.SHOW_SPARKLINE] = show }
    suspend fun setSparklineDays(days: Int) = ds.edit { it[Keys.SPARKLINE_DAYS] = days }

    suspend fun cacheWidgetData(priceUsd: Double, changePct: Double, balanceXrp: Double) =
        ds.edit {
            it[Keys.CACHED_PRICE_USD]   = priceUsd
            it[Keys.CACHED_CHANGE_PCT]  = changePct
            it[Keys.CACHED_BALANCE_XRP] = balanceXrp
            it[Keys.CACHED_UPDATED_MS]  = System.currentTimeMillis()
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
