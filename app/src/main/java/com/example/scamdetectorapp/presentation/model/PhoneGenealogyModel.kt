package com.example.scamdetectorapp.presentation.model

/**
 * 電話族譜資料模型
 * 優化後：統一使用 Float (0.0..1.0) 作為數據單位，便於動畫計算與顯示層格式化。
 */
data class PhoneGenealogyData(
    val rootNumber: String,
    val tagId: String,
    val relatedNodes: List<GenealogyNode>
)

data class GenealogyNode(
    val id: Int,
    val phoneNumber: String,
    val relationship: String, // 關係標籤 (如：靜態特徵)
    val connectionStrength: Float,
    val lastActive: String,
    val reasons: List<String> // 新增：具體關聯原因列表
)
