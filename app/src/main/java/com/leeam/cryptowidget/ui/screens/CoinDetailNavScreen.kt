package com.leeam.cryptowidget.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.leeam.cryptowidget.data.local.AlertMode
import com.leeam.cryptowidget.data.model.CoinRegistry
import com.leeam.cryptowidget.data.model.WalletConfig
import com.leeam.cryptowidget.ui.components.*
import com.leeam.cryptowidget.ui.settings.SettingsViewModel
import com.leeam.cryptowidget.ui.theme.*
import com.leeam.cryptowidget.ui.util.CoinFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailNavScreen(
    vm: SettingsViewModel,
    coinId: String,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()

    // Sync ViewModel to this coin when the screen opens
    LaunchedEffect(coinId) {
        vm.onCoinChange(coinId)
    }

    // Resolve coin: check custom coins first (in state), fall back to built-in registry.
    val coin = state.customCoins.firstOrNull { it.id == coinId } ?: CoinRegistry.byId(coinId)
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.saveSuccess, state.saveError) {
        when {
            state.saveSuccess       -> { snackbar.showSnackbar("Saved"); vm.clearFeedback() }
            state.saveError != null -> { snackbar.showSnackbar("Error: ${state.saveError}"); vm.clearFeedback() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(coin.displayName, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(coin.symbol, color = LocalThemeColors.current.accent, fontSize = 11.sp)
                    }
                },
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
                // Wallet card — only shown for coins with wallet support
                AnimatedVisibility(
                    visible = coin.walletConfig !is WalletConfig.None,
                    enter   = fadeIn() + slideInVertically(),
                    exit    = fadeOut() + slideOutVertically()
                ) {
                    CryptoCard {
                        SectionLabel("WALLET ADDRESS")
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value         = state.walletAddress,
                            onValueChange = vm::onWalletChange,
                            placeholder   = {
                                Text(
                                    "Enter ${coin.symbol} wallet address",
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            modifier     = Modifier.fillMaxWidth(),
                            singleLine   = true,
                            textStyle    = LocalTextStyle.current.copy(
                                color      = LocalThemeColors.current.accent,
                                fontFamily = FontFamily.Monospace,
                                fontSize   = 13.sp
                            ),
                            colors          = cryptoTextFieldColors(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                when {
                                    state.walletTestLoading ->
                                        CircularProgressIndicator(
                                            modifier    = Modifier.size(16.dp),
                                            color       = LocalThemeColors.current.accent,
                                            strokeWidth = 2.dp
                                        )
                                    state.walletTestResult != null -> {
                                        val ok = state.walletTestResult!!.startsWith("OK")
                                        Text(
                                            state.walletTestResult!!,
                                            color    = if (ok) ColorUp else ColorDown,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                            CryptoOutlinedButton("Verify Balance") { vm.testWallet() }
                        }
                    }
                }

                // Price alerts
                CryptoCard {
                    SectionLabel("PRICE ALERTS")
                    Spacer(Modifier.height(12.dp))

                    // Direction + threshold + add
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(AlertDirection.ABOVE to "Above", AlertDirection.BELOW to "Below").forEach { (dir, label) ->
                                FilterChip(
                                    selected = state.newAlertDirection == dir,
                                    onClick  = { vm.onNewAlertDirection(dir) },
                                    label    = { Text(label, fontSize = 11.sp) },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LocalThemeColors.current.secondary,
                                        selectedLabelColor     = TextPrimary,
                                        containerColor         = Surface,
                                        labelColor             = TextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true, selected = state.newAlertDirection == dir,
                                        borderColor = CardBorder, selectedBorderColor = LocalThemeColors.current.secondary
                                    )
                                )
                            }
                        }
                        OutlinedTextField(
                            value         = state.newAlertThreshold,
                            onValueChange = vm::onNewAlertThreshold,
                            placeholder   = { Text("0.00", color = TextSecondary, fontSize = 11.sp) },
                            modifier      = Modifier.weight(1f).height(52.dp),
                            singleLine    = true,
                            colors        = cryptoTextFieldColors(),
                            textStyle     = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix        = { Text("\$", color = TextSecondary, fontSize = 12.sp) }
                        )
                        Button(
                            onClick = vm::addAlert,
                            colors  = ButtonDefaults.buttonColors(containerColor = LocalThemeColors.current.accent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("+", color = BgDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Alert mode selector
                    SectionLabel("ALERT MODE")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf(
                            AlertMode.CROSSING  to "Crossing",
                            AlertMode.REPEATING to "Repeating",
                            AlertMode.ONE_SHOT  to "One-shot"
                        ).forEach { (mode, label) ->
                            CryptoFilterChip(
                                selected = state.newAlertMode == mode,
                                label    = label,
                                onClick  = { vm.onNewAlertMode(mode) }
                            )
                        }
                    }

                    AnimatedVisibility(visible = state.newAlertMode == AlertMode.REPEATING) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            SectionLabel("COOLDOWN")
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                listOf(15 to "15m", 30 to "30m", 60 to "1h", 240 to "4h", 720 to "12h", 1440 to "24h").forEach { (min, label) ->
                                    FilterChip(
                                        selected = state.newAlertCooldownMin == min,
                                        onClick  = { vm.onNewAlertCooldown(min) },
                                        label    = { Text(label, fontSize = 11.sp) },
                                        colors   = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = LocalThemeColors.current.secondary,
                                            selectedLabelColor     = TextPrimary,
                                            containerColor         = Surface,
                                            labelColor             = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true, selected = state.newAlertCooldownMin == min,
                                            borderColor = CardBorder, selectedBorderColor = LocalThemeColors.current.secondary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (state.newAlertMode) {
                            AlertMode.CROSSING  -> "Fires each time price crosses the threshold. Re-arms automatically."
                            AlertMode.REPEATING -> "Fires repeatedly while condition holds, respecting cooldown."
                            AlertMode.ONE_SHOT  -> "Fires once, then disables. Re-enable manually to reuse."
                        },
                        color = TextSecondary, fontSize = 11.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    val coinAlerts = state.alerts.filter { it.coinId == coinId }
                    if (coinAlerts.isEmpty()) {
                        Text("No alerts set for ${coin.symbol}", color = TextSecondary, fontSize = 12.sp)
                    } else {
                        coinAlerts.forEach { alert ->
                            AlertRow(
                                alert    = alert,
                                onToggle = { vm.toggleAlert(alert, it) },
                                onDelete = { vm.deleteAlert(alert) }
                            )
                            HorizontalDivider(color = CardBorder, thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Save button
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                SaveButton(label = "Save Wallet Address", onClick = vm::save)
            }
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
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked        = alert.isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = LocalThemeColors.current.accent,
                checkedTrackColor    = Surface,
                checkedBorderColor   = LocalThemeColors.current.accent,
                uncheckedThumbColor  = TextSecondary,
                uncheckedTrackColor  = SwitchTrackOff,
                uncheckedBorderColor = CardBorder
            )
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            val dirLabel = if (alert.direction == AlertDirection.ABOVE) "Above" else "Below"
            Text(
                "$dirLabel ${CoinFormatter.formatPrice(alert.thresholdUsd)}",
                color    = if (alert.isEnabled) TextPrimary else TextSecondary,
                fontSize = 13.sp
            )
            val modeBadge = when (alert.alertMode) {
                AlertMode.CROSSING  -> "crossing"
                AlertMode.REPEATING -> "every ${alert.cooldownMin}m"
                AlertMode.ONE_SHOT  -> "one-shot"
            }
            Text(modeBadge, color = LocalThemeColors.current.accent, fontSize = 10.sp)
        }
        if (alert.firedAtMs != null && alert.alertMode == AlertMode.ONE_SHOT) {
            Text("Fired", color = LocalThemeColors.current.secondary, fontSize = 10.sp, modifier = Modifier.padding(end = 6.dp))
        }
        TextButton(onClick = onDelete, contentPadding = PaddingValues(4.dp)) {
            Text("✕", color = ColorDown, fontSize = 14.sp)
        }
    }
}
