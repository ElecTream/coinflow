package com.leeam.cryptowidget.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.leeam.cryptowidget.data.local.AlertRepository
import com.leeam.cryptowidget.data.local.WidgetPreferences
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

    override suspend fun doWork(): Result {
        val coinId = widgetPreferences.coinId.first()
        val wallet = widgetPreferences.walletAddress.first()

        return try {
            val result = cryptoRepository.fetchWidgetData(coinId, wallet)
            val data = result.getOrThrow()

            WidgetUpdater.updateAllWidgets(applicationContext, data)

            widgetPreferences.cacheWidgetData(
                priceUsd   = data.priceUsd,
                changePct  = data.change24hPct,
                balanceXrp = data.walletBalanceXrp
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
