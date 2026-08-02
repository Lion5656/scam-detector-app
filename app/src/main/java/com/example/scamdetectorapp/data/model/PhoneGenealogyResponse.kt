package com.example.scamdetectorapp.data.model

/**
 * 後端族譜 API 回傳格式 (Data Layer)
 */
data class PhoneGenealogyResponse(
    val rootNumber: String,
    val tagId: String,
    val relatedNumbers: List<RelatedNumberResponse>
)

data class RelatedNumberResponse(
    val phoneNumber: String,
    val staticFeatures: StaticFeatureResponse?,
    val behaviorConcurrence: BehaviorResponse?,
    val score: Float,
    val lastActive: String
)

data class StaticFeatureResponse(
    val prefixSimilarity: Boolean,
    val numericDistance: Int?,
    val editDistance: Int?,
    val carrierMatch: Boolean
)

data class BehaviorResponse(
    val sharedReporters: Int,
    val callDensity: Float,
    val contentSimilarity: Float
)
