package com.example.scamdetectorapp.data.model

data class PhoneReportResult (
    val phoneNumber: String? = null,
    val status: String? = null,
    val totalReports: Int? = null,
    val reportTime: String? = null,
    val message: String? = null,
    val connectionReason: String? = null
) 