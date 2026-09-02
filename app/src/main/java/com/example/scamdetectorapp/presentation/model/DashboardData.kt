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
    val typeDistribution: List<ScamTypeRatio>,
    val phoneTypeDistribution: List<ScamTypeRatio>,
    val trendData: RiskTrendData
)

/**
 * 詐騙類型分佈比例
 */
data class ScamTypeRatio(
    val label: String,
    val percentage: Int,
    val color: Color
)

/**
 * 風險趨勢資料
 */
data class RiskTrendData(
    val lowRisk: List<Float>,
    val mediumRisk: List<Float>,
    val highRisk: List<Float>,
    val labels: List<String>
)
