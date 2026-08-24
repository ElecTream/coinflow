package com.electream.cryptowidget.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path

interface BitcoinService {
    /**
     * Blockstream public API — no key required.
     * Returns confirmed + unconfirmed on-chain stats for a Bitcoin address.
     */
    @GET("api/address/{address}")
    suspend fun getAddressInfo(@Path("address") address: String): BlockstreamAddressResponse
}

@Serializable
data class BlockstreamAddressResponse(
    @SerialName("chain_stats") val chainStats: BlockstreamStats = BlockstreamStats(),
    @SerialName("mempool_stats") val mempoolStats: BlockstreamStats = BlockstreamStats()
)

@Serializable
data class BlockstreamStats(
    /** Total satoshis ever received to this address (confirmed). */
    @SerialName("funded_txo_sum") val fundedTxoSum: Long = 0L,
    /** Total satoshis ever spent from this address (confirmed). */
    @SerialName("spent_txo_sum") val spentTxoSum: Long = 0L
)

/** Confirmed balance in BTC. */
fun BlockstreamAddressResponse.btcBalance(): Double =
    (chainStats.fundedTxoSum - chainStats.spentTxoSum) / 100_000_000.0
