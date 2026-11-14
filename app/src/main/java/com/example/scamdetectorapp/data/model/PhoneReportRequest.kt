package com.example.scamdetectorapp.data.model

data class PhoneReportRequest (
    val phoneNumber: String,
    val phoneType: String,
    // 僅在 phoneType 為「其他」時帶入自訂類型，null 時 Gson 不會序列化此欄位
    val otherType: String? = null
)