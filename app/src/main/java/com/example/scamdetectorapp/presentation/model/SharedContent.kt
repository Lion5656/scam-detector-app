package com.example.scamdetectorapp.presentation.model

enum class SharedType {
    TEXT, IMAGE
}

data class SharedContent(
    val type: SharedType,
    val data: String
)
