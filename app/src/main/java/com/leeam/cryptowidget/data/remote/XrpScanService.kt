package com.leeam.cryptowidget.data.remote

import com.leeam.cryptowidget.data.model.XrpScanAccountResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface XrpScanService {

    @GET("api/v1/account/{address}")
    suspend fun getAccount(
        @Path("address") address: String
    ): XrpScanAccountResponse
}
