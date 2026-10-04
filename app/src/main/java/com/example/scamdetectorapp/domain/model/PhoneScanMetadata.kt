package com.example.scamdetectorapp.domain.model

data class PhoneScanMetadata(
    val phoneNumber: String,
    val status: String,
    val canReport: Boolean,
    val familyStatic: List<RelatedPhone>,
    val reasons: List<String>
)

data class RelatedPhone(
    val phoneNumber: String?,
    val weight: Int?,
    val reason: String?,
    val targetPhoneType: String?
)
