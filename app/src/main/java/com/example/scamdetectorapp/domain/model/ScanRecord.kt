package com.example.scamdetectorapp.domain.model

import com.example.scamdetectorapp.presentation.model.ScanUiModel

/**
 * 掃描紀錄資料模型
 */
data class ScanRecord(
    val id: String,
    val type: DetectionMode,
    val input: String,
    val result: ScanUiModel,
    val timestamp: Long = System.currentTimeMillis()
)
