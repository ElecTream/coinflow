# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK (minified + shrunk)
./gradlew installDebug           # Build and install to connected device
./gradlew clean build            # Full clean build
```

No test files exist yet — `./gradlew test` will run if tests are added.

## Architecture Overview

**Coinflow** is a single-module Android app (`com.electream.cryptowidget`) that tracks multiple cryptocurrencies and displays them as a home screen widget plus a companion Compose UI.

**Tech stack:** Hilt DI, WorkManager, Jetpack Compose, Room, DataStore, Retrofit, Kotlin Coroutines, Kotlin Serialization.

### Data Flow

1. `CoinflowApplication` initializes the notification channel and schedules a periodic `PriceUpdateWorker` via `WorkScheduler` (default 15 min, network-constrained).
2. `PriceUpdateWorker` iterates every followed coin and calls `CryptoRepositoryImpl`, which dispatches per-coin:
   - Price + 24h change + hourly OHLC sparkline → **Kraken** (any pair) or a user-defined **GenericRest** endpoint
   - Wallet balance → XRPL / Bitcoin (Blockstream) / Ethereum (llamarpc) / Solana (mainnet-beta) / GenericRest, depending on the coin's `WalletConfig`
3. Each coin's results are cached per-id in **DataStore** (`WidgetPreferences`), and any enabled price alerts are checked against **Room** (`AlertDatabase`).
4. `WidgetUpdater` builds a `RemoteViews` object with the active coin's data; `SparklineRenderer` draws the 24h chart to a `Canvas` bitmap.
5. `CoinflowWidgetProvider` (AppWidgetProvider) applies the `RemoteViews` to all active widget instances. The widget exposes a top tab strip for swapping between the user's chosen coins (up to 5).

### Key Layers

| Layer | Location |
|---|---|
| Widget UI (RemoteViews) | `widget/CoinflowWidgetProvider`, `widget/WidgetUpdater`, `widget/SparklineRenderer` |
| App UI (Compose) | `ui/settings/SettingsActivity` (NavHost), `ui/screens/*`, `ui/components/*`, `ui/chart/*` |
| Background work | `worker/PriceUpdateWorker`, `worker/WorkScheduler` |
| Repositories | `data/repository/CryptoRepositoryImpl`, `data/repository/CoinRepositoryImpl` |
| Remote APIs | `data/remote/KrakenService`, `XrplService`, `BitcoinService`, `EthereumService`, `SolanaService`, `GenericRestService` |
| Local storage | `data/local/WidgetPreferences` (DataStore, per-coin keys), `data/local/AlertDatabase` (Room), `data/local/CustomCoinDao` (BYOC) |
| DI | `di/NetworkModule` (one Retrofit per host), `di/DatabaseModule`, `di/RepositoryModule` |

### Error Handling Pattern

Each data source in `CryptoRepositoryImpl` fails independently — a sparkline fetch failure does not block the price display. `PriceUpdateWorker` persists the last error message to DataStore for display in the Settings diagnostics section.

### No default coin

Coinflow has **no implicit default coin**. On a fresh install the widget renders a "Tap to choose coins" CTA (`res/layout/widget_empty.xml`) that opens `SettingsActivity`. The worker is idle while `WidgetPreferences.followedCoinIds` is empty — no background fetches, no battery cost.

Upgrades from v1.1.7 (single-coin schema) are handled by `WidgetPreferencesBootstrap` on `Application.onCreate`: the legacy `cached_*` keys and `wallet_address` are copied into the per-coin schema for whatever coin was active at upgrade time, and `widget_coin_ids` / `followed_coin_ids` are seeded with that coin. Bump `WidgetPreferencesBootstrap.TARGET_VERSION` when the migration shape itself changes.

### Adding a built-in coin

1. Add one `CoinDefinition` to `CoinRegistry.all` in `data/model/CoinDefinition.kt`.
   - `id` must be lowercase kebab/snake-case (regex `^[a-z0-9_-]+$`) and STABLE forever — DataStore keys derive from it.
   - `priceSource` → `PriceSource.Kraken("PAIRUSD")` for Kraken-listed coins; else `PriceSource.GenericRest(...)`.
   - `walletConfig` → `WalletConfig.None` if balance tracking is unsupported.
2. Build and install. No other file needs changing.

Validation runs at app startup: duplicate ids and malformed id strings throw `IllegalArgumentException` so the app fails fast rather than silently corrupting DataStore.

## APIs

- **Kraken** (`https://api.kraken.com/0/public`) — price + OHLC for any pair, no API key required, no geo-restrictions
- **XRPL** (`https://xrplcluster.com`) — direct ledger query for XRP wallets, no API key required
- **Blockstream.info** — BTC wallet balance
- **eth.llamarpc.com** — Ethereum JSON-RPC for ETH wallet balance
- **api.mainnet-beta.solana.com** — Solana JSON-RPC for SOL wallet balance
- **GenericRestService** — user-supplied URL + dot-notation JSON path for BYOC coins

## Configuration Notes

- **minSdk 26**, **compileSdk/targetSdk 35**, **Kotlin 2.0.21**, **JDK 17**
- Debug app ID is `com.electream.cryptowidget.debug` (separate from release for side-by-side install)
- ProGuard rules in `app/proguard-rules.pro` preserve Kotlin Serialization, Retrofit, OkHttp, Room, Hilt, WorkManager, and all `data.model` classes
- Widget minimum size: 180×110dp; supports resizing via `crypto_widget_info.xml`
