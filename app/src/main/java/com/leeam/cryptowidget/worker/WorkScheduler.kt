package com.leeam.cryptowidget.worker

import android.content.Context
import androidx.work.*
import com.leeam.cryptowidget.CryptoWidgetApplication.Companion.WORK_TAG_PERIODIC
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val WORK_TAG_IMMEDIATE = "price_refresh_immediate"

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val wm = WorkManager.getInstance(context)

    fun schedulePeriodicRefresh(intervalMinutes: Int) {
        val safeInterval = intervalMinutes.coerceAtLeast(15).toLong()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<PriceUpdateWorker>(
            safeInterval, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WORK_TAG_PERIODIC)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        wm.enqueueUniquePeriodicWork(
            WORK_TAG_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun triggerImmediateRefresh() {
        val request = OneTimeWorkRequestBuilder<PriceUpdateWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        wm.enqueueUniqueWork(WORK_TAG_IMMEDIATE, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll() {
        wm.cancelAllWorkByTag(WORK_TAG_PERIODIC)
    }
}
