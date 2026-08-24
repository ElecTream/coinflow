package com.electream.cryptowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.datastore.preferences.core.Preferences
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.electream.cryptowidget.R
import com.electream.cryptowidget.data.local.AlertRepository
import com.electream.cryptowidget.data.local.AppTheme
import com.electream.cryptowidget.data.local.ChartStyle
import com.electream.cryptowidget.data.local.DebugLog
import com.electream.cryptowidget.data.local.WidgetPreferences
import com.electream.cryptowidget.data.model.CoinDefinition
import com.electream.cryptowidget.data.model.CoinRegistry
import com.electream.cryptowidget.data.model.WidgetData
import com.electream.cryptowidget.data.repository.CoinRepository
import com.electream.cryptowidget.data.repository.CryptoRepository
import com.electream.cryptowidget.notifications.AlertNotifier
import com.electream.cryptowidget.ui.theme.toThemeColors
import com.electream.cryptowidget.worker.PriceUpdateWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoinflowWidgetEntryPoint {
    fun widgetPreferences(): WidgetPreferences
    fun cryptoRepository(): CryptoRepository
    fun coinRepository(): CoinRepository
    fun alertRepository(): AlertRepository
    fun alertNotifier(): AlertNotifier
    fun debugLog(): DebugLog
}

/**
 * The widget receiver. Critical lifecycle note:
 *
 * `AppWidgetProvider` extends `BroadcastReceiver`. The OS only guarantees the process
 * is alive while [onReceive] is on the stack — once it returns, Android is free to kill
 * the process, taking with it any in-flight `CoroutineScope(Dispatchers.IO).launch`
 * blocks that haven't completed.
 *
 * For a freshly installed app with no foreground component, this happens almost every
 * time. The symptom is the framework's `initialLayout` showing "Loading…" forever and
 * never being replaced with the real RemoteViews.
 *
 * The fix is [goAsync]: it returns a [PendingResult] that keeps the process pinned
 * until [PendingResult.finish] is called (up to ~10s). All async work runs inside a
 * single coroutine that calls finish() in its `finally` block.
 */
class CoinflowWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_SELECT_COIN = "com.electream.cryptowidget.ACTION_SELECT_COIN"
        const val ACTION_REFRESH     = "com.electream.cryptowidget.ACTION_REFRESH"
        const val EXTRA_COIN_ID      = "extra_coin_id"

        /** ~10s budget on most Android versions; we cap below to leave headroom. */
        private const val GO_ASYNC_BUDGET_MS = 8_000L

        @Volatile var spinJob: Job? = null

        fun cancelSpinner() {
            spinJob?.cancel()
            spinJob = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val log    = runCatching { entryPoint(context).debugLog() }.getOrNull()
        log?.info("Widget.onReceive", "action=$action")

        // Let the framework dispatch onEnabled / onDeleted / onDisabled etc. for any
        // action we don't handle ourselves. Note: we do NOT call super for the actions
        // below — we drive them off the explicit cases so our goAsync coroutine owns
        // the entire pipeline and Android can't deliver a duplicate onUpdate alongside.
        when (action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            AppWidgetManager.ACTION_APPWIDGET_ENABLED,
            AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED,
            ACTION_REFRESH,
            ACTION_SELECT_COIN -> Unit  // handled below
            else -> {
                super.onReceive(context, intent)
                return
            }
        }

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeoutOrNull(GO_ASYNC_BUDGET_MS) {
                    when (action) {
                        AppWidgetManager.ACTION_APPWIDGET_UPDATE,
                        AppWidgetManager.ACTION_APPWIDGET_ENABLED -> {
                            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                                ?: allWidgetIds(appContext)
                            renderFromCacheOrEmpty(appContext, ids, source = "onUpdate")
                        }
                        AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED -> {
                            val id = intent.getIntExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                AppWidgetManager.INVALID_APPWIDGET_ID
                            )
                            val ids = if (id != AppWidgetManager.INVALID_APPWIDGET_ID)
                                intArrayOf(id) else allWidgetIds(appContext)
                            renderFromCacheOrEmpty(appContext, ids, source = "onOptionsChanged")
                        }
                        ACTION_REFRESH -> {
                            showRefreshingText(appContext)
                            fetchDirectly(appContext)
                        }
                        ACTION_SELECT_COIN -> {
                            val coinId = intent.getStringExtra(EXTRA_COIN_ID).orEmpty()
                            if (coinId.isNotEmpty()) {
                                switchActiveCoin(appContext, coinId)
                            }
                        }
                    }
                } ?: log?.warn("Widget.onReceive", "timed out action=$action after ${GO_ASYNC_BUDGET_MS}ms")
            } catch (e: Exception) {
                log?.error("Widget.onReceive", "uncaught action=$action", e)
                // Best-effort fallback so the user never sees the misleading initial layout
                // forever. Push the empty CTA — at minimum the body opens Settings.
                runCatching {
                    val ids = allWidgetIds(appContext)
                    if (ids.isNotEmpty()) {
                        val fallback = WidgetUpdater.buildEmptyRemoteViews(appContext)
                        val mgr = AppWidgetManager.getInstance(appContext)
                        ids.forEach { mgr.updateAppWidget(it, fallback) }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun allWidgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, CoinflowWidgetProvider::class.java)
        )

    // ──────────────────────────────────────────────────────────────────────────
    // Render: cache-first, empty-CTA fallback. Source of truth for what the user
    // sees on every onUpdate / onOptionsChanged.
    // ──────────────────────────────────────────────────────────────────────────

    private suspend fun renderFromCacheOrEmpty(
        context: Context,
        ids: IntArray,
        source: String
    ) {
        val ep    = entryPoint(context)
        val log   = ep.debugLog()
        val prefs = ep.widgetPreferences()
        val coins = ep.coinRepository()
        val manager = AppWidgetManager.getInstance(context)

        log.info("Widget.render", "$source ids=${ids.toList()}")

        if (ids.isEmpty()) {
            log.warn("Widget.render", "$source: no widget ids — nothing to render")
            return
        }

        val snapshot = ep.widgetPreferences().snapshot()
        val storedCoinId    = snapshot.coinId
        val widgetCoinIds   = snapshot.widgetCoinIds
        val followedCoinIds = snapshot.followedCoinIds

        log.info("Widget.render",
            "$source stored=$storedCoinId widget=$widgetCoinIds followed=$followedCoinIds")

        // (a) No coins picked yet — show the picker CTA.
        if (storedCoinId.isEmpty() && widgetCoinIds.isEmpty() && followedCoinIds.isEmpty()) {
            val empty = WidgetUpdater.buildEmptyRemoteViews(context)
            ids.forEach { manager.updateAppWidget(it, empty) }
            log.info("Widget.render", "$source: empty CTA applied to ${ids.size} widgets")
            return
        }

        // The user has picked at least one coin — kick off a background refresh
        // (worker no-ops if followed is empty for any reason).
        enqueueRefresh(context)

        // Resolve the coin to render.
        val candidates = buildList {
            if (storedCoinId.isNotEmpty()) add(storedCoinId)
            widgetCoinIds.forEach { if (it !in this) add(it) }
            followedCoinIds.forEach { if (it !in this) add(it) }
        }
        val coinId = candidates.firstOrNull { snapshot.priceFor(it) > 0.0 }
            ?: candidates.firstOrNull()
            ?: ""

        if (coinId.isEmpty()) {
            val empty = WidgetUpdater.buildEmptyRemoteViews(context)
            ids.forEach { manager.updateAppWidget(it, empty) }
            log.warn("Widget.render", "$source: candidates resolved empty — applied CTA")
            return
        }

        val theme = snapshot.theme()
        val tabIds = widgetCoinIds.ifEmpty { listOf(coinId) }
        val coinLookup = buildCoinLookup(coins, tabIds + coinId)
        val price = snapshot.priceFor(coinId)

        // (b) Coins chosen but cache empty — render the skeleton with controls.
        if (price <= 0.0) {
            val skeleton = WidgetUpdater.buildLoadingSkeletonRemoteViews(
                context, tabIds, coinId, coinLookup, theme
            )
            ids.forEach { manager.updateAppWidget(it, skeleton) }
            log.info("Widget.render", "$source: skeleton applied (no cache yet) coin=$coinId")
            return
        }

        // (c) Cache has data — full render.
        if (coinId != storedCoinId) prefs.setCoinId(coinId)

        val cached = WidgetData(
            coinId              = coinId,
            symbol              = (coinLookup[coinId] ?: CoinRegistry.byId(coinId)).symbol,
            priceUsd            = price,
            change24hPct        = snapshot.changeFor(coinId),
            walletBalance       = snapshot.balanceFor(coinId),
            walletValueUsd      = snapshot.balanceFor(coinId) * price,
            lastUpdatedMs       = snapshot.updatedMsFor(coinId),
            sparklinePrices     = snapshot.sparklineFor(coinId),
            sparklineTimestamps = snapshot.sparklineTsFor(coinId)
        )
        WidgetUpdater.updateAllWidgets(
            context, cached, snapshot.chartStyle, theme,
            widgetCoinIds = tabIds,
            activeCoinId  = coinId,
            coinLookup    = coinLookup
        )
        log.info("Widget.render", "$source: full render coin=$coinId price=$price")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tab tap handler.
    // ──────────────────────────────────────────────────────────────────────────

    private suspend fun switchActiveCoin(context: Context, coinId: String) {
        val ep    = entryPoint(context)
        val log   = ep.debugLog()
        val prefs = ep.widgetPreferences()
        val coins = ep.coinRepository()

        log.info("Widget.switchActiveCoin", "to=$coinId")
        prefs.setCoinId(coinId)

        val snapshot      = prefs.snapshot()
        val theme         = snapshot.theme()
        val widgetCoinIds = snapshot.widgetCoinIds.ifEmpty { listOf(coinId) }
        val coinLookup    = buildCoinLookup(coins, widgetCoinIds + coinId)
        val price         = snapshot.priceFor(coinId)
        val manager       = AppWidgetManager.getInstance(context)
        val ids           = allWidgetIds(context)

        if (price <= 0.0) {
            val skeleton = WidgetUpdater.buildLoadingSkeletonRemoteViews(
                context, widgetCoinIds, coinId, coinLookup, theme
            )
            ids.forEach { manager.updateAppWidget(it, skeleton) }
            enqueueRefresh(context)
            log.info("Widget.switchActiveCoin", "$coinId not cached — skeleton + enqueue")
            return
        }

        val data = WidgetData(
            coinId              = coinId,
            symbol              = (coinLookup[coinId] ?: CoinRegistry.byId(coinId)).symbol,
            priceUsd            = price,
            change24hPct        = snapshot.changeFor(coinId),
            walletBalance       = snapshot.balanceFor(coinId),
            walletValueUsd      = snapshot.balanceFor(coinId) * price,
            lastUpdatedMs       = snapshot.updatedMsFor(coinId),
            sparklinePrices     = snapshot.sparklineFor(coinId),
            sparklineTimestamps = snapshot.sparklineTsFor(coinId)
        )
        WidgetUpdater.updateAllWidgets(
            context, data, snapshot.chartStyle, theme,
            widgetCoinIds = widgetCoinIds,
            activeCoinId  = coinId,
            coinLookup    = coinLookup
        )
        log.info("Widget.switchActiveCoin", "$coinId rendered from cache")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Refresh button handler.
    // ──────────────────────────────────────────────────────────────────────────

    private suspend fun fetchDirectly(context: Context) {
        val ep       = entryPoint(context)
        val log      = ep.debugLog()
        val prefs    = ep.widgetPreferences()
        val repo     = ep.cryptoRepository()
        val coins    = ep.coinRepository()
        val alerts   = ep.alertRepository()
        val notifier = ep.alertNotifier()

        val coinId = prefs.coinId.first()
        if (coinId.isEmpty()) {
            log.info("Widget.fetchDirectly", "no active coin — nothing to fetch")
            return
        }

        val snapshot      = prefs.snapshot()
        val wallet        = prefs.walletAddressFor(coinId).first()
        val style         = snapshot.chartStyle
        val theme         = snapshot.theme()
        val widgetCoinIds = snapshot.widgetCoinIds.ifEmpty { listOf(coinId) }
        val coinLookup    = buildCoinLookup(coins, widgetCoinIds + coinId)

        log.info("Widget.fetchDirectly", "coin=$coinId")
        val result = repo.fetchWidgetData(coinId, wallet)
        result.fold(
            onSuccess = { data ->
                prefs.setLastFetchError(coinId, null)
                WidgetUpdater.updateAllWidgets(
                    context, data, style, theme,
                    widgetCoinIds = widgetCoinIds,
                    activeCoinId  = coinId,
                    coinLookup    = coinLookup
                )
                prefs.cacheCoinData(
                    coinId              = coinId,
                    priceUsd            = data.priceUsd,
                    changePct           = data.change24hPct,
                    balance             = data.walletBalance,
                    sparkline           = data.sparklinePrices,
                    sparklineTimestamps = data.sparklineTimestamps
                )
                alerts.checkAndFireAlerts(coinId, data.priceUsd) { alert ->
                    notifier.fireAlertNotification(alert, data.priceUsd)
                }
                prefs.recordWorkerResult(null)
                log.info("Widget.fetchDirectly", "ok coin=$coinId price=${data.priceUsd}")
            },
            onFailure = { e ->
                log.error("Widget.fetchDirectly", "failed coin=$coinId", e)
                prefs.setLastFetchError(coinId, e.message ?: "Fetch failed")
                val errorData = WidgetData(coinId = coinId, errorMessage = e.message ?: "Fetch failed")
                WidgetUpdater.updateAllWidgets(
                    context, errorData, style, theme,
                    widgetCoinIds = widgetCoinIds,
                    activeCoinId  = coinId,
                    coinLookup    = coinLookup
                )
                prefs.recordWorkerResult(e.message)
            }
        )
    }

    /**
     * Push a "Refreshing…" indicator immediately so the user gets feedback before
     * the fetch coroutine even starts. Uses a full RemoteViews (not
     * partiallyUpdateAppWidget — that's a no-op until a full update has landed,
     * which on a fresh install hasn't happened yet).
     */
    private suspend fun showRefreshingText(context: Context) {
        val ep      = entryPoint(context)
        val prefs   = ep.widgetPreferences()
        val coins   = ep.coinRepository()
        val ids     = allWidgetIds(context)
        if (ids.isEmpty()) return

        val coinId = prefs.coinId.first()
        if (coinId.isEmpty()) return

        val snapshot      = prefs.snapshot()
        val theme         = snapshot.theme()
        val widgetCoinIds = snapshot.widgetCoinIds.ifEmpty { listOf(coinId) }
        val coinLookup    = buildCoinLookup(coins, widgetCoinIds + coinId)
        val skeleton      = WidgetUpdater.buildLoadingSkeletonRemoteViews(
            context, widgetCoinIds, coinId, coinLookup, theme
        )
        val manager = AppWidgetManager.getInstance(context)
        ids.forEach { manager.updateAppWidget(it, skeleton) }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers.
    // ──────────────────────────────────────────────────────────────────────────

    private suspend fun buildCoinLookup(
        coins: CoinRepository,
        ids: List<String>
    ): Map<String, CoinDefinition> = ids.filter { it.isNotEmpty() }.distinct().associateWith {
        coins.coinById(it) ?: CoinRegistry.byId(it)
    }

    private fun enqueueRefresh(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "refresh_immediate",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PriceUpdateWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()
            )
    }

    private fun entryPoint(context: Context): CoinflowWidgetEntryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, CoinflowWidgetEntryPoint::class.java)
}

// ──────────────────────────────────────────────────────────────────────────────
// DataStore snapshot: read the whole preferences object once instead of
// subscribing to ds.data per-Flow per-key. Inside the broadcast's goAsync window
// we have ~8s; this keeps us well under that even on cold start where the first
// ds.data emission has to read from disk.
// ──────────────────────────────────────────────────────────────────────────────

internal class WidgetPrefsSnapshot(
    private val prefs: Preferences
) {
    val coinId: String
        get() = prefs[WidgetPreferences.Keys.COIN_ID] ?: ""
    val widgetCoinIds: List<String>
        get() = prefs[WidgetPreferences.Keys.WIDGET_COIN_IDS]
            ?.split(",")?.filter { it.isNotBlank() }?.take(5) ?: emptyList()
    val followedCoinIds: List<String>
        get() = prefs[WidgetPreferences.Keys.FOLLOWED_COIN_IDS]
            ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val chartStyle: ChartStyle
        get() = runCatching { ChartStyle.valueOf(prefs[WidgetPreferences.Keys.CHART_STYLE] ?: "AREA") }
            .getOrDefault(ChartStyle.AREA)
    private val appTheme: AppTheme
        get() = runCatching { AppTheme.valueOf(prefs[WidgetPreferences.Keys.APP_THEME] ?: "CYBER") }
            .getOrDefault(AppTheme.CYBER)
    private val customAccentArgb: Int
        get() = prefs[WidgetPreferences.Keys.CUSTOM_ACCENT_ARGB] ?: 0xFF00D4FF.toInt()
    private val customSecondaryArgb: Int
        get() = prefs[WidgetPreferences.Keys.CUSTOM_SECONDARY_ARGB] ?: 0xFF7B2FFF.toInt()

    fun theme() = appTheme.toThemeColors(customAccentArgb, customSecondaryArgb)

    fun priceFor(coinId: String): Double =
        prefs[WidgetPreferences.Keys.priceUsdFor(coinId)]
            ?: legacyDouble(coinId, WidgetPreferences.Keys.CACHED_PRICE_USD)

    fun changeFor(coinId: String): Double =
        prefs[WidgetPreferences.Keys.changePctFor(coinId)]
            ?: legacyDouble(coinId, WidgetPreferences.Keys.CACHED_CHANGE_PCT)

    fun balanceFor(coinId: String): Double =
        prefs[WidgetPreferences.Keys.balanceFor(coinId)]
            ?: legacyDouble(coinId, WidgetPreferences.Keys.CACHED_BALANCE)

    fun updatedMsFor(coinId: String): Long =
        prefs[WidgetPreferences.Keys.updatedMsFor(coinId)]
            ?: legacyLong(coinId, WidgetPreferences.Keys.CACHED_UPDATED_MS)

    fun sparklineFor(coinId: String): List<Double> {
        val raw = prefs[WidgetPreferences.Keys.sparklineFor(coinId)]
            ?: legacyString(coinId, WidgetPreferences.Keys.CACHED_SPARKLINE)
        return raw?.split(",")?.mapNotNull { it.toDoubleOrNull() } ?: emptyList()
    }

    fun sparklineTsFor(coinId: String): List<Long> {
        val raw = prefs[WidgetPreferences.Keys.sparklineTsFor(coinId)]
            ?: legacyString(coinId, WidgetPreferences.Keys.CACHED_SPARKLINE_TS)
        return raw?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }

    private fun isLegacyActive(coinId: String): Boolean =
        coinId.isNotEmpty() && prefs[WidgetPreferences.Keys.COIN_ID] == coinId

    private fun legacyDouble(coinId: String, key: Preferences.Key<Double>): Double =
        if (isLegacyActive(coinId)) prefs[key] ?: 0.0 else 0.0
    private fun legacyLong(coinId: String, key: Preferences.Key<Long>): Long =
        if (isLegacyActive(coinId)) prefs[key] ?: 0L else 0L
    private fun legacyString(coinId: String, key: Preferences.Key<String>): String? =
        if (isLegacyActive(coinId)) prefs[key] else null
}

internal suspend fun WidgetPreferences.snapshot(): WidgetPrefsSnapshot {
    // Access the underlying datastore through editPrefs by reading; alternatively expose
    // dataFlow. Cheaper: bounce through editPrefs in read-only mode. But editPrefs writes,
    // so we use ds.data.first() via a dedicated accessor.
    return WidgetPrefsSnapshot(snapshotPrefs())
}
