package com.example.scamdetectorapp.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.presentation.components.CustomBottomBar
import com.example.scamdetectorapp.presentation.screens.dashboard.DashboardScreen
import com.example.scamdetectorapp.presentation.screens.detection.GenericDetectionFlow
import com.example.scamdetectorapp.presentation.screens.detection.PhoneGenealogyScreen
import com.example.scamdetectorapp.presentation.screens.history.HistoryScreen
import com.example.scamdetectorapp.presentation.screens.setting.SettingScreen
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import com.example.scamdetectorapp.ui.theme.AppBackgroundBrush

@Composable
fun MainAppScreen() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.provideFactory(context.applicationContext as android.app.Application)
    )

    // 預設切換為「首頁」
    var currentTab by remember { mutableStateOf("首頁") }
    // 新增：目前是否處於檢測流程中 (首頁的子狀態)
    var activeDetectionMode by remember { mutableStateOf<DetectionMode?>(null) }
    // 新增：儲存要查看族譜的號碼，若不為 null 則顯示族譜頁面
    var genealogyPhoneNumber by remember { mutableStateOf<String?>(null) }

    if (genealogyPhoneNumber != null) {
        PhoneGenealogyScreen(
            phoneNumber = genealogyPhoneNumber!!,
            onBack = { genealogyPhoneNumber = null }
        )
    } else {
        Scaffold(
            containerColor = Color.Transparent, // 讓 Scaffold 容器透明
            contentWindowInsets = WindowInsets(0, 0, 0, 0) // 移除預設 Insets 避免干擾滿版佈局
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackgroundBrush)
            ) {
                // 主內容區域
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 這裡不使用 innerPadding.bottom，讓內容可以延伸到底部
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    when (currentTab) {
                        "首頁" -> {
                            if (activeDetectionMode == null) {
                                HomeScreen(onNavigateTo = { dest ->
                                    // 判斷是進入檢測流程還是跳轉至其他頁面
                                    when (dest) {
                                        "網址", "電話", "簡訊", "購物檢測" -> {
                                            viewModel.resetAllStates()
                                            activeDetectionMode = when (dest) {
                                                "網址" -> DetectionMode.URL
                                                "電話" -> DetectionMode.PHONE
                                                "簡訊" -> DetectionMode.TEXT
                                                "購物檢測" -> DetectionMode.PRICE
                                                else -> null
                                            }
                                        }
                                        else -> {
                                            // 跳轉至 儀表板 或 新聞 等頁面
                                            currentTab = dest
                                        }
                                    }
                                })
                            } else {
                                // 顯示檢測流程
                                val mode = activeDetectionMode!!
                                when (mode) {
                                    DetectionMode.URL -> key(DetectionMode.URL) {
                                        GenericDetectionFlow(
                                            mode = DetectionMode.URL,
                                            title = "檢測詐騙網址",
                                            placeholder = "貼上網址，例如 https://...",
                                            desc = "",
                                            icon = ImageVector.vectorResource(R.drawable.ic_solid_url_v3),
                                            accentColor = Color(0xFFA78BFA),
                                            onSwitchMode = { activeDetectionMode = it },
                                            viewModel = viewModel
                                        )
                                    }
                                    DetectionMode.PHONE -> key(DetectionMode.PHONE) {
                                        GenericDetectionFlow(
                                            mode = DetectionMode.PHONE,
                                            title = "檢測詐騙電話",
                                            placeholder = "輸入電話號碼 (如 0912...)",
                                            desc = "",
                                            icon = ImageVector.vectorResource(R.drawable.ic_solid_phone),
                                            accentColor = Color(0xFFA78BFA),
                                            onNavigateToGenealogy = { genealogyPhoneNumber = it },
                                            onSwitchMode = { activeDetectionMode = it },
                                            viewModel = viewModel
                                        )
                                    }
                                    DetectionMode.TEXT -> key(DetectionMode.TEXT) {
                                        GenericDetectionFlow(
                                            mode = DetectionMode.TEXT,
                                            title = "檢測詐騙簡訊",
                                            placeholder = "貼上簡訊內容...",
                                            desc = "",
                                            icon = ImageVector.vectorResource(R.drawable.ic_solid_message),
                                            accentColor = Color(0xFFA78BFA),
                                            isMultiLine = true,
                                            onSwitchMode = { activeDetectionMode = it },
                                            viewModel = viewModel
                                        )
                                    }
                                    DetectionMode.PRICE -> key(DetectionMode.PRICE) {
                                        GenericDetectionFlow(
                                            mode = DetectionMode.PRICE,
                                            title = "FB 一頁式購物檢測",
                                            placeholder = "",
                                            desc = "",
                                            icon = ImageVector.vectorResource(R.drawable.ic_solid_shopping_v2),
                                            accentColor = Color(0xFFA78BFA),
                                            onSwitchMode = { activeDetectionMode = it },
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }

                        "儀表板" -> DashboardScreen()

                        "新聞" -> NewsScreen(onBack = { currentTab = "首頁" })

                        "檢測紀錄" -> HistoryScreen()

                        "設定" -> SettingScreen()
                    }
                }

                // 將 CustomBottomBar 移入 Box 中並置於底部中心，達成真正的「懸浮於內容之上」
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    // 若處於檢測模式中，傳遞空字串使底部欄不顯示任何選中狀態
                    val displayedTab = if (activeDetectionMode == null) currentTab else ""
                    CustomBottomBar(displayedTab) { selected ->
                        currentTab = selected
                        activeDetectionMode = null
                    }
                }
            }
        }
    }
}
