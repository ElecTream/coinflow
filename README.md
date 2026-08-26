# Coinflow

A home-screen crypto widget plus a companion app for live prices, wallets, and alerts.

| | |
| --- | --- |
| **Brand** | ElecTream |
| **Listing** | Coinflow |
| **Package** | `com.electream.cryptowidget` (debug: `.debug`) |
| **Version** | 1.2.12 (versionCode 10212) |
| **Platforms** | Android only |

## What it does

- Live prices from Kraken’s public API (no key), plus sparklines
- Multi-coin widget (up to 5 tabs on a 4×2)
- Optional wallet balances (BTC / ETH / SOL / XRP + custom REST)
- Portfolio screen and price alerts
- Themes: Cyber, Amber, Matrix, Midnight, or custom

No default coin on a fresh install — the widget asks you to pick.

## Run

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Release APK (still falls back to the debug keystore unless `RELEASE_*` Gradle properties are set):

```bash
./gradlew assembleRelease
```

Play needs an **AAB**, not ABI-split APKs: `./gradlew bundleRelease` once release signing is real.

## Docs

| File | What it is |
| --- | --- |
| [PRIVACY.md](PRIVACY.md) | Privacy policy (host a public copy for Play) |
| [STORE.md](STORE.md) | Play listing copy, Data safety, upload checklist |
