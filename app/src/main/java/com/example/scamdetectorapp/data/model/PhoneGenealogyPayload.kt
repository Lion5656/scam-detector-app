package com.example.scamdetectorapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * 後端族譜 API 回傳格式 (Data Layer)
 */
data class PhoneGenealogyPayload(
    @SerializedName("root_number") val rootNumber: String,
    @SerializedName("cluster_id") val tagId: String,
    @SerializedName("related_numbers") val relatedNumbers: List<RelatedNumberPayload>
)

data class RelatedNumberPayload(
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("static_features") val staticFeatures: StaticFeaturePayload?,
    @SerializedName("behavior_cooccurrence") val behaviorCooccurrence: BehaviorPayload?,
    @SerializedName("score") val score: Float,
    @SerializedName("last_active") val lastActive: String
)

data class StaticFeaturePayload(
    @SerializedName("prefix_similarity") val prefixSimilarity: Boolean,
    @SerializedName("numeric_distance") val numericDistance: Int?,
    @SerializedName("edit_distance") val editDistance: Int?,
    @SerializedName("carrier_match") val carrierMatch: Boolean
)

data class BehaviorPayload(
    @SerializedName("shared_reporters") val sharedReporters: Int,
    @SerializedName("call_density") val callDensity: Float,
    @SerializedName("content_similarity") val contentSimilarity: Float
)
