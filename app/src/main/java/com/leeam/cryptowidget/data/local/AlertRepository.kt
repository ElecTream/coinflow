package com.leeam.cryptowidget.data.local

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(private val dao: AlertDao) {

    fun getAllAlerts(): Flow<List<AlertEntity>> = dao.getAllAlerts()

    suspend fun getEnabledAlertsForCoin(coinId: String): List<AlertEntity> =
        dao.getEnabledAlertsForCoin(coinId)

    suspend fun addAlert(
        coinId: String,
        symbol: String,
        direction: AlertDirection,
        thresholdUsd: Double
    ): Long = dao.insertAlert(
        AlertEntity(
            coinId = coinId,
            symbol = symbol,
            direction = direction,
            thresholdUsd = thresholdUsd
        )
    )

    suspend fun updateAlert(alert: AlertEntity) = dao.updateAlert(alert)
    suspend fun deleteAlert(alert: AlertEntity) = dao.deleteAlert(alert)
    suspend fun markAlertFired(id: Int) = dao.markAlertFired(id, System.currentTimeMillis())
    suspend fun setAlertEnabled(id: Int, enabled: Boolean) = dao.setAlertEnabled(id, enabled)

    suspend fun checkAndFireAlerts(
        coinId: String,
        currentPriceUsd: Double,
        onFire: suspend (AlertEntity) -> Unit
    ) {
        val enabled = getEnabledAlertsForCoin(coinId)
        for (alert in enabled) {
            val triggered = when (alert.direction) {
                AlertDirection.ABOVE -> currentPriceUsd >= alert.thresholdUsd
                AlertDirection.BELOW -> currentPriceUsd <= alert.thresholdUsd
            }
            if (triggered) {
                onFire(alert)
                dao.setAlertEnabled(alert.id, false)
                dao.markAlertFired(alert.id, System.currentTimeMillis())
            }
        }
    }
}
