package com.example.scamdetectorapp.data.model

data class TextCheckResult (
    val label: String? = null,
    val score: Float? = null,
    val reason: String? = null,
    val clsModelConfidence: Float? = null
)