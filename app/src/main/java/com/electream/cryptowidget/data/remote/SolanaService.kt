package com.electream.cryptowidget.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

interface SolanaService {
    /**
     * api.mainnet-beta.solana.com — free Solana JSON-RPC, no key required.
     * Calls getBalance for a given public key.
     */
    @POST(".")
    suspend fun getBalance(@Body request: SolRpcRequest): SolRpcResponse
}

@Serializable
data class SolRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String = "getBalance",
    /** params[0] = base58 public key */
    val params: List<String>
)

@Serializable
data class SolRpcResponse(
    val result: SolRpcResult? = null,
    val error: JsonElement? = null
)

@Serializable
data class SolRpcResult(
    /** Confirmed balance in lamports (1 SOL = 1,000,000,000 lamports). */
    val value: Long = 0L,
    val context: JsonElement? = null
)

/** Converts lamports to SOL as a Double. */
fun SolRpcResponse.solBalance(): Double = (result?.value ?: 0L) / 1_000_000_000.0
