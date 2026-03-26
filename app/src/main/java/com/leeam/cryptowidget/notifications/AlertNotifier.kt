package com.leeam.cryptowidget.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.leeam.cryptowidget.CryptoWidgetApplication.Companion.ALERT_CHANNEL_ID
import com.leeam.cryptowidget.R
import com.leeam.cryptowidget.data.local.AlertDirection
import com.leeam.cryptowidget.data.local.AlertEntity
import com.leeam.cryptowidget.ui.settings.SettingsActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nm = context.getSystemService(NotificationManager::class.java)

    fun fireAlertNotification(alert: AlertEntity, currentPriceUsd: Double) {
        val directionWord = when (alert.direction) {
            AlertDirection.ABOVE -> "exceeded"
            AlertDirection.BELOW -> "dropped below"
        }
        val title = "${alert.symbol} Price Alert"
        val body = "${alert.symbol} has $directionWord \$${
            String.format(Locale.US, "%.4f", alert.thresholdUsd)
        }! Current: \$${String.format(Locale.US, "%.4f", currentPriceUsd)}"

        val tapIntent = PendingIntent.getActivity(
            context,
            alert.id,
            Intent(context, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF00D4FF.toInt())
            .setColorized(true)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .build()

        nm.notify(alert.id, notification)
    }
}
