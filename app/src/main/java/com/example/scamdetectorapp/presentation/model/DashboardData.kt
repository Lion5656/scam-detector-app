package com.example.scamdetectorapp.presentation.model

import androidx.compose.ui.graphics.Color

/**
 * 儀表板統計資料模型
 */
data class DashboardStats(
    val highRiskMessages: Int,
    val interceptedCount: Int,
    val learningProgress: Int, // 百分比
    val reportedCases: Int,
    val typeDistribution: List<ScamTypeRatio>
)

/**
 * 詐騙類型分佈比例
 */
data class ScamTypeRatio(
    val label: String,
    val percentage: Int,
    val color: Color
)
