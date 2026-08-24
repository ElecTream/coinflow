package com.electream.cryptowidget.data.local

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
        thresholdUsd: Double,
        alertMode: AlertMode = AlertMode.CROSSING,
        cooldownMin: Int = 60
    ): Long = dao.insertAlert(
        AlertEntity(
            coinId = coinId,
            symbol = symbol,
            direction = direction,
            thresholdUsd = thresholdUsd,
            alertMode = alertMode,
            cooldownMin = cooldownMin
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
        val now = System.currentTimeMillis()

        for (alert in enabled) {
            // Which side of the threshold is the current price on?
            val currentSide = if (currentPriceUsd >= alert.thresholdUsd) "ABOVE" else "BELOW"
            val targetSide = alert.direction.name // "ABOVE" or "BELOW"

            when (alert.alertMode) {
                AlertMode.ONE_SHOT -> {
                    if (currentSide == targetSide) {
                        onFire(alert)
                        dao.setAlertEnabled(alert.id, false)
                        dao.markAlertFired(alert.id, now)
                    }
                }

                AlertMode.REPEATING -> {
                    if (currentSide == targetSide) {
                        val lastFired = alert.firedAtMs
                        val cooldownMs = alert.cooldownMin * 60_000L
                        if (lastFired == null || (now - lastFired) >= cooldownMs) {
                            onFire(alert)
                            dao.markAlertFired(alert.id, now)
                        }
                    }
                }

                AlertMode.CROSSING -> {
                    val lastSide = alert.lastKnownSide
                    // Fire only when price transitions from the opposite side into the target side.
                    // On the very first check (lastSide == null) we just record the side — no fire.
                    if (lastSide != null && lastSide != currentSide && currentSide == targetSide) {
                        onFire(alert)
                        dao.markAlertFired(alert.id, now)
                    }
                    // Always update last known side so the next crossing can be detected.
                    dao.updateLastKnownSide(alert.id, currentSide)
                }
            }
        }
    }
}
