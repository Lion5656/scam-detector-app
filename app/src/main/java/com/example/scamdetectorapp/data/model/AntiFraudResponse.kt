package com.example.scamdetectorapp.data.model

/**
 * 統一的 Response 包裝
 * 使用 Generic <T> 讓 data 欄位可以自動解析成對應的物件
 */
data class AntiFraudResponse<T>(
    val success: Boolean,
    val version: String,
    val data: T?,
    val error_code: Int? = null,
    val error_message: String? = null
)
