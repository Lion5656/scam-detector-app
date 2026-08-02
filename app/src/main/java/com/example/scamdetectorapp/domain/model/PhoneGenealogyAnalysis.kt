package com.example.scamdetectorapp.domain.model

/**
 * 號碼族譜分析結果 (Domain Layer)
 */
data class PhoneGenealogyAnalysis(
    val rootNumber: String,
    val nodes: List<GenealogyAnalysisNode>
)

data class GenealogyAnalysisNode(
    val phoneNumber: String,
    val relationshipType: String, // 靜態特徵, 行為共現
    val connectionStrength: Float,
    val reasons: List<String>, // 具體原因列表
    val lastActive: String
)
