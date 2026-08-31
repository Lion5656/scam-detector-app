package com.example.scamdetectorapp.presentation.screens.home

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.presentation.components.CustomBottomBar
import com.example.scamdetectorapp.presentation.model.SharedContent
import com.example.scamdetectorapp.presentation.model.SharedType
import com.example.scamdetectorapp.presentation.screens.dashboard.DashboardScreen
import com.example.scamdetectorapp.presentation.screens.detection.GenericDetectionFlow
import com.example.scamdetectorapp.presentation.screens.detection.PhoneGenealogyScreen
import com.example.scamdetectorapp.presentation.screens.detection.ScreenStep
import com.example.scamdetectorapp.presentation.screens.history.HistoryScreen
import com.example.scamdetectorapp.presentation.screens.setting.SettingScreen
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import com.example.scamdetectorapp.ui.theme.AppBackgroundBrush

@Composable
fun MainAppScreen(
    sharedContent: SharedContent? = null,
    onSharedContentHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.provideFactory(context.applicationContext as Application)
    )

    val initialData = remember(sharedContent) {
        sharedContent?.let{
            val mode = detectModeFromContent(it)
            val input = if (it.type == SharedType.IMAGE) "uri:${it.data}" else it.data.trim()
            Pair(mode, input)
        }
    }

    val sharedDetectionMode by remember {
        mutableStateOf(initialData?.first)
    }

    var currentTab by remember { mutableStateOf(if(initialData != null) "分享檢測" else "首頁") }

    var activeDetectionMode by remember { mutableStateOf(initialData?.first) }
    // 儲存要查看族譜的號碼，若不為 null 則顯示族譜頁面
    var genealogyPhoneNumber by remember { mutableStateOf<String?>(null) }

    val isShareAutoInputEnabled by viewModel.isShareAutoInputEnabled.collectAsState()

    LaunchedEffect(sharedContent) {
        initialData?.let { (mode, input) ->
            viewModel.resetAllStates()
            if (isShareAutoInputEnabled) {
                viewModel.setInput(mode, input)
                viewModel.scan(mode, input)
            }

            onSharedContentHandled()
        }
    }

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
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    when (currentTab) {
                        "分享檢測" -> {
                            sharedDetectionMode?.let { mode ->
                                GenericDetectionFlow(
                                    mode = mode,
                                    initialStep = ScreenStep.SCANNING,
                                    onExitFlow = { currentTab = "首頁" },
                                    viewModel = viewModel,
                                )
                            }
                        }
                        "首頁" -> {
                            if (activeDetectionMode == null) {
                                HomeScreen(
                                    onNavigateTo = { dest ->
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
                                    },
                                    viewModel = viewModel
                                )
                            } else {
                                // 顯示檢測流程
                                when (val mode = activeDetectionMode!!) {
                                    DetectionMode.URL -> key(DetectionMode.URL) {
                                        GenericDetectionFlow(
                                            mode = DetectionMode.URL,
                                            onSwitchMode = { activeDetectionMode = it },
                                            onExitFlow = { 
                                                viewModel.resetState(mode)
                                                activeDetectionMode = null 
                                            },
                                            viewModel = viewModel
                                        )
                                    }
                                    DetectionMode.PHONE -> key(DetectionMode.PHONE) {
                                        GenericDetectionFlow(
                                            mode = DetectionMode.PHONE,
                                            onNavigateToGenealogy = { genealogyPhoneNumber = it },
                                            onSwitchMode = { activeDetectionMode = it },
                                            onExitFlow = { 
                                                viewModel.resetState(mode)
                                                activeDetectionMode = null 
                                            },
                                            viewModel = viewModel
                                        )
                                    }
                                    DetectionMode.TEXT -> key(DetectionMode.TEXT) {
                                        GenericDetectionFlow(
                                            mode = DetectionMode.TEXT,
                                            isMultiLine = true,
                                            onSwitchMode = { activeDetectionMode = it },
                                            onExitFlow = { activeDetectionMode = null },
                                            viewModel = viewModel
                                        )
                                    }
                                    DetectionMode.PRICE -> key(DetectionMode.PRICE) {
                                        GenericDetectionFlow(
                                            mode = DetectionMode.PRICE,
                                            onSwitchMode = { activeDetectionMode = it },
                                            onExitFlow = { activeDetectionMode = null },
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                        "儀表板" -> DashboardScreen(onBack = { currentTab = "首頁" })

                        "新聞" -> NewsScreen(onBack = { currentTab = "首頁" })

                        "檢測紀錄" -> HistoryScreen(onBack = { currentTab = "首頁" }, viewModel = viewModel)

                        "設定" -> SettingScreen(onBack = { currentTab = "首頁" }, viewModel = viewModel)
                    }
                }

                // 將 CustomBottomBar 移入 Box 中並置於底部中心，達成真正的「懸浮於內容之上」
                val hideBottomBar = activeDetectionMode != null ||
                                   currentTab == "儀表板" || 
                                   currentTab == "新聞" || 
                                   currentTab == "檢測紀錄" ||
                                   currentTab == "設定"
                if (!hideBottomBar) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        CustomBottomBar(currentTab) { selected ->
                            currentTab = selected
                            activeDetectionMode = null
                        }
                    }
                }
            }
        }
    }
}

private fun detectModeFromContent(content: SharedContent) : DetectionMode{
    return when (content.type) {
        SharedType.IMAGE -> DetectionMode.PRICE
        SharedType.TEXT -> {
            val text = content.data.trim()
            val urlRegex = Regex(
                """^(https?://)?(www\.)?[a-zA-Z0-9-]+(\.[a-zA-Z0-9-]+)+(/\S*)?$""",
                RegexOption.IGNORE_CASE
            )
            when {
                text.matches(urlRegex) -> DetectionMode.URL
                text.filter { it.isDigit() }.startsWith("0") && text.length in 9..10  -> DetectionMode.PHONE
                else -> DetectionMode.TEXT
            }
        }
    }
}
