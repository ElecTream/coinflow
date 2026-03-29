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
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.data.model.CryptoWidgetData
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
interface CryptoWidgetEntryPoint {
    fun widgetPreferences(): WidgetPreferences
    fun cryptoRepository(): CryptoRepository
    fun alertRepository(): AlertRepository
    fun alertNotifier(): AlertNotifier
}

class CryptoWidgetProvider : AppWidgetProvider() {

    companion object {
        /**
         * Holds the running spin-animation job so WidgetUpdater can cancel it
         * before restoring the static icon, preventing stale frames from
         * overwriting the restored 0° icon.
         */
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
        if (intent.action == "com.leeam.cryptowidget.ACTION_REFRESH") {
            showRefreshSpinner(context)
            fetchDirectly(context)
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
     * Immediately renders cached DataStore data so the widget never shows a blank
     * loading state when data is already available. Falls back to the loading layout
     * only on first-ever install when no cached price exists.
     */
    private fun showCachedOrLoading(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = EntryPointAccessors
                .fromApplication(context.applicationContext, CryptoWidgetEntryPoint::class.java)
                .widgetPreferences()

            val price = prefs.cachedPriceUsd.first()
            if (price <= 0.0) {
                // First install — no cached data yet, show loading layout
                ids.forEach {
                    manager.updateAppWidget(it, RemoteViews(context.packageName, R.layout.widget_loading))
                }
                return@launch
            }

            val coinId    = prefs.coinId.first()
            val change    = prefs.cachedChangePct.first()
            val balance   = prefs.cachedBalance.first()
            val updatedMs = prefs.cachedUpdatedMs.first()
            val sparkline   = prefs.cachedSparkline.first()
            val sparklineTs = prefs.cachedSparklineTimestamps.first()
            val style       = prefs.chartStyle.first()
            val theme     = prefs.appTheme.first().toThemeColors()
            val coin      = CoinRegistry.byId(coinId)

            val cached = CryptoWidgetData(
                coinId         = coinId,
                symbol         = coin.symbol,
                priceUsd       = price,
                change24hPct   = change,
                walletBalance  = balance,
                walletValueUsd = balance * price,
                lastUpdatedMs  = updatedMs,
                sparklinePrices     = sparkline,
                sparklineTimestamps = sparklineTs
            )
            WidgetUpdater.updateAllWidgets(context, cached, style, theme)
        }
    }

    /**
     * Directly fetches fresh data without going through WorkManager, so the widget
     * updates as soon as the network round-trip completes — no JobScheduler latency.
     * Mirrors the logic in PriceUpdateWorker (cache, update widget, check alerts).
     */
    private fun fetchDirectly(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val ep = EntryPointAccessors
                .fromApplication(context.applicationContext, CryptoWidgetEntryPoint::class.java)
            val prefs   = ep.widgetPreferences()
            val repo    = ep.cryptoRepository()
            val alerts  = ep.alertRepository()
            val notifier = ep.alertNotifier()

            val coinId = prefs.coinId.first()
            val wallet = prefs.walletAddressFor(coinId).first()
            val style  = prefs.chartStyle.first()
            val theme  = prefs.appTheme.first().toThemeColors()

            val result = repo.fetchWidgetData(coinId, wallet)
            val data = result.getOrElse { e ->
                val errorData = CryptoWidgetData(coinId = coinId, errorMessage = e.message ?: "Fetch failed")
                WidgetUpdater.updateAllWidgets(context, errorData)
                prefs.recordWorkerResult(e.message)
                return@launch
            }

            WidgetUpdater.updateAllWidgets(context, data, style, theme)
            prefs.cacheWidgetData(
                data.priceUsd, data.change24hPct, data.walletBalance,
                data.sparklinePrices, data.sparklineTimestamps
            )
            alerts.checkAndFireAlerts(coinId, data.priceUsd) { alert ->
                notifier.fireAlertNotification(alert, data.priceUsd)
            }
            prefs.recordWorkerResult(null)
        }
    }

    /**
     * Immediately shows "Refreshing…" in the timestamp and animates the refresh icon
     * by cycling through 4 static rotation-frame drawables at 250ms intervals.
     * The job is stored in [spinJob] so WidgetUpdater can cancel it before restoring
     * the static 0° icon, avoiding stale frames landing after the update completes.
     */
    private fun showRefreshSpinner(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, CryptoWidgetProvider::class.java))
        if (ids.isEmpty()) return

        // Instant text feedback
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

        cancelSpinner() // cancel any previous loop before starting a new one
        spinJob = CoroutineScope(Dispatchers.IO).launch {
            repeat(24) { tick -> // 3s safety cap — cancelled immediately when fetch completes
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
}
