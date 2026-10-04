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
    // 宣告為 nullable，避免後端未回傳此欄位時 Gson 反射塞入 null 造成 NPE
    val familyStatic: List<PhoneFamilyStaticItem>? = null
)

/**
 * 族譜關聯號碼（欄位名對應後端 snake_case JSON，請勿改名）
 */
data class PhoneFamilyStaticItem(
    val related_phone: String? = null,
    val weight: Int? = null,
    val reason: String? = null,
    val target_phone_type: String? = null
)
