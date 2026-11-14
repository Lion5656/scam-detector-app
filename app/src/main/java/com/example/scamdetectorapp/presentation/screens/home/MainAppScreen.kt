package com.example.scamdetectorapp.presentation.screens.home

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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

    // 進出族譜頁面時以淡出→淡入銜接；底層先鋪背景，轉場空檔不會露出視窗底色
    AnimatedContent(
        targetState = genealogyPhoneNumber,
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundBrush),
        transitionSpec = { fadeThroughTransition() },
        label = "GenealogyTransition"
    ) { phoneNumber ->
        if (phoneNumber != null) {
            PhoneGenealogyScreen(
                phoneNumber = phoneNumber,
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
                            // 切換分頁、進出檢測流程時加入轉場動畫；
                            // 內容一律依 page 參數繪製，離場中的舊頁面才不會讀到新狀態
                            AnimatedContent(
                                targetState = MainPage(
                                    tab = currentTab,
                                    mode = if (currentTab == "首頁") activeDetectionMode else null
                                ),
                                modifier = Modifier.fillMaxSize(),
                                transitionSpec = { mainPageTransition() },
                                label = "MainPageTransition"
                            ) { page ->
                                when (page.tab) {
                                    "首頁" -> {
                                        val detectionMode = page.mode
                                        if (detectionMode == null) {
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
                                            when (val mode: DetectionMode = detectionMode) {
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
                    }

                    // 將 CustomBottomBar 移入 Box 中並置於底部中心，達成真正的「懸浮於內容之上」
                    val hideBottomBar = sharedContent != null ||
                                       activeDetectionMode != null ||
                                       currentTab == "儀表板" || 
                                       currentTab == "新聞" || 
                                       currentTab == "檢測紀錄" ||
                                       currentTab == "設定"
                    Box(
                        modifier = Modifier.align(Alignment.BottomCenter)
                    ) {
                        // 捲動或切換頁面時以 0.2s 滑動 + 淡入淡出銜接顯示／隱藏
                        AnimatedVisibility(
                            visible = !hideBottomBar && isBottomBarScrollVisible,
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

// 主內容區目前的頁面：分頁 + 首頁內的檢測模式 (非首頁時為 null)
private data class MainPage(val tab: String, val mode: DetectionMode?)

// 檢測模式之間互切（畫面結構相同）只做短暫交叉淡化，其餘換頁使用淡出→淡入
private fun AnimatedContentTransitionScope<MainPage>.mainPageTransition(): ContentTransform =
    if (initialState.mode != null && targetState.mode != null) {
        (fadeIn(tween(MODE_SWITCH_DURATION_MS)) togetherWith
                fadeOut(tween(MODE_SWITCH_DURATION_MS))).using(null)
    } else {
        fadeThroughTransition()
    }

// Material fade through：舊頁先淡出，新頁接著淡入並由 96% 微幅放大，總長約 0.3s
private fun <S> AnimatedContentTransitionScope<S>.fadeThroughTransition(): ContentTransform {
    val enterSpec = tween<Float>(PAGE_ENTER_DURATION_MS, delayMillis = PAGE_EXIT_DURATION_MS)
    return (fadeIn(enterSpec) + scaleIn(enterSpec, initialScale = PAGE_ENTER_INITIAL_SCALE)) togetherWith
            fadeOut(tween(PAGE_EXIT_DURATION_MS)) using null // 頁面皆為滿版，不需尺寸動畫
}

// 換頁轉場：舊頁淡出時間、新頁淡入時間 (ms) 與新頁起始縮放比例
private const val PAGE_EXIT_DURATION_MS = 90
private const val PAGE_ENTER_DURATION_MS = 210
private const val PAGE_ENTER_INITIAL_SCALE = 0.96f

// 檢測模式互切的交叉淡化時間 (ms)
private const val MODE_SWITCH_DURATION_MS = 150

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
