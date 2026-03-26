package com.leeam.cryptowidget.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Response from: GET https://api.xrpscan.com/api/v1/account/{address}
@Serializable
data class XrpScanAccountResponse(
    val account: String = "",
    @SerialName("xrpBalance") val xrpBalance: String = "0",
    val activated: Boolean = false,
    @SerialName("accountName") val accountName: XrpAccountName? = null,
    val error: String? = null
)

@Serializable
data class XrpAccountName(
    val name: String? = null,
    val desc: String? = null
)
