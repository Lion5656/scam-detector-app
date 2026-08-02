package com.example.scamdetectorapp.domain.model

data class ScanResult(
    val riskLevel: String?,
    val score: String? = null,
    val threatType: String? = null,
    val suggestion: String? = null,
    val detailInfo: Map<String, Any>? = null
)
