package com.example.scamdetectorapp.presentation.screens.home

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DataThresholding
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.tooling.preview.Preview
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.data.SettingsManager
import com.example.scamdetectorapp.data.repository.NewsRepository
import com.example.scamdetectorapp.data.repository.NewsType
import com.example.scamdetectorapp.service.MonitorService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onNavigateTo: (String) -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()
    
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            handleSpecialPermissions(context)
        } else {
            Log.d("HomeScreen", "部分基礎權限被拒絕")
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF06090E))) {
        // 背景網格
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 40.dp.toPx()
            for (x in 0..size.width.toInt() step gridSize.toInt()) {
                drawLine(Color(0xFF2979FF).copy(alpha = 0.03f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
            }
            for (y in 0..size.height.toInt() step gridSize.toInt()) {
                drawLine(Color(0xFF2979FF).copy(alpha = 0.03f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        color = Color(0xFF448AFF),
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

            // 促銷廣告橫幅
            PromotionBanner()

            Spacer(modifier = Modifier.height(20.dp))

            // --- 主動防護 ---
            Text(
                text = "主動防護",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            var isProtectionEnabled by remember { mutableStateOf(settingsManager.isProtectionEnabled) }
            
            ProtectionFeatureCard(
                title = if (isProtectionEnabled) "即時防護中" else "防護未啟動",
                desc = "通話中檢測，敏感操作防護",
                icon = if (isProtectionEnabled) Icons.Outlined.VerifiedUser else Icons.Outlined.Shield,
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
                        } else {
                            handleSpecialPermissions(context)
                        }
                    }
                    isProtectionEnabled = checked
                    settingsManager.isProtectionEnabled = checked
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

            FeatureCard("網址檢測", "檢查釣魚網站與惡意連結", Icons.Outlined.Public, Color(0xFF2979FF)) { onNavigateTo("網址") }
            Spacer(modifier = Modifier.height(12.dp))
            FeatureCard("電話檢測", "辨識騷擾與詐騙來電", Icons.Outlined.Phone, Color(0xFF00E5FF)) { onNavigateTo("電話") }
            Spacer(modifier = Modifier.height(12.dp))
            FeatureCard("簡訊檢測", "分析可疑簡訊內容", Icons.AutoMirrored.Outlined.Message, Color(0xFFD500F9)) { onNavigateTo("簡訊") }
            Spacer(modifier = Modifier.height(12.dp))
            FeatureCard("購物檢測", "貼上圖片，檢測商品價格是否正常", ImageVector.vectorResource(id = R.drawable.shopping_cart_24dp), Color(0xFFFFD600)) { onNavigateTo("購物檢測") }

            Spacer(modifier = Modifier.height(40.dp))

            // --- 防詐新聞預覽 ---
            NewsPreviewSection(onClick = { onNavigateTo("新聞") })
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DynamicAiRobot(modifier: Modifier, onNavigate: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "robot")
    
    // --- 1. 基礎狀態 ---
    var isCharging by remember { mutableStateOf(false) }
    var holographicText by remember { mutableStateOf("") }
    val shockwaveScale = remember { Animatable(0f) }
    val shockwaveAlpha = remember { Animatable(0f) }

    // --- 2. 基礎動畫 ---
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 10.dp.value,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float"
    )
    val eyesGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "rotate"
    )

    // --- 3. 數據掃描線動畫 ---
    val scanLinePos by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes { durationMillis = 4000; 0f at 0; 0f at 2000; 1f at 3000; 1f at 4000 },
            repeatMode = RepeatMode.Restart
        ), label = "scan"
    )

    // --- 4. 隨機全息提示邏輯 ---
    LaunchedEffect(Unit) {
        val messages = listOf("SCANNING...", "SECURED", "STAY ALERT", "AI ACTIVE", "THREAT 0%")
        while (true) {
            delay((3000..7000).random().toLong())
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
                        // 點擊特效：能量波
                        launch {
                            shockwaveScale.snapTo(0f)
                            shockwaveAlpha.snapTo(0.6f)
                            launch { shockwaveScale.animateTo(2f, tween(500, easing = LinearOutSlowInEasing)) }
                            launch { shockwaveAlpha.animateTo(0f, tween(500)) }
                        }
                        // 充能過場延遲
                        delay(600)
                        onNavigate()
                        isCharging = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // --- 特效層：能量波 ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = (size.minDimension / 2) * shockwaveScale.value,
                alpha = shockwaveAlpha.value,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // --- 特效層：全息提示 ---
        if (holographicText.isNotEmpty()) {
            Text(
                text = holographicText,
                modifier = Modifier.offset(y = (-45).dp),
                color = Color(0xFF448AFF).copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(shadow = Shadow(Color(0xFF2979FF), blurRadius = 8f))
            )
        }

        // --- 核心層：掃描環 ---
        Canvas(modifier = Modifier.size(70.dp).graphicsLayer { rotationZ = rotation }) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(Color.Transparent, Color(0xFF2979FF).copy(alpha = 0.4f), Color.Transparent)),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // --- 核心層：機器人主體 ---
        Surface(
            modifier = Modifier.size(54.dp),
            color = Color(0xFF0D1520),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = if (isCharging) 2.dp else 1.5.dp,
                color = if (isCharging) Color.White else Color(0xFF2979FF).copy(alpha = 0.7f)
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // 數據掃描雷射
                if (scanLinePos > 0f && scanLinePos < 1f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .offset(y = (54 * scanLinePos).dp)
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFFF1744).copy(alpha = 0.5f), Color.Transparent)))
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 電子眼睛 (充能時變色)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp, 5.dp)
                                    .clip(CircleShape)
                                    .background(if (isCharging) Color.White else Color(0xFF00E5FF).copy(alpha = eyesGlow))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // 下方數據條
                    Box(
                        modifier = Modifier
                            .size(24.dp, 2.dp)
                            .background(Color(0xFF2979FF).copy(alpha = 0.3f))
                    )
                }
            }
        }

        // --- 特效層：充能外圈 ---
        if (isCharging) {
            CircularProgressIndicator(
                modifier = Modifier.size(85.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun ProtectionFeatureCard(title: String, desc: String, icon: ImageVector, isEnabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121A21),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isEnabled) Color(0xFF00C853).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background((if (isEnabled) Color(0xFF00C853) else Color(0xFF2979FF)).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = if (isEnabled) Color(0xFF00C853) else Color(0xFF2979FF))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(desc, fontSize = 12.sp, color = Color.Gray)
            }
            Switch(checked = isEnabled, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00C853)))
        }
    }
}

