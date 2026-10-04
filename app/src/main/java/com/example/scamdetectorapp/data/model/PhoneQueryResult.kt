package com.example.scamdetectorapp.data.model

data class PhoneQueryResult(
    val phoneNumber: String? = null,
    val status: String? = null,
    val phoneType: String? = null,
    val totalReports: Int? = null,
    val firstReportedAt: String? = null,
    val lastReportedAt: String? = null,
    val ownerName: String? = null,
    val canReport: Boolean? = null,
    // 號碼族譜：後端回傳的靜態特徵關聯號碼清單
    val familyStatic: List<PhoneFamilyStaticItem>? = null
)

/**
 * 族譜關聯號碼
 */
data class PhoneFamilyStaticItem(
    val relatedPhone: String? = null,
    val weight: Int? = null,
    val reason: String? = null,
    val targetPhoneType: String? = null
)
