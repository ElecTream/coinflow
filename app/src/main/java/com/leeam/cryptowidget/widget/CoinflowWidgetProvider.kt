package com.leeam.cryptowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.leeam.cryptowidget.R
import com.leeam.cryptowidget.data.local.AlertRepository
import com.leeam.cryptowidget.data.local.WidgetPreferences
import com.leeam.cryptowidget.data.model.CoinDefinition
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.data.model.WidgetData
import com.leeam.cryptowidget.data.repository.CoinRepository
import com.leeam.cryptowidget.data.repository.CryptoRepository
import com.leeam.cryptowidget.notifications.AlertNotifier
import com.leeam.cryptowidget.ui.theme.toThemeColors
import com.leeam.cryptowidget.worker.PriceUpdateWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CoinflowWidgetEntryPoint {
    fun widgetPreferences(): WidgetPreferences
    fun cryptoRepository(): CryptoRepository
    fun coinRepository(): CoinRepository
    fun alertRepository(): AlertRepository
    fun alertNotifier(): AlertNotifier
}

/** Resolve a coin id to its [CoinDefinition], checking custom coins before built-ins. */
private suspend fun CoinRepository.resolveCoin(id: String): CoinDefinition =
    coinById(id) ?: CoinRegistry.byId(id)

class CoinflowWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_SELECT_COIN = "com.leeam.cryptowidget.ACTION_SELECT_COIN"
        const val EXTRA_COIN_ID      = "extra_coin_id"

        @Volatile var spinJob: Job? = null

        fun cancelSpinner() {
            spinJob?.cancel()
            spinJob = null
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        showCachedOrLoading(context, appWidgetManager, appWidgetIds)
        enqueueRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            "com.leeam.cryptowidget.ACTION_REFRESH" -> {
                showRefreshSpinner(context)
                fetchDirectly(context)
            }
            ACTION_SELECT_COIN -> {
                val coinId = intent.getStringExtra(EXTRA_COIN_ID) ?: return
                switchActiveCoin(context, coinId)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        showCachedOrLoading(context, appWidgetManager, intArrayOf(appWidgetId))
        enqueueRefresh(context)
    }

    /**
     * Switch active coin: persist the selection, then immediately re-render from cache.
     * No network call needed — the price data is already in DataStore.
     */
    private fun switchActiveCoin(context: Context, coinId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val ep    = entryPoint(context)
            val prefs = ep.widgetPreferences()
            val coins = ep.coinRepository()

            prefs.setCoinId(coinId)

            val price         = prefs.priceUsdFor(coinId).first()
            val change        = prefs.changePctFor(coinId).first()
            val balance       = prefs.balanceFor(coinId).first()
            val updatedMs     = prefs.updatedMsFor(coinId).first()
            val sparkline     = prefs.sparklineFor(coinId).first()
            val sparkTs       = prefs.sparklineTsFor(coinId).first()
            val style         = prefs.chartStyle.first()
            val theme         = prefs.appTheme.first().toThemeColors(
                customAccentArgb    = prefs.customAccentArgb.first(),
                customSecondaryArgb = prefs.customSecondaryArgb.first()
            )
            val widgetCoinIds = prefs.widgetCoinIds.first()
            val coinLookup    = buildCoinLookup(coins, widgetCoinIds + coinId)
            val coin          = coinLookup[coinId] ?: CoinRegistry.byId(coinId)

            val data = WidgetData(
                coinId              = coinId,
                symbol              = coin.symbol,
                priceUsd            = price,
                change24hPct        = change,
                walletBalance       = balance,
                walletValueUsd      = balance * price,
                lastUpdatedMs       = updatedMs,
                sparklinePrices     = sparkline,
                sparklineTimestamps = sparkTs
            )

            val manager = AppWidgetManager.getInstance(context)
            val ids     = manager.getAppWidgetIds(ComponentName(context, CoinflowWidgetProvider::class.java))
            val density = context.resources.displayMetrics.density

            for (widgetId in ids) {
                val options = manager.getAppWidgetOptions(widgetId)
                val w = (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180) * density)
                    .toInt().coerceAtLeast(200)
                val h = (options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110) * density * 0.30f)
                    .toInt().coerceAtLeast(40)
                val views = WidgetUpdater.buildRemoteViews(
                    context, data, w, h, style, theme,
                    widgetCoinIds = widgetCoinIds,
                    activeCoinId  = coinId,
                    coinLookup    = coinLookup
                )
                manager.updateAppWidget(widgetId, views)
            }
        }
    }

    private suspend fun buildCoinLookup(
        coins: CoinRepository,
        ids: List<String>
    ): Map<String, CoinDefinition> = ids.distinct().associateWith { coins.resolveCoin(it) }

    private fun showCachedOrLoading(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val ep    = entryPoint(context)
            val prefs = ep.widgetPreferences()
            val coins = ep.coinRepository()

            val storedCoinId  = prefs.coinId.first()
            val widgetCoinIds = prefs.widgetCoinIds.first()

            // Resolve which coin to actually render. If the stored active coin has no
            // cached price (fresh install, schema migration, or coin was removed), fall
            // back to the first widget tab that does have data.
            val candidates = buildList {
                add(storedCoinId)
                widgetCoinIds.forEach { if (it !in this) add(it) }
            }
            val coinId = candidates.firstOrNull { prefs.priceUsdFor(it).first() > 0.0 }
                ?: storedCoinId
            val price  = prefs.priceUsdFor(coinId).first()

            if (price <= 0.0) {
                ids.forEach {
                    manager.updateAppWidget(it, RemoteViews(context.packageName, R.layout.widget_loading))
                }
                return@launch
            }

            // Self-heal: if the rendered coin doesn't match the stored active coin,
            // persist the switch so subsequent refreshes target it.
            if (coinId != storedCoinId) prefs.setCoinId(coinId)

            val change        = prefs.changePctFor(coinId).first()
            val balance       = prefs.balanceFor(coinId).first()
            val updatedMs     = prefs.updatedMsFor(coinId).first()
            val sparkline     = prefs.sparklineFor(coinId).first()
            val sparklineTs   = prefs.sparklineTsFor(coinId).first()
            val style         = prefs.chartStyle.first()
            val theme         = prefs.appTheme.first().toThemeColors(
                customAccentArgb    = prefs.customAccentArgb.first(),
                customSecondaryArgb = prefs.customSecondaryArgb.first()
            )
            val coinLookup    = buildCoinLookup(coins, widgetCoinIds + coinId)
            val coin          = coinLookup[coinId] ?: CoinRegistry.byId(coinId)

            val cached = WidgetData(
                coinId              = coinId,
                symbol              = coin.symbol,
                priceUsd            = price,
                change24hPct        = change,
                walletBalance       = balance,
                walletValueUsd      = balance * price,
                lastUpdatedMs       = updatedMs,
                sparklinePrices     = sparkline,
                sparklineTimestamps = sparklineTs
            )
            WidgetUpdater.updateAllWidgets(
                context, cached, style, theme,
                widgetCoinIds = widgetCoinIds,
                activeCoinId  = coinId,
                coinLookup    = coinLookup
            )
        }
    }

    private fun fetchDirectly(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val ep       = entryPoint(context)
            val prefs    = ep.widgetPreferences()
            val repo     = ep.cryptoRepository()
            val coins    = ep.coinRepository()
            val alerts   = ep.alertRepository()
            val notifier = ep.alertNotifier()

            val coinId        = prefs.coinId.first()
            val wallet        = prefs.walletAddressFor(coinId).first()
            val style         = prefs.chartStyle.first()
            val theme         = prefs.appTheme.first().toThemeColors(
                customAccentArgb    = prefs.customAccentArgb.first(),
                customSecondaryArgb = prefs.customSecondaryArgb.first()
            )
            val widgetCoinIds = prefs.widgetCoinIds.first()
            val coinLookup    = buildCoinLookup(coins, widgetCoinIds + coinId)

            val result = repo.fetchWidgetData(coinId, wallet)
            val data   = result.getOrElse { e ->
                val errorData = WidgetData(coinId = coinId, errorMessage = e.message ?: "Fetch failed")
                WidgetUpdater.updateAllWidgets(
                    context, errorData, style, theme,
                    widgetCoinIds = widgetCoinIds,
                    activeCoinId  = coinId,
                    coinLookup    = coinLookup
                )
                prefs.recordWorkerResult(e.message)
                return@launch
            }

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
        }
    }

    private fun showRefreshSpinner(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids     = manager.getAppWidgetIds(ComponentName(context, CoinflowWidgetProvider::class.java))
        if (ids.isEmpty()) return

        val textViews = RemoteViews(context.packageName, R.layout.widget_layout)
        textViews.setTextViewText(R.id.tv_last_updated, "Refreshing…")
        ids.forEach { manager.partiallyUpdateAppWidget(it, textViews) }

        val frames = listOf(
            R.drawable.ic_refresh,
            R.drawable.ic_refresh_frame_315,
            R.drawable.ic_refresh_frame_270,
            R.drawable.ic_refresh_frame_225,
            R.drawable.ic_refresh_frame_180,
            R.drawable.ic_refresh_frame_135,
            R.drawable.ic_refresh_frame_90,
            R.drawable.ic_refresh_frame_45
        )

        cancelSpinner()
        spinJob = CoroutineScope(Dispatchers.IO).launch {
            repeat(24) { tick ->
                val frameViews = RemoteViews(context.packageName, R.layout.widget_layout)
                frameViews.setImageViewResource(R.id.btn_refresh, frames[tick % 8])
                frameViews.setInt(R.id.btn_refresh, "setImageAlpha", 180)
                ids.forEach { manager.partiallyUpdateAppWidget(it, frameViews) }
                delay(125)
            }
        }
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
