# Play listing — Coinflow

ElecTream. Package `com.electream.cryptowidget`. Version **1.2.12** (10212). Android only.

## Title (30)

Coinflow

## Short description (80)

Live crypto prices on your home screen. Wallets and alerts if you want them.

## Full description

Coinflow is a home-screen widget for live crypto prices, with a companion app for the rest.

Follow any Kraken pair. Swipe up to five coins on one widget. Optional wallet balances for BTC, ETH, SOL, and XRP, plus custom REST coins. Price alerts, a portfolio view, and a handful of themes.

No account. No ads. Published by ElecTream.

## Data safety (draft)

- Collected: on-device settings; optional wallet addresses; market requests to Kraken / public RPCs
- Shared: no ads; public APIs see IP and addresses you query
- Encrypted in transit: yes (HTTPS)
- Users can delete: clear in-app or uninstall
- Children: no
- Financial / crypto: widget + optional self-custody addresses, not a brokerage

## Before upload

- [ ] Dedicated upload keystore (stop the debug-keystore fallback) + Play App Signing
- [ ] `./gradlew bundleRelease` (Play wants an AAB, not ABI-split APKs)
- [ ] Public HTTPS URL for [PRIVACY.md](PRIVACY.md)
- [ ] Phone screenshots + 1024×500 feature graphic (`coinflowicon.png`)
- [ ] Data safety + crypto/financial declarations
- [ ] Turn off or document `allowBackup` if wallet addresses should not hit cloud backup
- [ ] Drop unused `FOREGROUND_SERVICE` or give it a type
