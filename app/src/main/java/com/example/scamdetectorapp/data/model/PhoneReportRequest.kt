package com.example.scamdetectorapp.data.model

data class PhoneReportRequest (
    val phoneNumber: String,
    val phoneType: String,
    val otherType: String
)