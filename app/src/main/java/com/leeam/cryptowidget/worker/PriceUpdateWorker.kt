package com.leeam.cryptowidget.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.leeam.cryptowidget.CoinflowApplication.Companion.ALERT_CHANNEL_ID
import com.leeam.cryptowidget.R
import com.leeam.cryptowidget.data.local.AlertRepository
import com.leeam.cryptowidget.data.local.WidgetPreferences
import com.leeam.cryptowidget.data.model.WidgetData
import com.leeam.cryptowidget.data.repository.CryptoRepository
import com.leeam.cryptowidget.notifications.AlertNotifier
import com.leeam.cryptowidget.ui.theme.toThemeColors
import com.leeam.cryptowidget.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlin.math.max

@HiltWorker
class PriceUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val cryptoRepository: CryptoRepository,
    private val widgetPreferences: WidgetPreferences,
    private val alertRepository: AlertRepository,
    private val alertNotifier: AlertNotifier
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
        val intervalMin     = widgetPreferences.refreshIntervalMin.first()
        val activeCoinId    = widgetPreferences.coinId.first()
        val widgetCoinIds   = widgetPreferences.widgetCoinIds.first()
        val followedCoinIds = widgetPreferences.followedCoinIds.first()
        val chartStyle      = widgetPreferences.chartStyle.first()
        val themeColors     = widgetPreferences.appTheme.first().toThemeColors(
            customAccentArgb    = widgetPreferences.customAccentArgb.first(),
            customSecondaryArgb = widgetPreferences.customSecondaryArgb.first()
        )

        // Widget coins refresh at user interval; background coins at 4× (min 60 min)
        val widgetThresholdMs     = intervalMin * 60_000L
        val backgroundThresholdMs = max(4 * intervalMin, 60) * 60_000L

        val now = System.currentTimeMillis()
        var lastError: String? = null

        for (coinId in followedCoinIds) {
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

                    if (coinId == activeCoinId) {
                        WidgetUpdater.updateAllWidgets(
                            applicationContext, data, chartStyle, themeColors,
                            widgetCoinIds = widgetCoinIds,
                            activeCoinId  = coinId
                        )
                    }

                    alertRepository.checkAndFireAlerts(coinId, data.priceUsd) { alert ->
                        alertNotifier.fireAlertNotification(alert, data.priceUsd)
                    }
                },
                onFailure = { e ->
                    lastError = e.message
                    if (coinId == activeCoinId) {
                        val errorData = WidgetData(coinId = coinId, errorMessage = e.message ?: "Fetch failed")
                        try { WidgetUpdater.updateAllWidgets(applicationContext, errorData) } catch (_: Exception) {}
                    }
                }
            )
        }

        widgetPreferences.recordWorkerResult(lastError)
        return if (lastError == null) Result.success() else Result.retry()
    }
}
