# Coinflow

A polished Android home-screen widget and companion app for live multi-coin crypto price tracking.

Package: `com.electream.cryptowidget` (debug: `com.electream.cryptowidget.debug`). Published by Electream.

## Features

- **Live prices** for any number of coins, pulled from Kraken's public market API (no key required)
- **Sparkline charts** showing 24-hour price history with green/red glow segments
- **Multi-coin widget** — pick up to 5 coins, swipe between them on a single 4×2 widget
- **Wallet balance tracking** — XRPL, Bitcoin, Ethereum, Solana, plus generic REST endpoints for custom coins
- **Multi-coin portfolio screen** — total value, 24h delta, per-coin allocation breakdown
- **Configurable refresh intervals** — 15m, 30m, 1h, 2h, 6h
- **Price alerts** — ABOVE/BELOW thresholds in crossing, repeating, or one-shot modes
- **Themeable** — Cyber, Amber, Matrix, Midnight, or fully custom accent + secondary
- **BYOC (Bring Your Own Coin)** — add any Kraken pair, or wire up a custom REST endpoint with dot-notation JSON paths

## Architecture

```
CoinflowApplication (Hilt)
  └── WorkScheduler
        └── PriceUpdateWorker (WorkManager, periodic)
              ├── CryptoRepository
              │     ├── KrakenService   (price + OHLC sparkline, any pair)
              │     ├── XrplService     (XRPL wallet balance)
              │     ├── BitcoinService  (Blockstream.info balance)
              │     ├── EthereumService (eth.llamarpc.com JSON-RPC balance)
              │     ├── SolanaService   (mainnet-beta JSON-RPC balance)
              │     └── GenericRestService (custom URL + JSON path for BYOC)
              ├── CoinRepository (built-in + Room-stored custom coins)
              ├── WidgetUpdater → RemoteViews → CoinflowWidgetProvider
              ├── AlertRepository → checkAndFireAlerts
              └── AlertNotifier → NotificationManager
```

## Built-in coins

| Symbol | Name     | Kraken pair | Wallet support      |
|--------|----------|-------------|---------------------|
| BTC    | Bitcoin  | XBTUSD      | Blockstream.info    |
| ETH    | Ethereum | ETHUSD      | eth.llamarpc.com    |
| SOL    | Solana   | SOLUSD      | mainnet-beta JSON-RPC |
| XRP    | XRP      | XRPUSD      | xrplcluster.com     |

Add more via the in-app **Add Coin** flow — either pick from the full Kraken pair list or supply a custom REST endpoint.

## Build

```bash
# Debug build
./gradlew assembleDebug

# Release build (minified + shrunk)
./gradlew assembleRelease
```

Requires:
- JDK 17+
- Android SDK with API 35 build tools
- Android device/emulator with API 26+ (Android 8.0)

## Install

```bash
./gradlew installDebug
```

## Setup

1. Open the **Coinflow** app and tap the coins you want to follow.
2. Pick which followed coins appear as widget tabs (max 5).
3. Add the widget to your home screen (long-press → Widgets → Coinflow).
4. Optional: tap a coin on the home tracker to set a wallet address or configure price alerts.

## Min SDK

API 26 (Android 8.0 Oreo).
