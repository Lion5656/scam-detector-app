package com.example.scamdetectorapp.presentation.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.presentation.components.CustomBottomBar
import com.example.scamdetectorapp.presentation.screens.dashboard.DashboardScreen
import com.example.scamdetectorapp.presentation.screens.detection.GenericDetectionFlow

@Composable
fun MainAppScreen() {
    // 預設切換為「首頁」
    var currentTab by remember { mutableStateOf("首頁") }
    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        containerColor = backgroundColor,
        bottomBar = {
            // 如果當前是在新聞頁面，可以選擇隱藏 BottomBar 或保持顯示
            // 這裡保持顯示，讓使用者隨時可以切換到其他功能
            CustomBottomBar(currentTab) { selected -> currentTab = selected }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                "首頁" -> HomeScreen(onNavigateTo = { currentTab = it })
                
                "儀表板" -> DashboardScreen()

                "新聞" -> NewsScreen(onBack = { currentTab = "首頁" })

                "網址" -> key(DetectionMode.URL) {
                    GenericDetectionFlow(
                        mode = DetectionMode.URL,
                        title = "檢測詐騙網址",
                        placeholder = "貼上網址，例如 https://...",
                        desc = "支援檢查釣魚網站、假冒連結"
                    )
                }
                "電話" -> key(DetectionMode.PHONE) {
                    GenericDetectionFlow(
                        mode = DetectionMode.PHONE,
                        title = "檢測詐騙電話",
                        placeholder = "輸入電話號碼 (如 0912...)",
                        desc = "檢查常見詐騙客服、假警方電話",
                    )
                }
                "簡訊" -> key(DetectionMode.TEXT) {
                    GenericDetectionFlow(
                        mode = DetectionMode.TEXT,
                        title = "檢測詐騙簡訊",
                        placeholder = "貼上簡訊內容...",
                        desc = "分析關鍵字、假連結、催款語法",
                        isMultiLine = true
                    )
                }
                "購物檢測" -> key(DetectionMode.PRICE) {
                    GenericDetectionFlow(
                        mode = DetectionMode.PRICE,
                        title = "FB 一頁式購物檢測",
                        placeholder = "",
                        desc = "上傳商品圖片，AI 自動辨識並分析價格來源是否異常"
                    )
                }
            }
        }
    }
}
