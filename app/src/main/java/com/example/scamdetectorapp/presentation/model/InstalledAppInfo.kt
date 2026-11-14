package com.example.scamdetectorapp.presentation.model

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 已安裝 App 的預載資訊（防護 App 管理清單使用）
 * 名稱與圖示於 App 啟動時即在背景解析完成，避免開啟清單時才於主執行緒讀取 PackageManager。
 */
data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap
)
