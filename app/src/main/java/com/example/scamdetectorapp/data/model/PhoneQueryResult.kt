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
    val familyStatic: List<PhoneFamilyStaticItem> = emptyList()
)

data class PhoneFamilyStaticItem(
    val related_phone: String? = null,
    val weight: Int? = null,
    val reason: String? = null,
    val target_phone_type: String? = null
)