@Composable
private fun FeatureCard(title: String, desc: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121A21),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(desc, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.DarkGray)
        }
    }
}

@Composable
private fun NewsPreviewSection(onClick: () -> Unit) {
    val previewNews = NewsRepository.getPreviewNews()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("防詐資訊專區", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            TextButton(onClick = onClick) { Text("查看更多", color = Color(0xFF448AFF), fontSize = 14.sp) }
        }
        previewNews.forEachIndexed { index, news ->
            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
            Surface(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF121A21),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFF2979FF).copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(if (news.type == NewsType.TREND) Icons.Default.DataThresholding else Icons.Default.Newspaper, contentDescription = null, tint = Color(0xFF2979FF), modifier = Modifier.size(24.dp))
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

private fun handleSpecialPermissions(context: Context) {
    if (!Settings.canDrawOverlays(context)) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
    } else if (!hasUsageStatsPermission(context)) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = @Suppress("DEPRECATION") appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * 促銷廣告橫幅組件
 */
@Composable
private fun PromotionBanner() {
    val scamPrimary = colorResource(R.color.scam_primary)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(), // 讓卡片高度根據內容自適應，避免過大
        shape = RoundedCornerShape(20.dp), // 微調圓角讓比例更精緻
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    scamPrimary.copy(alpha = 0.6f),
                    scamPrimary.copy(alpha = 0.1f),
                    scamPrimary.copy(alpha = 0.7f),
                    scamPrimary.copy(alpha = 0.1f),
                    scamPrimary.copy(alpha = 0.5f)
                )
            )
        ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A21))
    ) {
        // 使用 BoxWithConstraints 作為底層，才能正確拿到寬度與 matchParentSize
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            val isSmallScreen = maxWidth < 360.dp

            // 1. 背景裝飾：科技感發光效果 (自適應卡片大小)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.Center)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(scamPrimary.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
            )

            // 2. 科技背景：細微點狀矩陣 (使用 matchParentSize 填滿當前大小)
            Canvas(modifier = Modifier.matchParentSize().alpha(0.08f)) {
                val gap = 12.dp.toPx()
                for (x in 0..size.width.toInt() step gap.toInt()) {
                    for (y in 0..size.height.toInt() step gap.toInt()) {
                        drawCircle(
                            color = scamPrimary,
                            radius = 1.dp.toPx(),
                            center = Offset(x.toFloat(), y.toFloat())
                        )
                    }
                }
            }

            // 3. 主要內容層
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 24.dp, top = 36.dp, bottom = 18.dp), // 增加上方內襯間距
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center // 讓整排內容在卡片內置中
            ) {
                // 圖標容器
                Box(
                    modifier = Modifier
                        .size(46.dp) // 稍微縮小一點，更符合「稍大於文字」的精緻感
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2979FF),
                                    Color(0xFF00E5FF),
                                    Color(0xFFD500F9)
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(30.dp))

                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.wrapContentSize()
                ) {
                    Text(
                        text = "數位守護 ‧ 全天候命",
                        color = Color.White,
                        fontSize = if (isSmallScreen) 16.sp else 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "透過 AI 技術準確攔截威脅",
                        color = Color.Gray,
                        fontSize = if (isSmallScreen) 12.sp else 13.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
