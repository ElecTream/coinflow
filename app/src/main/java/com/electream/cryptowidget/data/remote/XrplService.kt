package com.electream.cryptowidget.data.remote

import com.electream.cryptowidget.data.model.XrplRequest
import com.electream.cryptowidget.data.model.XrplResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface XrplService {

    @POST("/")
    suspend fun accountInfo(
        @Body request: XrplRequest
    ): XrplResponse
}
