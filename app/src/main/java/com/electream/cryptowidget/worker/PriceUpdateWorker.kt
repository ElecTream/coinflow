package com.electream.cryptowidget.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.electream.cryptowidget.CoinflowApplication.Companion.ALERT_CHANNEL_ID
import com.electream.cryptowidget.R
import com.electream.cryptowidget.data.local.AlertRepository
import com.electream.cryptowidget.data.local.DebugLog
import com.electream.cryptowidget.data.local.WidgetPreferences
import com.electream.cryptowidget.data.model.CoinRegistry
import com.electream.cryptowidget.data.model.WidgetData
import com.electream.cryptowidget.data.repository.CoinRepository
import com.electream.cryptowidget.data.repository.CryptoRepository
import com.electream.cryptowidget.notifications.AlertNotifier
import com.electream.cryptowidget.ui.theme.toThemeColors
import com.electream.cryptowidget.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlin.math.max

@HiltWorker
class PriceUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cryptoRepository: CryptoRepository,
    private val coinRepository: CoinRepository,
    private val widgetPreferences: WidgetPreferences,
    private val alertRepository: AlertRepository,
    private val alertNotifier: AlertNotifier,
    private val debugLog: DebugLog
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val NOTIFICATION_ID_REFRESH = 9001
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, ALERT_CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText("Updating prices…")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID_REFRESH, notification)
    }

    override suspend fun doWork(): Result {
        debugLog.info("Worker.doWork", "started")
        return runCatching { doWorkInner() }
            .onFailure { debugLog.error("Worker.doWork", "uncaught failure", it) }
            .getOrElse { Result.retry() }
    }

    private suspend fun doWorkInner(): Result {
        val intervalMin     = widgetPreferences.refreshIntervalMin.first()
        val activeCoinId    = widgetPreferences.coinId.first()
        val widgetCoinIds   = widgetPreferences.widgetCoinIds.first()
        val followedCoinIds = widgetPreferences.followedCoinIds.first()

        // Early exit: nothing to do until the user picks coins.
        if (followedCoinIds.isEmpty()) {
            debugLog.info("Worker", "no coins followed — skipping")
            widgetPreferences.recordWorkerResult(null)
            return Result.success()
        }

        val chartStyle      = widgetPreferences.chartStyle.first()
        val themeColors     = widgetPreferences.appTheme.first().toThemeColors(
            customAccentArgb    = widgetPreferences.customAccentArgb.first(),
            customSecondaryArgb = widgetPreferences.customSecondaryArgb.first()
        )
        val coinLookup = (widgetCoinIds + followedCoinIds + activeCoinId)
            .filter { it.isNotEmpty() }
            .distinct()
            .associateWith {
                coinRepository.coinById(it) ?: CoinRegistry.byId(it)
            }

        // Widget coins refresh at user interval; background coins at 4× (min 60 min)
        val widgetThresholdMs     = intervalMin * 60_000L
        val backgroundThresholdMs = max(4 * intervalMin, 60) * 60_000L

        val now = System.currentTimeMillis()
        var lastError: String? = null

        // Active coin first so a fresh widget renders ASAP after a reinstall.
        val orderedCoinIds =
            if (activeCoinId.isNotEmpty()) (listOf(activeCoinId) + followedCoinIds).distinct()
            else followedCoinIds

        for (coinId in orderedCoinIds) {
            val isWidgetCoin = coinId in widgetCoinIds
            val threshold    = if (isWidgetCoin) widgetThresholdMs else backgroundThresholdMs
            val lastFetched  = widgetPreferences.lastFetchedMsFor(coinId).first()

            if (lastFetched > 0L && (now - lastFetched) < threshold) continue

            val wallet = widgetPreferences.walletAddressFor(coinId).first()

            cryptoRepository.fetchWidgetData(coinId, wallet).fold(
                onSuccess = { data ->
                    widgetPreferences.cacheCoinData(
                        coinId              = coinId,
                        priceUsd            = data.priceUsd,
                        changePct           = data.change24hPct,
                        balance             = data.walletBalance,
                        sparkline           = data.sparklinePrices,
                        sparklineTimestamps = data.sparklineTimestamps
                    )
                    widgetPreferences.setLastFetchError(coinId, null)
                    debugLog.info("Worker.fetch", "ok $coinId price=${data.priceUsd}")

                    if (activeCoinId.isNotEmpty() && coinId == activeCoinId) {
                        try {
                            WidgetUpdater.updateAllWidgets(
                                applicationContext, data, chartStyle, themeColors,
                                widgetCoinIds = widgetCoinIds.ifEmpty { listOf(coinId) },
                                activeCoinId  = coinId,
                                coinLookup    = coinLookup
                            )
                        } catch (e: Exception) {
                            debugLog.error("Worker.render", "failed for $coinId", e)
                        }
                    }

                    alertRepository.checkAndFireAlerts(coinId, data.priceUsd) { alert ->
                        alertNotifier.fireAlertNotification(alert, data.priceUsd)
                    }
                },
                onFailure = { e ->
                    lastError = e.message
                    widgetPreferences.setLastFetchError(coinId, e.message ?: "Fetch failed")
                    debugLog.error("Worker.fetch", "failed $coinId", e)
                    if (activeCoinId.isNotEmpty() && coinId == activeCoinId) {
                        val errorData = WidgetData(coinId = coinId, errorMessage = e.message ?: "Fetch failed")
                        try {
                            WidgetUpdater.updateAllWidgets(
                                applicationContext, errorData, chartStyle, themeColors,
                                widgetCoinIds = widgetCoinIds.ifEmpty { listOf(coinId) },
                                activeCoinId  = coinId,
                                coinLookup    = coinLookup
                            )
                        } catch (renderError: Exception) {
                            debugLog.error("Worker.render", "error-render failed $coinId", renderError)
                        }
                    }
                }
            )
        }

        // Trailing render: re-paint the active coin from its cache regardless of whether
        // it was fetched this run. Covers the case where the active coin's fetch was
        // skipped (threshold not elapsed) but the on-screen "Xm ago" is now stale.
        if (activeCoinId.isNotEmpty()) {
            runCatching {
                val cached = widgetPreferences.snapshotFor(activeCoinId)
                WidgetUpdater.updateAllWidgets(
                    applicationContext, cached, chartStyle, themeColors,
                    widgetCoinIds = widgetCoinIds.ifEmpty { listOf(activeCoinId) },
                    activeCoinId  = activeCoinId,
                    coinLookup    = coinLookup
                )
            }.onFailure { debugLog.error("Worker.trailingRender", "failed for $activeCoinId", it) }
        }

        widgetPreferences.recordWorkerResult(lastError)
        return if (lastError == null) Result.success() else Result.retry()
    }
}
