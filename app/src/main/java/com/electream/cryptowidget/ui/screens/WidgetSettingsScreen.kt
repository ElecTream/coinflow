package com.electream.cryptowidget.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.electream.cryptowidget.data.local.ChartStyle
import com.electream.cryptowidget.ui.components.*
import com.electream.cryptowidget.ui.settings.SettingsViewModel
import com.electream.cryptowidget.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.saveSuccess, state.saveError) {
        when {
            state.saveSuccess  -> { snackbar.showSnackbar("Settings saved"); vm.clearFeedback() }
            state.saveError != null -> { snackbar.showSnackbar("Error: ${state.saveError}"); vm.clearFeedback() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Widget Settings", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LocalThemeColors.current.accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = BgDark
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Refresh interval
                CryptoCard {
                    SectionLabel("REFRESH INTERVAL")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h", 360 to "6h").forEach { (min, label) ->
                            CryptoFilterChip(
                                selected = state.refreshIntervalMin == min,
                                label    = label,
                                onClick  = { vm.onIntervalChange(min) }
                            )
                        }
                    }
                }

                // Sparkline + chart style
                CryptoCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Show Sparkline",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.showSparkline,
                            onCheckedChange = vm::onShowSparklineChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor    = LocalThemeColors.current.accent,
                                checkedTrackColor    = Surface,
                                checkedBorderColor   = LocalThemeColors.current.accent,
                                uncheckedThumbColor  = TextSecondary,
                                uncheckedTrackColor  = SwitchTrackOff,
                                uncheckedBorderColor = CardBorder
                            )
                        )
                    }
                    AnimatedVisibility(visible = state.showSparkline) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            SectionLabel("CHART STYLE")
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(ChartStyle.AREA to "Area", ChartStyle.LINE to "Line", ChartStyle.CANDLE to "Candle").forEach { (style, label) ->
                                    CryptoFilterChip(
                                        selected = state.chartStyle == style,
                                        label    = label,
                                        onClick  = { vm.onChartStyleChange(style) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Diagnostics
                if (state.lastWorkerRunMs > 0L) {
                    CryptoCard {
                        SectionLabel("DIAGNOSTICS")
                        Spacer(Modifier.height(10.dp))
                        val fmt = SimpleDateFormat("HH:mm:ss MMM d", Locale.US)
                        Text(
                            "Last update: ${fmt.format(Date(state.lastWorkerRunMs))}",
                            color = TextSecondary, fontSize = 12.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        if (state.lastErrorMsg != null) {
                            Text("Error: ${state.lastErrorMsg}", color = ColorDown, fontSize = 12.sp)
                        } else {
                            Text("Worker OK", color = ColorUp, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Sticky save button
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                SaveButton(label = "Save & Update Widget", onClick = vm::save)
            }
        }
    }
}
