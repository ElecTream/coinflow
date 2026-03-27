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

**Coinflow** is a single-module Android app (`com.leeam.cryptowidget`) that tracks XRP price and displays it as a home screen widget.

**Tech stack:** Hilt DI, WorkManager, Jetpack Compose (settings UI), Room, DataStore, Retrofit, Kotlin Coroutines, Kotlin Serialization.

### Data Flow

1. `CryptoWidgetApplication` initializes the notification channel and schedules a periodic `PriceUpdateWorker` via `WorkScheduler` (default 15 min, network-constrained).
2. `PriceUpdateWorker` calls `CryptoRepositoryImpl`, which hits two APIs in parallel:
   - **Kraken** — live XRP price + 24h change + hourly sparkline data
   - **XRPL** (direct ledger) — wallet XRP balance (if address configured)
3. Results are cached in **DataStore** (`WidgetPreferences`), and any enabled price alerts are checked against **Room** (`AlertDatabase`).
4. `WidgetUpdater` builds a `RemoteViews` object with the new data; `SparklineRenderer` draws the 24h chart to a `Canvas` bitmap.
5. `CryptoWidgetProvider` (AppWidgetProvider) applies the `RemoteViews` to all active widget instances.

### Key Layers

| Layer | Location |
|---|---|
| Widget UI (RemoteViews) | `widget/CryptoWidgetProvider`, `widget/WidgetUpdater`, `widget/SparklineRenderer` |
| Settings UI (Compose) | `ui/settings/SettingsActivity`, `ui/settings/SettingsViewModel` |
| Background work | `worker/PriceUpdateWorker`, `worker/WorkScheduler` |
| Repository | `data/repository/CryptoRepositoryImpl` |
| Remote APIs | `data/remote/KrakenService`, `data/remote/XrplService` |
| Local storage | `data/local/WidgetPreferences` (DataStore), `data/local/AlertDatabase` (Room) |
| DI | `di/NetworkModule` (two Retrofit instances), `di/DatabaseModule` |

### Error Handling Pattern

Each data source in `CryptoRepositoryImpl` fails independently — a sparkline fetch failure does not block the price display. `PriceUpdateWorker` persists the last error message to DataStore for display in the Settings diagnostics section.

## APIs

- **Kraken** (`https://api.kraken.com/0/public`) — no API key required, US-based, no geo-restrictions
- **XRPL** (`https://xrplcluster.com`) — direct ledger query, no API key required

## Configuration Notes

- **minSdk 26**, **compileSdk/targetSdk 35**, **Kotlin 2.0.21**, **JDK 17**
- Debug app ID is `com.leeam.cryptowidget.debug` (separate from release for side-by-side install)
- ProGuard rules in `app/proguard-rules.pro` preserve Kotlin Serialization, Retrofit, OkHttp, Room, Hilt, WorkManager, and all `data.model` classes
- Widget minimum size: 180×110dp; supports resizing via `crypto_widget_info.xml`
