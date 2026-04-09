package com.leeam.cryptowidget.ui.nav

object Screen {
    const val HOME           = "home"
    const val PORTFOLIO      = "portfolio"
    const val APPEARANCE     = "appearance"
    const val WIDGET_SETTINGS = "widget_settings"
    const val ADD_COIN        = "add_coin"

    // Parameterized
    const val COIN_DETAIL     = "coin_detail/{coinId}"
    const val ARG_COIN_ID     = "coinId"

    fun coinDetail(coinId: String) = "coin_detail/$coinId"
}
