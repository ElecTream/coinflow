package com.electream.cryptowidget.data.local

import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot migration + bootstrap. Runs once per process and once per persisted schema
 * version. Two flows it handles:
 *
 * 1. **Upgrade from v1.1.7 (single-coin schema).** Copies legacy keys
 *    (`cached_*`, `wallet_address`) into the per-coin schema for whatever coin was the
 *    active coin at the time of upgrade, so the user's last-known price and wallet are
 *    preserved through the schema change. Legacy keys are kept in place for one release
 *    cycle as a passive backup; remove them in SCHEMA_VERSION 2.
 *
 * 2. **Fresh install.** Does nothing — Coinflow has no implicit default coin. The widget
 *    shows a "tap to choose coins" CTA and the worker is idle until the user picks at
 *    least one coin in Settings.
 *
 * The migration is gated by [WidgetPreferences.Keys.SCHEMA_VERSION] inside DataStore, so
 * it survives backup/restore and won't repeat after a successful run.
 */
@Singleton
class WidgetPreferencesBootstrap @Inject constructor(
    private val prefs: WidgetPreferences,
    private val debugLog: DebugLog,
) {
    @Volatile private var ran = false

    suspend fun runIfNeeded() {
        if (ran) return
        var summary: String? = null
        prefs.editPrefs { p ->
            val currentVersion = p[WidgetPreferences.Keys.SCHEMA_VERSION] ?: 0
            if (currentVersion >= TARGET_VERSION) {
                ran = true
                summary = "already at v$currentVersion"
                return@editPrefs
            }

            val legacyActive = p[WidgetPreferences.Keys.COIN_ID]?.takeIf { it.isNotBlank() }
            if (legacyActive != null) {
                // ── v1.1.7 upgrader: copy legacy single-coin cache → per-coin ────────
                p[WidgetPreferences.Keys.CACHED_PRICE_USD]?.let {
                    p[WidgetPreferences.Keys.priceUsdFor(legacyActive)] = it
                }
                p[WidgetPreferences.Keys.CACHED_CHANGE_PCT]?.let {
                    p[WidgetPreferences.Keys.changePctFor(legacyActive)] = it
                }
                p[WidgetPreferences.Keys.CACHED_BALANCE]?.let {
                    p[WidgetPreferences.Keys.balanceFor(legacyActive)] = it
                }
                p[WidgetPreferences.Keys.CACHED_UPDATED_MS]?.let {
                    p[WidgetPreferences.Keys.updatedMsFor(legacyActive)] = it
                }
                p[WidgetPreferences.Keys.CACHED_SPARKLINE]?.let {
                    p[WidgetPreferences.Keys.sparklineFor(legacyActive)] = it
                }
                p[WidgetPreferences.Keys.CACHED_SPARKLINE_TS]?.let {
                    p[WidgetPreferences.Keys.sparklineTsFor(legacyActive)] = it
                }
                p[WidgetPreferences.Keys.WALLET_ADDRESS_LEGACY]?.let {
                    p[WidgetPreferences.Keys.walletAddressFor(legacyActive)] = it
                }
                if (p[WidgetPreferences.Keys.WIDGET_COIN_IDS] == null) {
                    p[WidgetPreferences.Keys.WIDGET_COIN_IDS] = legacyActive
                }
                if (p[WidgetPreferences.Keys.FOLLOWED_COIN_IDS] == null) {
                    p[WidgetPreferences.Keys.FOLLOWED_COIN_IDS] = legacyActive
                }
                summary = "upgraded 1.1.7 schema → v$TARGET_VERSION active=$legacyActive"
            } else {
                summary = "fresh install — leaving empty for user setup (v$TARGET_VERSION)"
            }

            p[WidgetPreferences.Keys.SCHEMA_VERSION] = TARGET_VERSION
        }
        ran = true
        summary?.let { debugLog.info("Bootstrap", it) }
    }

    companion object {
        /** Bump this when [runIfNeeded] needs to perform a new migration. */
        const val TARGET_VERSION = 1
    }
}
