package com.example.scamdetectorapp.presentation.screens.home

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.presentation.components.CustomBottomBar
import com.example.scamdetectorapp.presentation.model.SharedContent
import com.example.scamdetectorapp.presentation.model.SharedType
import com.example.scamdetectorapp.presentation.screens.dashboard.DashboardScreen
import com.example.scamdetectorapp.presentation.screens.detection.GenericDetectionFlow
import com.example.scamdetectorapp.presentation.screens.detection.PhoneGenealogyScreen
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

    var currentTab by remember { mutableStateOf("首頁") }

    var activeDetectionMode by remember { mutableStateOf<DetectionMode?>(null) }
    // 儲存要查看族譜的號碼，若不為 null 則顯示族譜頁面
    var genealogyPhoneNumber by remember { mutableStateOf<String?>(null) }
    // 每次收到分享內容遞增，強制重建檢測流程以載入新的輸入
    var shareSession by remember { mutableIntStateOf(0) }

    val isShareAutoInputEnabled by viewModel.isShareAutoInputEnabled.collectAsState()

    // 依捲動方向切換導覽列：內容往下捲動時隱藏，往上捲動時重新顯示
    var isBottomBarScrollVisible by remember { mutableStateOf(true) }
    val bottomBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                when {
                    available.y < -BOTTOM_BAR_TOGGLE_THRESHOLD_PX -> isBottomBarScrollVisible = false
                    available.y > BOTTOM_BAR_TOGGLE_THRESHOLD_PX -> isBottomBarScrollVisible = true
                }
                return Offset.Zero // 不消耗捲動量，僅觀察方向
            }
        }
    }
    // 切換分頁或進出檢測流程時重置為顯示，避免回到首頁時導覽列仍是隱藏狀態
    LaunchedEffect(currentTab, activeDetectionMode) {
        isBottomBarScrollVisible = true
    }

    // 分享內容直接導入一般檢測流程（與首頁進入相同），返回後可正常切換模式與操作
    LaunchedEffect(sharedContent) {
        val content = sharedContent ?: return@LaunchedEffect
        val mode = detectModeFromContent(content)
        val input = if (content.type == SharedType.IMAGE) "uri:${content.data}" else content.data.trim()

        viewModel.resetAllStates()
        if (isShareAutoInputEnabled) {
            viewModel.setInput(mode, input)
            // scan 會同步設為 Loading，檢測流程建立時即直接進入掃描畫面
            viewModel.scan(mode, input)
        }

        genealogyPhoneNumber = null
        currentTab = "首頁"
        activeDetectionMode = mode
        shareSession++
        onSharedContentHandled()
    }

    if (genealogyPhoneNumber != null) {
        PhoneGenealogyScreen(
            phoneNumber = genealogyPhoneNumber!!,
            onBack = { genealogyPhoneNumber = null },
            viewModel = viewModel
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
                        .nestedScroll(bottomBarScrollConnection)
                ) {
                    // 分享內容尚未處理完成前不顯示畫面，避免閃現首頁或舊的檢測流程
                    if (sharedContent == null) key(shareSession) {
                        when (currentTab) {
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
                            "儀表板" -> DashboardScreen(onBack = { currentTab = "首頁" }, viewModel = viewModel)

                            "新聞" -> NewsScreen(onBack = { currentTab = "首頁" }, viewModel = viewModel)

                            "檢測紀錄" -> HistoryScreen(onBack = { currentTab = "首頁" }, viewModel = viewModel)

                            "設定" -> SettingScreen(onBack = { currentTab = "首頁" }, viewModel = viewModel)
                        }
                    }
                }

                // 將 CustomBottomBar 移入 Box 中並置於底部中心，達成真正的「懸浮於內容之上」
                val hideBottomBar = sharedContent != null ||
                                   activeDetectionMode != null ||
                                   currentTab == "儀表板" || 
                                   currentTab == "新聞" || 
                                   currentTab == "檢測紀錄" ||
                                   currentTab == "設定"
                if (!hideBottomBar) {
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        // 捲動時以 0.2s 滑動 + 淡入淡出銜接顯示／隱藏
                        AnimatedVisibility(
                            visible = isBottomBarScrollVisible,
                            enter = slideInVertically(tween(BOTTOM_BAR_ANIM_DURATION_MS)) { it } +
                                    fadeIn(tween(BOTTOM_BAR_ANIM_DURATION_MS)),
                            exit = slideOutVertically(tween(BOTTOM_BAR_ANIM_DURATION_MS)) { it } +
                                   fadeOut(tween(BOTTOM_BAR_ANIM_DURATION_MS))
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
}

// 導覽列顯示／隱藏動畫時間 (ms)
private const val BOTTOM_BAR_ANIM_DURATION_MS = 200

// 單次捲動量超過此值才切換導覽列，避免手指微動造成閃爍 (px)
private const val BOTTOM_BAR_TOGGLE_THRESHOLD_PX = 4f

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
