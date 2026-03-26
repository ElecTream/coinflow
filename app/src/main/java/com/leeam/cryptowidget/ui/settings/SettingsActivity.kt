package com.leeam.cryptowidget.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leeam.cryptowidget.data.local.AlertDirection
import com.leeam.cryptowidget.data.local.AlertEntity
import com.leeam.cryptowidget.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    private val vm: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CryptoWidgetTheme {
                SettingsScreen(vm)
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: SettingsViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.saveSuccess, state.saveError) {
        when {
            state.saveSuccess -> {
                snackbar.showSnackbar("Saved! Widget updating...")
                vm.clearFeedback()
            }
            state.saveError != null -> {
                snackbar.showSnackbar("Error: ${state.saveError}")
                vm.clearFeedback()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Text("Crypto Widget", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("XRP Price Tracker", color = TextSecondary, fontSize = 13.sp)

            // Live price card
            CryptoCard {
                SectionLabel("LIVE PRICE")
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        state.priceLoading ->
                            CircularProgressIndicator(color = AccentCyan, modifier = Modifier.size(24.dp))
                        state.priceError != null ->
                            Text("Error: ${state.priceError}", color = ColorDown, fontSize = 12.sp)
                        state.livePrice != null -> {
                            Column {
                                Text(
                                    "\$${String.format(Locale.US, "%.4f", state.livePrice!!)}",
                                    color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold
                                )
                                state.change24h?.let { change ->
                                    val upColor = if (change >= 0) ColorUp else ColorDown
                                    val arrow = if (change >= 0) "▲" else "▼"
                                    Text(
                                        "$arrow ${String.format(Locale.US, "%.2f", abs(change))}% 24h",
                                        color = upColor, fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CryptoOutlinedButton("↻ Refresh") { vm.fetchLivePrice() }
                        CryptoOutlinedButton("⚡ Force Widget") { vm.save() }
                    }
                }
            }

            // Wallet card
            CryptoCard {
                SectionLabel("WALLET ADDRESS (XRP)")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.walletAddress,
                    onValueChange = vm::onWalletChange,
                    placeholder = { Text("rXXXXXXXXXXX...", color = TextSecondary, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = AccentCyan, fontFamily = FontFamily.Monospace, fontSize = 13.sp
                    ),
                    colors = cryptoTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        when {
                            state.walletTestLoading ->
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentCyan, strokeWidth = 2.dp)
                            state.walletTestResult != null -> {
                                val ok = state.walletTestResult!!.startsWith("OK")
                                Text(state.walletTestResult!!, color = if (ok) ColorUp else ColorDown, fontSize = 11.sp)
                            }
                        }
                    }
                    CryptoOutlinedButton("Test Balance") { vm.testWallet() }
                }
            }

            // Refresh interval card
            CryptoCard {
                SectionLabel("REFRESH INTERVAL")
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h", 360 to "6h").forEach { (min, label) ->
                        FilterChip(
                            selected = state.refreshIntervalMin == min,
                            onClick = { vm.onIntervalChange(min) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentCyan,
                                selectedLabelColor = BgDark,
                                containerColor = Surface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Sparkline toggle
            CryptoCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Show Sparkline Chart", color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.showSparkline,
                        onCheckedChange = vm::onShowSparklineChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = Surface
                        )
                    )
                }
            }

            // Price alerts card
            CryptoCard {
                SectionLabel("PRICE ALERTS")
                Spacer(Modifier.height(10.dp))

                // Add new alert row
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Direction chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(AlertDirection.ABOVE, AlertDirection.BELOW).forEach { dir ->
                            FilterChip(
                                selected = state.newAlertDirection == dir,
                                onClick = { vm.onNewAlertDirection(dir) },
                                label = { Text(dir.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPurple,
                                    selectedLabelColor = TextPrimary,
                                    containerColor = Surface,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = state.newAlertThreshold,
                        onValueChange = vm::onNewAlertThreshold,
                        placeholder = { Text("Price", color = TextSecondary, fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        singleLine = true,
                        colors = cryptoTextFieldColors(),
                        textStyle = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("\$", color = TextSecondary, fontSize = 12.sp) }
                    )
                    Button(
                        onClick = vm::addAlert,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("+", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (state.alerts.isEmpty()) {
                    Text("No alerts set", color = TextSecondary, fontSize = 12.sp)
                } else {
                    state.alerts.forEach { alert ->
                        AlertRow(
                            alert    = alert,
                            onToggle = { vm.toggleAlert(alert, it) },
                            onDelete = { vm.deleteAlert(alert) }
                        )
                        HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                    }
                }
            }

            // Diagnostics card
            if (state.lastWorkerRunMs > 0L) {
                CryptoCard {
                    SectionLabel("DIAGNOSTICS")
                    Spacer(Modifier.height(4.dp))
                    val fmt = SimpleDateFormat("HH:mm:ss MMM d", Locale.US)
                    Text("Last update: ${fmt.format(Date(state.lastWorkerRunMs))}", color = TextSecondary, fontSize = 11.sp)
                    if (state.lastErrorMsg != null) {
                        Text("Error: ${state.lastErrorMsg}", color = ColorDown, fontSize = 11.sp)
                    } else {
                        Text("Status: OK", color = ColorUp, fontSize = 11.sp)
                    }
                }
            }

            // Save button
            Button(
                onClick = vm::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save & Update Widget", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AlertRow(
    alert: AlertEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = alert.isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentCyan,
                checkedTrackColor = Surface
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "${alert.direction.name.lowercase().replaceFirstChar { it.uppercase() }} \$${
                String.format(Locale.US, "%.4f", alert.thresholdUsd)
            }",
            color = if (alert.isEnabled) TextPrimary else TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        if (alert.firedAtMs != null) {
            Text("Fired", color = AccentPurple, fontSize = 10.sp, modifier = Modifier.padding(end = 6.dp))
        }
        TextButton(onClick = onDelete, contentPadding = PaddingValues(4.dp)) {
            Text("✕", color = ColorDown, fontSize = 14.sp)
        }
    }
}

@Composable
private fun CryptoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)

@Composable
private fun CryptoOutlinedButton(label: String, onClick: () -> Unit) =
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
        border = BorderStroke(1.dp, AccentCyan),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) { Text(label, fontSize = 11.sp) }

@Composable
private fun cryptoTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = CardBorder,
    cursorColor = AccentCyan,
    focusedLabelColor = AccentCyan
)
