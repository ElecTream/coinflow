package com.leeam.cryptowidget.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.leeam.cryptowidget.CoinflowApplication.Companion.ALERT_CHANNEL_ID
import com.leeam.cryptowidget.R
import com.leeam.cryptowidget.data.local.AlertDirection
import com.leeam.cryptowidget.data.local.AlertEntity
import com.leeam.cryptowidget.data.local.AlertMode
import com.leeam.cryptowidget.ui.settings.SettingsActivity
import com.leeam.cryptowidget.ui.util.CoinFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nm = context.getSystemService(NotificationManager::class.java)

    fun fireAlertNotification(alert: AlertEntity, currentPriceUsd: Double) {
        // Guard: don't fire if notification permission not granted (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val isAbove = alert.direction == AlertDirection.ABOVE
        val arrow = if (isAbove) "▲" else "▼"

        val thresholdStr = CoinFormatter.formatPrice(alert.thresholdUsd)
        val currentStr   = CoinFormatter.formatPrice(currentPriceUsd)

        val (title, shortBody, bigBody) = when (alert.alertMode) {
            AlertMode.CROSSING -> {
                val crossWord = if (isAbove) "crossed above" else "crossed below"
                Triple(
                    "$arrow ${alert.symbol} $crossWord $thresholdStr",
                    "Now at $currentStr",
                    "${alert.symbol} $crossWord $thresholdStr\nCurrent price: $currentStr"
                )
            }
            AlertMode.REPEATING -> {
                val stateWord = if (isAbove) "above" else "below"
                Triple(
                    "$arrow ${alert.symbol} still $stateWord $thresholdStr",
                    "Now at $currentStr  •  repeating every ${alert.cooldownMin}m",
                    "${alert.symbol} remains $stateWord $thresholdStr\nCurrent price: $currentStr"
                )
            }
            AlertMode.ONE_SHOT -> {
                val dirWord = if (isAbove) "exceeded" else "dropped below"
                Triple(
                    "$arrow ${alert.symbol} price alert",
                    "${alert.symbol} $dirWord $thresholdStr",
                    "${alert.symbol} has $dirWord $thresholdStr\nCurrent price: $currentStr"
                )
            }
        }

        val tapIntent = PendingIntent.getActivity(
            context,
            alert.id,
            Intent(context, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(SettingsActivity.EXTRA_COIN_ID, alert.coinId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val accentColor = if (isAbove) 0xFF00FF88.toInt() else 0xFFFF4466.toInt()

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(shortBody)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(bigBody)
            )
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(accentColor)
            .setColorized(true)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setLights(accentColor, 500, 2000)
            .setGroup(alert.coinId)
            .build()

        nm.notify(alert.id, notification)

        // Group summary — collapses multiple alerts for the same coin on the lock screen / shade.
        // Uses a stable ID derived from the coin id so repeated calls safely overwrite it.
        val summaryId = alert.coinId.hashCode()
        val summaryTapIntent = PendingIntent.getActivity(
            context,
            summaryId,
            Intent(context, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(SettingsActivity.EXTRA_COIN_ID, alert.coinId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val summary = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${alert.symbol} Price Alerts")
            .setGroup(alert.coinId)
            .setGroupSummary(true)
            .setContentIntent(summaryTapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        nm.notify(summaryId, summary)
    }
}
