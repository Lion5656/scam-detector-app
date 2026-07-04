package com.example.scamdetectorapp.data.remote

import com.example.scamdetectorapp.data.model.OneSixFiveResponse
import retrofit2.http.GET

interface OneSixFiveApi {
    /**
     * 獲取 165 反詐騙闢謠專區資料
     * 來源：內政部資料開放平臺
     */
    @GET("api/v1/rest/datastore/A01010000C-000962-156")
    suspend fun getRumors(): OneSixFiveResponse
}
