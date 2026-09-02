package com.example.scamdetectorapp.presentation.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataThresholding
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetectorapp.data.repository.NewsRepository
import com.example.scamdetectorapp.data.repository.NewsType
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import com.example.scamdetectorapp.service.MonitorService
import com.example.scamdetectorapp.ui.theme.*
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigateTo: (String) -> Unit,
    viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(LocalContext.current.applicationContext as android.app.Application))
) {
    val isProtectionEnabled by viewModel.isProtectionEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // 請求基礎權限後，務必先刷新狀態再判斷
        viewModel.updatePermissionStatus()

        // 使用協程確保在狀態更新後再檢查（或是直接判斷，因為 update 是在背景跑，
        // 但 hasDisplayPermissions 讀取的是 Flow 的最新值，這裏可能會有極短暫的同步落差）
        scope.launch {
            delay(100) // 給予極短時間讓 ViewModel 的 launch 執行
            if (!viewModel.hasDisplayPermissions()) {
                viewModel.setHighlightPermission(true)
                onNavigateTo("設定")
            } else {
                // 全部滿足則開啟防護
                viewModel.toggleProtectionEnabled(true)
                val intent = Intent(context, MonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }

    // 當從設定頁面回來時，自動刷新權限狀態
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.updatePermissionStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize().background(com.example.scamdetectorapp.ui.theme.AppBackgroundBrush)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 40.dp.toPx()
            for (x in 0..size.width.toInt() step gridSize.toInt()) {
                drawLine(ElectricBlue.copy(alpha = 0.03f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
            }
            for (y in 0..size.height.toInt() step gridSize.toInt()) {
                drawLine(ElectricBlue.copy(alpha = 0.03f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 在最上方增加大範圍邊距，讓內容初始位置下移，營造內容從導覽列下方冒出來的感覺
            Spacer(modifier = Modifier.height(60.dp))

            // --- 頂部區域與動態機器人 ---
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "SCAM GUARD",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                    Text(
                        text = "您的全方位防詐護盾",
                        fontSize = 13.sp,
                        color = VibrantBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 右上角動態小機器人
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp)
                ) {
                    DynamicAiRobot(
                        modifier = Modifier.size(100.dp),
                        onNavigate = { onNavigateTo("儀表板") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))


            // --- 主動防護 ---
            Text(
                text = "主動防護",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProtectionFeatureCard(
                title = if (isProtectionEnabled) "即時防護中" else "防護未啟動",
                desc = "通話中進行防護",
                isEnabled = isProtectionEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        val permissions = mutableListOf(Manifest.permission.READ_PHONE_STATE)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        val needsBasePermissions = permissions.any {
                            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                        }
                        
                        if (needsBasePermissions) {
                            permissionsLauncher.launch(permissions.toTypedArray())
                            return@ProtectionFeatureCard
                        } else {
                            // 檢查核心特殊權限 (Overlay, UsageStats)
                            if (!viewModel.hasDisplayPermissions()) {
                                // 延遲跳轉，先讓使用者看到 Switch 的點擊反饋但不改變狀態
                                scope.launch {
                                    viewModel.setHighlightPermission(true)
                                    delay(800) // 稍微停頓，避免跳轉過於突兀
                                    onNavigateTo("設定")
                                }
                                return@ProtectionFeatureCard
                            }
                        }
                    }

                    // 只有在權限滿足時才執行切換邏輯
                    viewModel.toggleProtectionEnabled(checked)

                    val intent = Intent(context, MonitorService::class.java)
                    if (checked) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
                    } else {
                        context.stopService(intent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- 快速檢測 ---
            Text(
                text = "快速檢測",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            StartDetectionCard { onNavigateTo("購物檢測") }


            Spacer(modifier = Modifier.height(40.dp))

            // --- 防詐新聞預覽 ---
            NewsPreviewSection(onClick = { onNavigateTo("新聞") })

            Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun DynamicAiRobot(modifier: Modifier, onNavigate: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "robot")
    var isCharging by remember { mutableStateOf(false) }
    var holographicText by remember { mutableStateOf("") }
    val shockwaveScale = remember { Animatable(0f) }
    val shockwaveAlpha = remember { Animatable(0f) }

    // 加載新版 Lottie 機器人動畫
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("Robot assistant  Online manager.lottie")
    )

    val floatAnim by infiniteTransition.animateFloat(0f, 10.dp.value, infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "rotate")

    LaunchedEffect(Unit) {
        val messages = listOf("AI ACTIVE", "ANALYZING", "SCANNING...", "MANAGER ON", "SECURED")
        while (true) {
            delay((4000..8000).random().toLong())
            holographicText = messages.random()
            delay(2000)
            holographicText = ""
        }
    }

    Box(
        modifier = modifier
            .offset(y = floatAnim.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!isCharging) {
                    isCharging = true
                    coroutineScope.launch {
                        shockwaveScale.snapTo(0f)
                        shockwaveAlpha.snapTo(0.6f)
                        launch { shockwaveScale.animateTo(2.5f, tween(600, easing = LinearOutSlowInEasing)) }
                        launch { shockwaveAlpha.animateTo(0f, tween(600)) }
                        delay(600)
                        onNavigate()
                        isCharging = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 點擊時的衝擊波特效
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color(0xFF00E5FF), radius = (size.minDimension / 2) * shockwaveScale.value, alpha = shockwaveAlpha.value, style = Stroke(width = 2.dp.toPx()))
        }

        // 外圍全息旋轉環
        Canvas(modifier = Modifier.size(85.dp).graphicsLayer { rotationZ = rotation }) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(Color.Transparent, Color(0xFF4F7CFF).copy(alpha = 0.3f), Color.Transparent)),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // 新版 Lottie 機器人助理
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.size(75.dp),
            contentScale = ContentScale.Fit
        )

        // 浮動的全息文字
        if (holographicText.isNotEmpty()) {
            Text(
                text = holographicText,
                modifier = Modifier.offset(y = (-50).dp),
                color = Color(0xFF4F7CFF).copy(alpha = 0.9f),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(shadow = Shadow(Color(0xFF4F7CFF), blurRadius = 8f))
            )
        }
    }
}

@Composable
private fun StartDetectionCard(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "card_scan")

    // 掃描線位移偏量
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_progress"
    )

    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF2962FF), Color(0xFF00D1FF)),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {
            // 背景裝飾圓圈
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.width / 2.5f,
                    center = Offset(size.width * 0.9f, size.height * 0.1f)
                )
            }

            // 大範圍掃描光束特效 (從左上到右下)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // 擴大位移範圍使大面積光束
                val progress = scanProgress * 2.5f - 0.75f
                val xPos = width * progress
                val yPos = height * progress

                val scanBrush = Brush.linearGradient(
                    0.2f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = 0.15f),
                    0.8f to Color.Transparent,
                    start = Offset(xPos - 400f, yPos - 400f),
                    end = Offset(xPos + 400f, yPos + 400f)
                )

                drawRect(
                    brush = scanBrush,
                    size = size
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "開始檢測",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "檢測未知風險來源",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ProtectionFeatureCard(
    title: String,
    desc: String,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val targetColor = if (isEnabled) BrightGreen else ElectricBlue
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(400),
        label = "color"
    )

    // 2. 定義「安全脈衝核心」圖示
    val cardShape = RoundedCornerShape(22.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp),
        color = Color(0xFF252E3A),
        shape = cardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圖示區域：使用「活性守護盾」意象 (盾牌 + 即時脈衝)
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                val activeVitalShield = remember(isEnabled, animatedColor) {
                    ImageVector.Builder(
                        defaultWidth = 24.dp, defaultHeight = 24.dp,
                        viewportWidth = 24f, viewportHeight = 24f
                    ).apply {
                        // 1. 外盾牌：專業穩重的輪廓
                        path(
                            fill = if (isEnabled) SolidColor(animatedColor) else null,
                            stroke = if (!isEnabled) SolidColor(animatedColor) else null,
                            strokeLineWidth = 2.0f,
                            strokeLineCap = StrokeCap.Round,
                            strokeLineJoin = StrokeJoin.Round
                        ) {
                            moveTo(12f, 1f)
                            lineTo(3f, 5f)
                            verticalLineTo(11f)
                            curveTo(3f, 16.55f, 6.84f, 21.74f, 12f, 23f)
                            curveTo(17.16f, 21.74f, 21f, 16.55f, 21f, 11f)
                            verticalLineTo(5f)
                            lineTo(12f, 1f)
                            close()
                        }
                        // 2. 中心脈衝線：象徵即時活性 (Active Pulse)
                        path(
                            stroke = if (isEnabled) SolidColor(Color.White) else SolidColor(animatedColor),
                            strokeLineWidth = 1.8f,
                            strokeLineCap = StrokeCap.Round,
                            strokeLineJoin = StrokeJoin.Round
                        ) {
                            moveTo(7f, 12f)
                            lineTo(9.5f, 12f)
                            lineTo(11f, 8f)
                            lineTo(13f, 16f)
                            lineTo(14.5f, 12f)
                            lineTo(17f, 12f)
                        }
                    }.build()
                }

                Icon(
                    imageVector = activeVitalShield,
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Normal
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = animatedColor,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.1f),
                    uncheckedBorderColor = Color.Transparent
                ),
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}


@Composable
private fun NewsPreviewSection(onClick: () -> Unit) {
    val previewNews = NewsRepository.getPreviewNews()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("防詐資訊專區", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = onClick) { Text("查看更多", color = VibrantBlue, fontSize = 14.sp) }
        }
        previewNews.forEachIndexed { index, news ->
            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
            Surface(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceDark,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(ElectricBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(if (news.type == NewsType.TREND) Icons.Default.DataThresholding else Icons.Default.Newspaper, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(news.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(news.summary, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

