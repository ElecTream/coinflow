package com.leeam.cryptowidget

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.leeam.cryptowidget.data.local.WidgetPreferences
import com.leeam.cryptowidget.data.local.WidgetPreferencesBootstrap
import com.leeam.cryptowidget.worker.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CoinflowApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workScheduler: WorkScheduler
    @Inject lateinit var bootstrap: WidgetPreferencesBootstrap
    @Inject lateinit var widgetPrefs: WidgetPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        appScope.launch {
            // Run migrations BEFORE the worker is allowed to write per-coin keys.
            bootstrap.runIfNeeded()
            // Periodic schedule is always on so it's ready the moment the user picks coins.
            workScheduler.schedulePeriodicRefresh(15)
            // Only kick off an immediate fetch if there's something to fetch.
            if (widgetPrefs.followedCoinIds.first().isNotEmpty()) {
                workScheduler.triggerImmediateRefresh()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFF00D4FF.toInt()
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val ALERT_CHANNEL_ID = "crypto_alerts"
        const val WORK_TAG_PERIODIC = "crypto_periodic_refresh"
    }
}
