package com.example.scamdetectorapp.presentation.model

import androidx.compose.ui.graphics.Color

/**
 * 詐騙知識卡資料模型
 */
data class KnowledgeCard(
    val id: Int,
    val category: String,
    val level: String, // 例如：高風險
    val title: String,
    val content: String,
    val detectionMethod: String,
    val tags: List<String> = emptyList(),
    var isCollected: Boolean = false,
    var isLearned: Boolean = false,
    val quiz: Quiz? = null
)

/**
 * 小測驗資料模型
 */
data class Quiz(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

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
