package com.leeam.cryptowidget.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import java.math.BigInteger

interface EthereumService {
    /**
     * ethereum-rpc.publicnode.com — free Ethereum JSON-RPC, no key required, generous rate limits.
     * Calls eth_getBalance for a given address.
     */
    @POST(".")
    suspend fun getBalance(@Body request: EthRpcRequest): EthRpcResponse
}

@Serializable
data class EthRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String = "eth_getBalance",
    /** params[0] = address, params[1] = block tag ("latest") */
    val params: List<String>,
    val id: Int = 1
)

@Serializable
data class EthRpcResponse(
    /** Hex-encoded wei value, e.g. "0x2386f26fc10000". Null on error. */
    val result: String? = null,
    val error: EthRpcError? = null
)

@Serializable
data class EthRpcError(val code: Int = 0, val message: String = "")

/**
 * Converts the hex wei result to ETH as a Double.
 * Uses BigInteger to handle values that exceed Long range (> ~9.2 ETH).
 */
fun EthRpcResponse.ethBalance(): Double {
    val hex = result?.removePrefix("0x") ?: return 0.0
    if (hex.isBlank()) return 0.0
    return BigInteger(hex, 16).toDouble() / 1_000_000_000_000_000_000.0
}
