package com.example.scamdetectorapp.presentation.model

import com.example.scamdetectorapp.domain.model.DetectionMode

data class ScanUiModel(
    val isSafe: Boolean,
    val riskLevel: String,
    val score: Int,
    val title: String,
    val reasons: List<String>,
    val mode: DetectionMode,
    val detailMap: Map<String, Any>? = null
)
