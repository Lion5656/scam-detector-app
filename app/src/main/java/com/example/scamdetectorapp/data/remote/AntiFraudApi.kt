package com.example.scamdetectorapp.data.remote

import com.example.scamdetectorapp.data.model.TextRequest
import com.example.scamdetectorapp.data.model.AntiFraudResponse
import com.example.scamdetectorapp.data.model.ImageCheckResult
import com.example.scamdetectorapp.data.model.PhoneQueryRequest
import com.example.scamdetectorapp.data.model.PhoneQueryResult
import com.example.scamdetectorapp.data.model.PhoneReportRequest
import com.example.scamdetectorapp.data.model.PhoneReportResult
import com.example.scamdetectorapp.data.model.TextCheckResult
import com.example.scamdetectorapp.data.model.UrlCheckResult
import com.example.scamdetectorapp.data.model.UrlRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AntiFraudApi {
    @POST("/api/v1/phones/search")
    suspend fun queryPhoneNum(
        @Body body: PhoneQueryRequest
    ): AntiFraudResponse<PhoneQueryResult>

    @POST("/api/v1/phones/report")
    suspend fun reportPhoneNum(
        @Body body: PhoneReportRequest
    ): AntiFraudResponse<PhoneReportResult>

    @POST("/api/v1/url/analyze")
    suspend fun analyzeUrl(
        @Body body: UrlRequest
    ): AntiFraudResponse<UrlCheckResult>

    @POST("/api/v1/text/analyze")
    suspend fun analyzeText(
        @Body body: TextRequest
    ): AntiFraudResponse<TextCheckResult>

    @Multipart
    @POST("/api/v1/price/analyze")
    suspend fun analyzePrice(
        @Part image: MultipartBody.Part
    ): AntiFraudResponse<ImageCheckResult>
}
