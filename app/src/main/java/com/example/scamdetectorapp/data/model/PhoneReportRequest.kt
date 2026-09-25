package com.example.scamdetectorapp.data.model

data class PhoneReportRequest (
    val phoneNumber: String,
    val phoneType: String,
    val otherType: String? = null,
    val reporterPhone: String? = null,
    val transferType: Int = 0,
    val transferContent: String? = null
)