package com.leeam.cryptowidget.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.leeam.cryptowidget.CryptoWidgetApplication.Companion.ALERT_CHANNEL_ID
import com.leeam.cryptowidget.R
import com.leeam.cryptowidget.data.local.AlertRepository
import com.leeam.cryptowidget.data.local.WidgetPreferences
import com.leeam.cryptowidget.ui.theme.toThemeColors
import com.leeam.cryptowidget.data.model.CryptoWidgetData
import com.leeam.cryptowidget.data.repository.CryptoRepository
import com.leeam.cryptowidget.notifications.AlertNotifier
import com.leeam.cryptowidget.widget.WidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

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
            .setContentText("Updating price…")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID_REFRESH, notification)
    }

    override suspend fun doWork(): Result {
        val coinId = widgetPreferences.coinId.first()
        val wallet = widgetPreferences.walletAddressFor(coinId).first()
        val chartStyle = widgetPreferences.chartStyle.first()
        val themeColors = widgetPreferences.appTheme.first().toThemeColors()

        return try {
            val result = cryptoRepository.fetchWidgetData(coinId, wallet)
            val data = result.getOrThrow()

            WidgetUpdater.updateAllWidgets(applicationContext, data, chartStyle, themeColors)

            widgetPreferences.cacheWidgetData(
                priceUsd            = data.priceUsd,
                changePct           = data.change24hPct,
                balance             = data.walletBalance,
                sparkline           = data.sparklinePrices,
                sparklineTimestamps = data.sparklineTimestamps
            )

            alertRepository.checkAndFireAlerts(coinId, data.priceUsd) { alert ->
                alertNotifier.fireAlertNotification(alert, data.priceUsd)
            }

            widgetPreferences.recordWorkerResult(null)
            Result.success()

        } catch (e: Exception) {
            val errorData = CryptoWidgetData(
                coinId = coinId,
                errorMessage = e.message ?: "Fetch failed"
            )
            try {
                WidgetUpdater.updateAllWidgets(applicationContext, errorData)
            } catch (_: Exception) { }

            widgetPreferences.recordWorkerResult(e.message)
            Result.retry()
        }
    }
}
