package com.leeam.cryptowidget.data.local

enum class AlertMode {
    /** Fires once when price crosses threshold, then re-arms automatically. Default. */
    CROSSING,

    /** Fires repeatedly on a cooldown while condition holds. */
    REPEATING,

    /** Fires once, then self-disables. User must re-enable manually. */
    ONE_SHOT
}
