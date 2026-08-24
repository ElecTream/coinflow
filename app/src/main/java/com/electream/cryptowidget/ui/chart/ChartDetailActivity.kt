package com.electream.cryptowidget.ui.chart

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.electream.cryptowidget.data.local.WidgetPreferences
import com.electream.cryptowidget.ui.settings.SettingsActivity
import com.electream.cryptowidget.ui.theme.CoinflowTheme
import com.electream.cryptowidget.ui.theme.toThemeColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class ChartDetailActivity : ComponentActivity() {

    @Inject lateinit var prefs: WidgetPreferences

    private val vm: ChartDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Read the persisted theme synchronously before first frame to avoid a flash.
        val themeColors = runBlocking {
            prefs.appTheme.first().toThemeColors(
                customAccentArgb    = prefs.customAccentArgb.first(),
                customSecondaryArgb = prefs.customSecondaryArgb.first()
            )
        }

        setContent {
            CoinflowTheme(themeColors = themeColors) {
                ChartDetailScreen(
                    vm         = vm,
                    onBack     = { finish() },
                    onSettings = {
                        startActivity(
                            Intent(this, SettingsActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                        )
                    }
                )
            }
        }
    }
}
