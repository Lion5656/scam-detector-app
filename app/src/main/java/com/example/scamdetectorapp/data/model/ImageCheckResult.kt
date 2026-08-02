package com.example.scamdetectorapp.data.model

data class ImageCheckResult (
    val productName: String? = null,
    val condition: String? = null,
    val listedPrice: Int? = null,
    val marketPrice: Int? = null,
    val sellerName: String? = null,
    val riskLabel: String? = null,
    val riskScore: String? = null,
    val result: String? = null,
)