package com.leeam.cryptowidget.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leeam.cryptowidget.ui.components.*
import com.leeam.cryptowidget.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomCoinScreen(onBack: () -> Unit) {
    val vm: AddCoinViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val filteredPairs by vm.filteredPairs.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.saveError) {
        state.saveError?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Coin", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = LocalThemeColors.current.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = BgDark
    ) { padding ->
        // In Kraken mode before a pair is selected, the list takes over the screen.
        // In all other states we show the scrollable form.
        if (state.mode == AddCoinMode.KRAKEN && state.selectedPair == null) {
            KrakenSearchPane(
                modifier       = Modifier.padding(padding),
                state          = state,
                filteredPairs  = filteredPairs,
                vm             = vm,
                onModeChange   = vm::onModeChange
            )
        } else {
            CoinFormPane(
                modifier  = Modifier.padding(padding),
                state     = state,
                vm        = vm,
                onBack    = onBack,
                onModeChange = vm::onModeChange
            )
        }
    }
}

// ── Kraken pair search pane ───────────────────────────────────────────────────

@Composable
private fun KrakenSearchPane(
    modifier: Modifier,
    state: AddCoinUiState,
    filteredPairs: List<KrakenPairUi>,
    vm: AddCoinViewModel,
    onModeChange: (AddCoinMode) -> Unit
) {
    val accent = LocalThemeColors.current.accent
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ModeToggle(current = AddCoinMode.KRAKEN, onModeChange = onModeChange)
        Spacer(Modifier.height(12.dp))

        // Search field
        OutlinedTextField(
            value         = state.searchQuery,
            onValueChange = vm::onSearchQueryChange,
            placeholder   = { Text("Search pairs (e.g. BTC, SOL/USD…)", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null, tint = accent) },
            trailingIcon  = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { vm.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            modifier        = Modifier.fillMaxWidth(),
            singleLine      = true,
            colors          = cryptoTextFieldColors(),
            textStyle       = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
        )

        Spacer(Modifier.height(8.dp))

        when {
            state.pairsLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
            state.pairsError != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Failed to load pairs", color = ColorDown, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        CryptoOutlinedButton("Retry") { vm.retryLoadPairs() }
                    }
                }
            }
            else -> {
                Text(
                    "${filteredPairs.size} pairs${if (state.searchQuery.isBlank()) " (type to filter)" else ""}",
                    color = TextSecondary, fontSize = 10.sp
                )
                Spacer(Modifier.height(4.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(filteredPairs, key = { it.pairId }) { pair ->
                        PairRow(pair = pair, onClick = { vm.onPairSelected(pair) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PairRow(pair: KrakenPairUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Surface.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(pair.wsName, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(pair.pairId, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

// ── Coin form pane (Kraken confirmed + Custom REST) ───────────────────────────

@Composable
private fun CoinFormPane(
    modifier: Modifier,
    state: AddCoinUiState,
    vm: AddCoinViewModel,
    onBack: () -> Unit,
    onModeChange: (AddCoinMode) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModeToggle(current = state.mode, onModeChange = onModeChange)

            // ── Kraken: selected pair chip ───────────────────────────────────
            if (state.mode == AddCoinMode.KRAKEN && state.selectedPair != null) {
                CryptoCard {
                    SectionLabel("SELECTED PAIR")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                state.selectedPair.wsName,
                                color = LocalThemeColors.current.accent,
                                fontSize = 18.sp, fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Pair ID: ${state.selectedPair.pairId}",
                                color = TextSecondary, fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(onClick = vm::onPairCleared) {
                            Icon(Icons.Default.Close, contentDescription = "Clear",
                                tint = ColorDown)
                        }
                    }
                }
            }

            // ── Name / symbol ────────────────────────────────────────────────
            CryptoCard {
                SectionLabel("COIN IDENTITY")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = state.coinSymbol,
                    onValueChange = vm::onSymbolChange,
                    label         = { Text("Symbol", color = TextSecondary, fontSize = 12.sp) },
                    placeholder   = { Text("e.g. LINK", color = TextSecondary) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    colors        = cryptoTextFieldColors(),
                    textStyle     = LocalTextStyle.current.copy(
                        color = LocalThemeColors.current.accent, fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = state.coinDisplayName,
                    onValueChange = vm::onDisplayNameChange,
                    label         = { Text("Display name", color = TextSecondary, fontSize = 12.sp) },
                    placeholder   = { Text("e.g. Chainlink / USD", color = TextSecondary) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    colors        = cryptoTextFieldColors(),
                    textStyle     = LocalTextStyle.current.copy(color = TextPrimary)
                )
            }

            // ── Custom REST price source ─────────────────────────────────────
            if (state.mode == AddCoinMode.CUSTOM) {
                CryptoCard {
                    SectionLabel("PRICE SOURCE  (required)")
                    Spacer(Modifier.height(8.dp))
                    UrlPathPair(
                        urlValue        = state.priceUrl,
                        urlLabel        = "Price URL",
                        urlPlaceholder  = "https://api.example.com/price",
                        pathValue       = state.priceJsonPath,
                        pathLabel       = "JSON path",
                        pathPlaceholder = "data.price",
                        onUrlChange     = vm::onPriceUrlChange,
                        onPathChange    = vm::onPricePathChange
                    )
                }

                CryptoCard {
                    SectionLabel("24H CHANGE  (optional)")
                    Spacer(Modifier.height(8.dp))
                    UrlPathPair(
                        urlValue        = state.changeUrl,
                        urlLabel        = "Change URL",
                        urlPlaceholder  = "https://… (leave blank to skip)",
                        pathValue       = state.changeJsonPath,
                        pathLabel       = "JSON path",
                        pathPlaceholder = "data.change_pct",
                        onUrlChange     = vm::onChangeUrlChange,
                        onPathChange    = vm::onChangePathChange
                    )
                }

                CryptoCard {
                    SectionLabel("SPARKLINE  (optional)")
                    Spacer(Modifier.height(8.dp))
                    UrlPathPair(
                        urlValue        = state.sparklineUrl,
                        urlLabel        = "Sparkline URL",
                        urlPlaceholder  = "https://… returns JSON array of prices",
                        pathValue       = state.sparklineJsonPath,
                        pathLabel       = "JSON path to array",
                        pathPlaceholder = "prices",
                        onUrlChange     = vm::onSparklineUrlChange,
                        onPathChange    = vm::onSparklinePathChange
                    )
                }
            }

            // ── Optional wallet section ──────────────────────────────────────
            CryptoCard {
                Row(
                    Modifier.fillMaxWidth().clickable { vm.toggleWalletSection() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionLabel("WALLET BALANCE  (optional)")
                    Text(
                        if (state.showWalletSection) "▲" else "▼",
                        color = LocalThemeColors.current.accent, fontSize = 11.sp
                    )
                }
                AnimatedVisibility(visible = state.showWalletSection) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Use {address} as a placeholder in the URL.",
                            color = TextSecondary, fontSize = 10.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        UrlPathPair(
                            urlValue        = state.walletUrlTemplate,
                            urlLabel        = "Balance URL",
                            urlPlaceholder  = "https://…/{address}/balance",
                            pathValue       = state.walletJsonPath,
                            pathLabel       = "JSON path",
                            pathPlaceholder = "data.balance",
                            onUrlChange     = vm::onWalletUrlChange,
                            onPathChange    = vm::onWalletPathChange
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value         = state.walletDivisor,
                            onValueChange = vm::onWalletDivisorChange,
                            label         = { Text("Divisor (smallest unit → coin)", color = TextSecondary, fontSize = 11.sp) },
                            placeholder   = { Text("1.0  (use 1e18 for wei → ETH)", color = TextSecondary) },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            colors        = cryptoTextFieldColors(),
                            textStyle     = LocalTextStyle.current.copy(
                                color = TextPrimary, fontFamily = FontFamily.Monospace
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                }
            }
        }

        // Sticky save button
        Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            SaveButton(
                label   = if (state.isSaving) "Saving…" else "Add Coin",
                onClick = { vm.save(onSuccess = onBack) }
            )
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun ModeToggle(current: AddCoinMode, onModeChange: (AddCoinMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(10.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AddCoinMode.entries.forEach { mode ->
            val selected = mode == current
            val label    = if (mode == AddCoinMode.KRAKEN) "Kraken Pair" else "Custom REST"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selected) LocalThemeColors.current.accent else Surface,
                        RoundedCornerShape(7.dp)
                    )
                    .clickable { onModeChange(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color      = if (selected) BgDark else TextSecondary,
                    fontSize   = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun UrlPathPair(
    urlValue: String, urlLabel: String, urlPlaceholder: String,
    pathValue: String, pathLabel: String, pathPlaceholder: String,
    onUrlChange: (String) -> Unit, onPathChange: (String) -> Unit
) {
    OutlinedTextField(
        value         = urlValue,
        onValueChange = onUrlChange,
        label         = { Text(urlLabel, color = TextSecondary, fontSize = 12.sp) },
        placeholder   = { Text(urlPlaceholder, color = TextSecondary, fontSize = 11.sp) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        colors        = cryptoTextFieldColors(),
        textStyle     = LocalTextStyle.current.copy(color = TextPrimary, fontSize = 12.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )
    Spacer(Modifier.height(6.dp))
    OutlinedTextField(
        value         = pathValue,
        onValueChange = onPathChange,
        label         = { Text(pathLabel, color = TextSecondary, fontSize = 12.sp) },
        placeholder   = { Text(pathPlaceholder, color = TextSecondary, fontSize = 11.sp) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        colors        = cryptoTextFieldColors(),
        textStyle     = LocalTextStyle.current.copy(
            color = LocalThemeColors.current.accent, fontFamily = FontFamily.Monospace, fontSize = 12.sp
        )
    )
}
