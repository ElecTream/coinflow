package com.leeam.cryptowidget.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// JSON-RPC request to XRPL: POST https://xrplcluster.com/
@Serializable
data class XrplRequest(
    val method: String = "account_info",
    val params: List<XrplAccountParam>
)

@Serializable
data class XrplAccountParam(
    val account: String,
    @SerialName("ledger_index") val ledgerIndex: String = "validated"
)

// JSON-RPC response from XRPL
@Serializable
data class XrplResponse(
    val result: XrplResult = XrplResult()
)

@Serializable
data class XrplResult(
    @SerialName("account_data") val accountData: XrplAccountData? = null,
    val status: String = "",
    val error: String? = null,
    @SerialName("error_message") val errorMessage: String? = null
)

@Serializable
data class XrplAccountData(
    @SerialName("Account") val account: String = "",
    @SerialName("Balance") val balance: String = "0" // in drops (1 XRP = 1,000,000 drops)
)
