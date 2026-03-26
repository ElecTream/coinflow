# Crypto Widget

A polished Android home-screen widget for live XRP price tracking.

## Features

- **Live XRP price** from CoinGecko (free, no API key required)
- **Sparkline chart** showing 24-hour price history with green/red glow effect
- **Wallet balance tracking** — enter any public XRP wallet address to see your holdings
- **Configurable refresh intervals** — 15m, 30m, 1h, 2h, or 6h
- **Price alerts** — set ABOVE/BELOW thresholds; get notified when XRP crosses them
- **Dark crypto theme** — electric cyan and purple on near-black
- Supports **widget resize** — sparkline re-renders at the new dimensions

## Architecture

```
CryptoWidgetApplication (Hilt)
  └── WorkScheduler
        └── PriceUpdateWorker (WorkManager, periodic)
              ├── CryptoRepository
              │     ├── CoinGeckoService (price + sparkline)
              │     └── XrpScanService (wallet balance)
              ├── WidgetUpdater → RemoteViews → CryptoWidgetProvider
              ├── AlertRepository → checkAndFireAlerts
              └── AlertNotifier → NotificationManager
```

## APIs used

| Service | Endpoint | Notes |
|---------|----------|-------|
| CoinGecko | `api/v3/simple/price` | Live price + 24h change |
| CoinGecko | `api/v3/coins/ripple/market_chart` | Sparkline data (hourly) |
| XRPScan | `api/v1/account/{address}` | Wallet XRP balance |

Both APIs are free with no API key required.

## Build

```bash
# Debug build
./gradlew assembleDebug

# Release build (signed with debug keystore)
./gradlew assembleRelease
```

Requires:
- JDK 17+
- Android SDK with API 35 build tools
- Android device/emulator with API 26+ (Android 8.0)

## Install

```bash
# Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Install release APK
adb install -r app/build/outputs/apk/release/app-release.apk

# Or one-step
./gradlew installDebug
```

## Setup

1. Add the widget to your home screen (long-press → Widgets → Crypto Widget)
2. Open the **Crypto Widget** app to configure:
   - Enter a public XRP wallet address (optional)
   - Set your preferred refresh interval
   - Add price alerts for above/below thresholds
   - Tap **Save & Update Widget**

## Min SDK

API 26 (Android 8.0 Oreo) — required for adaptive icons and modern WorkManager behavior.
