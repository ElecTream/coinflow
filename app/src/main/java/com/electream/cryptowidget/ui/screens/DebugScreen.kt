package com.electream.cryptowidget.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.electream.cryptowidget.BuildConfig
import com.electream.cryptowidget.data.local.DebugEntry
import com.electream.cryptowidget.data.local.DebugLevel
import com.electream.cryptowidget.ui.settings.SettingsViewModel
import com.electream.cryptowidget.ui.theme.*
import com.electream.cryptowidget.widget.CoinflowWidgetProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val logEntries by vm.debugEntries.collectAsStateWithLifecycle()
    val themeColors = LocalThemeColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val widgetIds = remember(state) {
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, CoinflowWidgetProvider::class.java))
            .toList()
    }

    var fullDump by remember { mutableStateOf("Building dump…") }
    LaunchedEffect(state, logEntries, widgetIds) {
        fullDump = buildFullDump(
            vmDump        = vm.diagnosticsDump(),
            entries       = logEntries,
            widgetIds     = widgetIds,
            packageName   = context.packageName
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Debug · Internal", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.BUILD_TYPE}",
                            color = themeColors.accent,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = themeColors.accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Action toolbar ──────────────────────────────────────────────
            ActionToolbar(
                onForceRefresh = {
                    vm.refreshAllFollowed()
                    scope.launch { snackbar.showSnackbar("Triggered refresh") }
                },
                onCopy = {
                    val cm = context.getSystemService(ClipboardManager::class.java)
                    cm?.setPrimaryClip(ClipData.newPlainText("Coinflow diagnostics", fullDump))
                    scope.launch { snackbar.showSnackbar("Copied to clipboard") }
                },
                onShare = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, fullDump)
                        putExtra(Intent.EXTRA_SUBJECT, "Coinflow diagnostics")
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Share diagnostics")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                onClearErrors = {
                    vm.clearDebugLog()
                    scope.launch { snackbar.showSnackbar("Error log cleared") }
                },
                onResetCache = {
                    vm.resetAllCoinCache()
                    scope.launch { snackbar.showSnackbar("Cache reset · refresh triggered") }
                }
            )

            // ── Section: Identity ───────────────────────────────────────────
            DebugCard(title = "APP IDENTITY") {
                KvRow("package", context.packageName)
                KvRow("version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                KvRow("build type", BuildConfig.BUILD_TYPE)
                KvRow("application id", BuildConfig.APPLICATION_ID)
            }

            // ── Section: Widget installation ────────────────────────────────
            DebugCard(title = "WIDGET INSTANCES") {
                KvRow("count", widgetIds.size.toString())
                if (widgetIds.isEmpty()) {
                    Text(
                        "No widget instances on home screens. Add one via long-press → Widgets → Coinflow.",
                        color = ColorDown,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    KvRow("ids", widgetIds.joinToString(", "))
                }
            }

            // ── Section: System state ───────────────────────────────────────
            DebugCard(title = "SYSTEM STATE") {
                KvRow("active widget coin", state.coinId)
                KvRow(
                    "active resolved",
                    (state.customCoins.firstOrNull { it.id == state.coinId }?.symbol
                        ?: state.followedSnapshots.firstOrNull { it.coin.id == state.coinId }?.coin?.symbol
                        ?: com.electream.cryptowidget.data.model.CoinRegistry.byId(state.coinId).symbol)
                )
                KvRow("followed (${state.followedCoinIds.size})", state.followedCoinIds.joinToString(", "))
                KvRow("widget tabs (${state.widgetCoinIds.size}/5)", state.widgetCoinIds.joinToString(", "))
                KvRow("refresh interval", "${state.refreshIntervalMin} min")
                KvRow("theme", state.appTheme.name)
                KvRow("chart style", state.chartStyle.name)
                KvRow("sparkline enabled", state.showSparkline.toString())
            }

            // ── Section: Worker telemetry ───────────────────────────────────
            DebugCard(title = "WORKER STATUS") {
                KvRow("last run", formatAbsAndRel(state.lastWorkerRunMs))
                KvRow(
                    label = "last error",
                    value = state.lastErrorMsg ?: "(none)",
                    highlight = if (state.lastErrorMsg != null) ColorDown else null
                )
                KvRow(
                    "refreshing now",
                    state.isRefreshingAll.toString(),
                    highlight = if (state.isRefreshingAll) LocalThemeColors.current.accent else null
                )
            }

            // ── Section: Per-coin cache ─────────────────────────────────────
            DebugCard(title = "PER-COIN CACHE  (${state.followedSnapshots.size})") {
                if (state.followedSnapshots.isEmpty()) {
                    Text("No followed coins.", color = TextSecondary, fontSize = 11.sp)
                } else {
                    state.followedSnapshots.forEachIndexed { idx, snap ->
                        if (idx > 0) HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                        Text(
                            "${snap.coin.symbol}  ·  ${snap.coin.id}",
                            color = LocalThemeColors.current.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        KvRow("name", snap.coin.displayName, compact = true)
                        KvRow(
                            "priceUsd",
                            snap.priceUsd.toString(),
                            compact = true,
                            highlight = if (snap.priceUsd <= 0.0) ColorDown else null
                        )
                        KvRow("change24h%", String.format(Locale.US, "%.4f", snap.change24hPct), compact = true)
                        KvRow("sparkline pts", snap.sparkline.size.toString(), compact = true)
                        KvRow("updated", formatAbsAndRel(snap.updatedMs), compact = true)
                        KvRow("priceSource", snap.coin.priceSource.toString(), compact = true)
                        KvRow("walletConfig", snap.coin.walletConfig::class.simpleName ?: "?", compact = true)
                    }
                }
            }

            // ── Section: Custom coin entities ───────────────────────────────
            DebugCard(title = "CUSTOM COINS  (${state.customCoins.size})") {
                if (state.customCoins.isEmpty()) {
                    Text("No custom (BYOC) coins added.", color = TextSecondary, fontSize = 11.sp)
                } else {
                    state.customCoins.forEachIndexed { idx, c ->
                        if (idx > 0) HorizontalDivider(color = CardBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))
                        Text(
                            "${c.symbol}  ·  ${c.id}",
                            color = LocalThemeColors.current.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        KvRow("displayName", c.displayName, compact = true)
                        KvRow("priceSource", c.priceSource.toString(), compact = true)
                        KvRow("walletConfig", c.walletConfig.toString(), compact = true)
                    }
                }
            }

            // ── Section: Error log ──────────────────────────────────────────
            DebugCard(title = "ERROR LOG  (${logEntries.size} entries)") {
                if (logEntries.isEmpty()) {
                    Text(
                        "No log entries. Anything thrown by the worker, widget, or fetches will show up here.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                } else {
                    val reversed = logEntries.reversed()
                    reversed.forEachIndexed { idx, entry ->
                        if (idx > 0) HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                        LogEntryRow(entry = entry)
                    }
                }
            }

            // ── Full text dump ──────────────────────────────────────────────
            DebugCard(title = "FULL DUMP  (paste-ready)") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        fullDump,
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Cards / rows ──────────────────────────────────────────────────────────

@Composable
private fun DebugCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface.copy(alpha = 0.85f))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            title,
            color = LocalThemeColors.current.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun KvRow(
    label: String,
    value: String,
    compact: Boolean = false,
    highlight: androidx.compose.ui.graphics.Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 1.dp else 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            color = TextSecondary,
            fontSize = if (compact) 10.sp else 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(0.45f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            color = highlight ?: TextPrimary,
            fontSize = if (compact) 10.sp else 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .weight(0.55f)
                .horizontalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun LogEntryRow(entry: DebugEntry) {
    val color = when (entry.level) {
        DebugLevel.ERROR -> ColorDown
        DebugLevel.WARN  -> Color(0xFFFFB300)
        DebugLevel.INFO  -> TextSecondary
    }
    val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(entry.timeMs))
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(time, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(color.copy(alpha = 0.18f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(entry.level.name, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                entry.source,
                color = LocalThemeColors.current.accent,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            entry.message,
            color = TextPrimary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 13.sp
        )
    }
}

// ── Action toolbar ────────────────────────────────────────────────────────

@Composable
private fun ActionToolbar(
    onForceRefresh: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onClearErrors: () -> Unit,
    onResetCache: () -> Unit
) {
    val accent = LocalThemeColors.current.accent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface.copy(alpha = 0.85f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Force refresh", accent, onClick = onForceRefresh, modifier = Modifier.weight(1f))
            ActionButton("Copy", accent, onClick = onCopy, modifier = Modifier.weight(1f))
            ActionButton("Share", accent, onClick = onShare, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Clear errors", ColorDown, onClick = onClearErrors, modifier = Modifier.weight(1f))
            ActionButton("Reset cache", ColorDown, onClick = onResetCache, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

@Suppress("FunctionName")
private fun Color(argb: Long): androidx.compose.ui.graphics.Color =
    androidx.compose.ui.graphics.Color(argb.toInt())

private fun formatAbsAndRel(ms: Long): String {
    if (ms <= 0L) return "(never)"
    val abs = SimpleDateFormat("HH:mm:ss MMM d", Locale.US).format(Date(ms))
    val deltaSec = (System.currentTimeMillis() - ms) / 1000
    val rel = when {
        deltaSec < 0       -> "in future?"
        deltaSec < 60      -> "${deltaSec}s ago"
        deltaSec < 3600    -> "${deltaSec / 60}m ago"
        deltaSec < 86_400  -> "${deltaSec / 3600}h ago"
        else               -> "${deltaSec / 86_400}d ago"
    }
    return "$abs  ($rel)"
}

private fun buildFullDump(
    vmDump: String,
    entries: List<DebugEntry>,
    widgetIds: List<Int>,
    packageName: String
): String {
    val sb = StringBuilder()
    sb.appendLine("======================================================")
    sb.appendLine("Coinflow full debug dump")
    sb.appendLine("======================================================")
    sb.appendLine("package: $packageName")
    sb.appendLine("version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) build=${BuildConfig.BUILD_TYPE}")
    sb.appendLine("widget instances: ${widgetIds.size} ids=${widgetIds}")
    sb.appendLine()
    sb.appendLine(vmDump)
    sb.appendLine("------------------------------------------------------")
    sb.appendLine("ERROR LOG  (${entries.size} entries, newest last)")
    sb.appendLine("------------------------------------------------------")
    if (entries.isEmpty()) sb.appendLine("(empty)")
    val fmt = SimpleDateFormat("HH:mm:ss yyyy-MM-dd", Locale.US)
    entries.forEach { e ->
        sb.appendLine("${fmt.format(Date(e.timeMs))}  [${e.level}]  ${e.source}  ::  ${e.message}")
    }
    return sb.toString()
}
