package com.example.scamdetectorapp.presentation.model

/**
 * 電話族譜資料模型
 * 優化後：統一使用 Float (0.0..1.0) 作為數據單位，便於動畫計算與顯示層格式化。
 */
data class PhoneGenealogyData(
    val rootNumber: String,
    val riskScore: Float, // 規則式風險評分 (0.0 - 1.0)
    val associationScore: Float, // 號碼關聯分數 (0.0 - 1.0)
    val clusterId: String, // 詐騙集團類型名稱
    val relatedNodes: List<GenealogyNode>
)

data class GenealogyNode(
    val id: Int,
    val phoneNumber: String,
    val relationship: String, // 關係標籤
    val connectionStrength: Float, // 關聯強度 (0.0 - 1.0)
    val lastActive: String = "2026/06/26",
    val detailReason: String = ""
)
