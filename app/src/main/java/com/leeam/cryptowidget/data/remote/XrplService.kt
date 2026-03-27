package com.leeam.cryptowidget.data.remote

import com.leeam.cryptowidget.data.model.XrplRequest
import com.leeam.cryptowidget.data.model.XrplResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface XrplService {

    @POST("/")
    suspend fun accountInfo(
        @Body request: XrplRequest
    ): XrplResponse
}
