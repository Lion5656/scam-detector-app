package com.example.scamdetectorapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * 165 政府開放資料 API 回傳結構 (闢謠專區)
 */
data class OneSixFiveResponse(
    val success: Boolean,
    val result: OneSixFiveResult
)

data class OneSixFiveResult(
    val records: List<ScamRumorRecord>
)

data class ScamRumorRecord(
    @SerializedName("闢謠標題")
    val title: String,
    @SerializedName("闢謠內容")
    val content: String,
    @SerializedName("發布日期")
    val date: String
)
