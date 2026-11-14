package com.example.scamdetectorapp.presentation.screens.home

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.presentation.components.CustomBottomBar
import com.example.scamdetectorapp.presentation.components.DETECTION_MODE_ORDER
import com.example.scamdetectorapp.presentation.model.SharedContent
import com.example.scamdetectorapp.presentation.model.SharedType
import com.example.scamdetectorapp.presentation.screens.dashboard.DashboardScreen
import com.example.scamdetectorapp.presentation.screens.detection.DetectionInputHeader
import com.example.scamdetectorapp.presentation.screens.detection.GenericDetectionFlow
import com.example.scamdetectorapp.presentation.screens.detection.PhoneGenealogyScreen
import com.example.scamdetectorapp.presentation.screens.history.HistoryScreen
import com.example.scamdetectorapp.presentation.screens.setting.SettingScreen
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import com.example.scamdetectorapp.presentation.viewmodel.ScanUiState
import com.example.scamdetectorapp.ui.theme.AppBackgroundBrush
import kotlin.math.abs

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
                                transitionSpec = { fadeThroughTransition() },
                                // 檢測模式之間互切由 DetectionModePager 處理，不重建頁面也不播放換頁轉場
                                contentKey = { it.tab to (it.mode != null) },
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
                                            // 顯示檢測流程：四種模式並排於可左右滑動的頁面中
                                            DetectionModePager(
                                                currentMode = detectionMode,
                                                onModeChange = { activeDetectionMode = it },
                                                onNavigateToGenealogy = { genealogyPhoneNumber = it },
                                                onExitFlow = { activeDetectionMode = null },
                                                viewModel = viewModel
                                            )
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

/**
 * 四種檢測模式依 [DETECTION_MODE_ORDER] 並排成可左右滑動的頁面，拖曳時可看到相鄰模式（與檢測紀錄相同）。
 * 標題與模式頁籤固定在上方不隨頁面滑動；點擊頁籤時直接跳頁不播動畫，滑動停妥後再回報新模式。
 * 僅在目前模式停留於輸入畫面時顯示固定標題並可滑動，掃描中與結果頁維持滿版且不會被滑走。
 */
@Composable
private fun DetectionModePager(
    currentMode: DetectionMode,
    onModeChange: (DetectionMode) -> Unit,
    onNavigateToGenealogy: (String) -> Unit,
    onExitFlow: () -> Unit,
    viewModel: MainViewModel
) {
    val pagerState = rememberPagerState(
        initialPage = DETECTION_MODE_ORDER.indexOf(currentMode),
        pageCount = { DETECTION_MODE_ORDER.size }
    )
    val latestOnModeChange by rememberUpdatedState(onModeChange)

    // 頁籤點擊：直接切換到對應頁面
    LaunchedEffect(currentMode) {
        val target = DETECTION_MODE_ORDER.indexOf(currentMode)
        if (pagerState.currentPage != target && !pagerState.isScrollInProgress) {
            pagerState.scrollToPage(target)
        }
    }
    // 左右滑動停妥後同步目前模式（頁籤選取狀態、返回處理皆依此）
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            latestOnModeChange(DETECTION_MODE_ORDER[page])
        }
    }

    val currentState by viewModel.getState(currentMode).collectAsState()
    val isInputStep = currentState is ScanUiState.Idle

    // 各模式輸入畫面的捲動狀態：固定標題依目前頁面的捲動量一起往上位移
    val scrollStates = remember { DETECTION_MODE_ORDER.associateWith { ScrollState(0) } }
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    // 左右滑動途中依頁面位移比例，在相鄰兩頁的捲動量之間內插，避免標題跳動
    val headerScrollPx by remember {
        derivedStateOf {
            val page = pagerState.currentPage
            val fraction = pagerState.currentPageOffsetFraction
            val from = scrollStates.getValue(DETECTION_MODE_ORDER[page]).value.toFloat()
            val neighbor = DETECTION_MODE_ORDER.getOrNull(if (fraction >= 0f) page + 1 else page - 1)
            val to = neighbor?.let { scrollStates.getValue(it).value.toFloat() } ?: from
            (from + (to - from) * abs(fraction)).coerceAtMost(headerHeightPx.toFloat())
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = isInputStep,
            key = { DETECTION_MODE_ORDER[it] }
        ) { page ->
            val mode = DETECTION_MODE_ORDER[page]
            GenericDetectionFlow(
                mode = mode,
                isMultiLine = mode == DetectionMode.TEXT,
                isActive = mode == currentMode,
                showInputHeader = false,
                inputScrollState = scrollStates.getValue(mode),
                inputHeaderSpace = with(density) { headerHeightPx.toDp() },
                onNavigateToGenealogy = if (mode == DetectionMode.PHONE) onNavigateToGenealogy else null,
                onSwitchMode = onModeChange,
                onExitFlow = {
                    // 網址、電話離開時另外清除檢測狀態（維持原本行為）
                    if (mode == DetectionMode.URL || mode == DetectionMode.PHONE) viewModel.resetState(mode)
                    onExitFlow()
                },
                viewModel = viewModel
            )
        }

        // 標題與頁籤：左右滑動時固定不動，上下捲動時隨內容一起移動
        AnimatedVisibility(
            visible = isInputStep,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.graphicsLayer { translationY = -headerScrollPx }
        ) {
            DetectionInputHeader(
                // 拖曳過半即切換選取狀態，與檢測紀錄頁籤一致
                currentMode = DETECTION_MODE_ORDER[pagerState.currentPage],
                onSwitchMode = onModeChange,
                // 與輸入畫面原本的返回鍵相同：清除目前模式的狀態與輸入後離開
                onExit = {
                    viewModel.resetState(currentMode)
                    viewModel.setInput(currentMode, "")
                    onExitFlow()
                },
                modifier = Modifier
                    // 量在標題本身上：收起時保留最後的高度，回到輸入畫面不會跳動
                    .onSizeChanged { headerHeightPx = it.height }
                    // 底色遮住左右滑動時鄰頁捲上來的內容
                    .background(AppBackgroundBrush)
                    .padding(horizontal = 24.dp)
            )
        }
    }
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
