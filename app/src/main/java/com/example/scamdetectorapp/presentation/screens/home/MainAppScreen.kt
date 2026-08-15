package com.example.scamdetectorapp.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.presentation.components.CustomBottomBar
import com.example.scamdetectorapp.presentation.screens.dashboard.DashboardScreen
import com.example.scamdetectorapp.presentation.screens.detection.GenericDetectionFlow
import com.example.scamdetectorapp.presentation.screens.detection.PhoneGenealogyScreen
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import com.example.scamdetectorapp.ui.theme.AppBackgroundBrush

private fun tabLabelFor(mode: DetectionMode): String = when (mode) {
    DetectionMode.URL -> "網址"
    DetectionMode.PHONE -> "電話"
    DetectionMode.TEXT -> "簡訊"
    DetectionMode.PRICE -> "購物檢測"
}

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.provideFactory(context.applicationContext as android.app.Application)
    )

    // 預設切換為「首頁」
    var currentTab by remember { mutableStateOf("首頁") }
    // 新增：儲存要查看族譜的號碼，若不為 null 則顯示族譜頁面
    var genealogyPhoneNumber by remember { mutableStateOf<String?>(null) }
    
    val backgroundColor = MaterialTheme.colorScheme.background

    if (genealogyPhoneNumber != null) {
        PhoneGenealogyScreen(
            phoneNumber = genealogyPhoneNumber!!,
            onBack = { genealogyPhoneNumber = null }
        )
    } else {
        Scaffold(
            containerColor = backgroundColor,
            bottomBar = {
                CustomBottomBar(currentTab) { selected -> currentTab = selected }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(AppBackgroundBrush)
            ) {
                when (currentTab) {
                    "首頁" -> HomeScreen(onNavigateTo = { 
                        // 當從首頁跳轉至檢測頁面時，強制重置所有狀態回到輸入頁
                        viewModel.resetAllStates()
                        currentTab = it 
                    })
                    
                    "儀表板" -> DashboardScreen()

                    "新聞" -> NewsScreen(onBack = { currentTab = "首頁" })

                    "網址" -> key(DetectionMode.URL) {
                        GenericDetectionFlow(
                            mode = DetectionMode.URL,
                            title = "檢測詐騙網址",
                            placeholder = "貼上網址，例如 https://...",
                            desc = "支援檢查釣魚網站、假冒連結",
                            onSwitchMode = { newMode -> currentTab = tabLabelFor(newMode) },
                            viewModel = viewModel
                        )
                    }
                    "電話" -> key(DetectionMode.PHONE) {
                        GenericDetectionFlow(
                            mode = DetectionMode.PHONE,
                            title = "檢測詐騙電話",
                            placeholder = "輸入電話號碼 (如 0912...)",
                            desc = "檢查常見詐騙客服、假警方電話",
                            onNavigateToGenealogy = { genealogyPhoneNumber = it },
                            onSwitchMode = { newMode -> currentTab = tabLabelFor(newMode) },
                            viewModel = viewModel
                        )
                    }
                    "簡訊" -> key(DetectionMode.TEXT) {
                        GenericDetectionFlow(
                            mode = DetectionMode.TEXT,
                            title = "檢測詐騙簡訊",
                            placeholder = "貼上簡訊內容...",
                            desc = "分析關鍵字、假連結、催款語法",
                            isMultiLine = true,
                            onSwitchMode = { newMode -> currentTab = tabLabelFor(newMode) },
                            viewModel = viewModel
                        )
                    }
                    "購物檢測" -> key(DetectionMode.PRICE) {
                        GenericDetectionFlow(
                            mode = DetectionMode.PRICE,
                            title = "FB 一頁式購物檢測",
                            placeholder = "",
                            desc = "上傳商品圖片，AI 自動辨識並分析價格來源是否異常",
                            onSwitchMode = { newMode -> currentTab = tabLabelFor(newMode) },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
