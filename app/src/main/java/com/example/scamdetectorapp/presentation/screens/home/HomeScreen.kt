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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.data.SettingsManager
import com.example.scamdetectorapp.data.repository.NewsRepository
import com.example.scamdetectorapp.data.repository.NewsType
import com.example.scamdetectorapp.service.MonitorService
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import com.example.scamdetectorapp.presentation.viewmodel.RecentScansUiState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(onNavigateTo: (String) -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(context.applicationContext as android.app.Application))
    val recentScansState by viewModel.recentScansState.collectAsState()
    
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

                // 右上角動態小機器人 (智慧互動特效系統)
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

            // 新增：類廣告促銷卡片
            PromotionBanner()

            Spacer(modifier = Modifier.height(40.dp))

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
                icon = if (isProtectionEnabled) Icons.Outlined.VerifiedUser else ImageVector.vectorResource(id = R.drawable.security_24dp),
                isEnabled = isProtectionEnabled,
                onCheckedChange = { checked ->
                    if (checked) {
                        val permissions = mutableListOf(Manifest.permission.READ_PHONE_STATE)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        
                        val needsBasePermissions = permissions.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
                        if (needsBasePermissions) permissionsLauncher.launch(permissions.toTypedArray()) else handleSpecialPermissions(context)
                    }
                    isProtectionEnabled = checked
                    settingsManager.isProtectionEnabled = checked
                    val intent = Intent(context, MonitorService::class.java)
                    if (checked) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
                    } else { context.stopService(intent) }
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
            FeatureCard("FB一頁式購物檢測", "貼上圖片，檢測商品價格是否正常", ImageVector.vectorResource(id = R.drawable.shopping_cart_24dp), Color(0xFF448AFF)) { onNavigateTo("購物檢測") }

            Spacer(modifier = Modifier.height(32.dp))

            // --- 最近檢測 ---
            RecentScansSection(
                state = recentScansState,
                onViewAllClick = { onNavigateTo("歷史紀錄") },
                onItemClick = { recordId -> onNavigateTo("詳情/$recordId") },
                onScanNowClick = { /* 這裡可以決定跳轉到哪個檢測頁面，預設電話 */ onNavigateTo("電話") }
            )

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
private fun ProtectionFeatureCard(title: String, desc: String, icon: ImageVector, isEnabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121A21),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isEnabled) Color(0xFF00C853).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
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
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
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
private fun RecentScansSection(
    state: RecentScansUiState,
    onViewAllClick: () -> Unit,
    onItemClick: (Long) -> Unit,
    onScanNowClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "最近檢測",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
            TextButton(onClick = onViewAllClick) {
                Text("查看全部", color = Color(0xFF448AFF), fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (state) {
            is RecentScansUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2979FF))
                }
            }
            is RecentScansUiState.Empty -> {
                EmptyRecentScans(onScanNowClick)
            }
            is RecentScansUiState.Success -> {
                state.scans.forEach { scan ->
                    RecentScanItem(scan, onItemClick)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            is RecentScansUiState.Error -> {
                Text("載入失敗: ${state.message}", color = Color.Red, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun RecentScanItem(
    scan: com.example.scamdetectorapp.data.local.DetectionEntity,
    onClick: (Long) -> Unit
) {
    val icon = when (scan.type.uppercase()) {
        "PHONE" -> Icons.Outlined.Phone
        "URL" -> Icons.Outlined.Public
        "TEXT" -> Icons.AutoMirrored.Outlined.Message
        "PRICE" -> ImageVector.vectorResource(id = R.drawable.shopping_cart_24dp)
        else -> Icons.Outlined.HelpOutline
    }

    val riskColor = when (scan.riskLevel.uppercase()) {
        "SAFE" -> Color(0xFF00C853)
        "SUSPICIOUS" -> Color(0xFFFFAB40)
        "DANGEROUS" -> Color(0xFFFF5252)
        else -> Color.Gray
    }

    val riskText = when (scan.riskLevel.uppercase()) {
        "SAFE" -> "安全"
        "SUSPICIOUS" -> "注意"
        "DANGEROUS" -> "詐騙/高風險"
        else -> "未知"
    }

    Surface(
        onClick = { onClick(scan.id) },
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121A21),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2979FF).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF2979FF), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    scan.input,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatTimestamp(scan.timestamp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = riskColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(4.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.5f))
            ) {
                Text(
                    riskText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
            }
        }
    }
}

@Composable
private fun EmptyRecentScans(onScanNowClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121A21),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("尚無檢測紀錄", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("立即開始您的第一次安全檢測", color = Color.Gray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onScanNowClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("立即檢測", color = Color.White)
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60 * 1000 -> "剛剛"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} 分鐘前"
        diff < 24 * 60 * 60 * 1000 -> {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val time = sdf.format(Date(timestamp))
            if (isToday(timestamp)) "今天 $time" else "昨天 $time"
        }
        else -> {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

private fun isToday(timestamp: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance()
    cal2.timeInMillis = timestamp
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun NewsPreviewSection(onClick: () -> Unit) {
    val previewNews = NewsRepository.getPreviewNews()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("全球詐騙情資", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            TextButton(onClick = onClick) { Text("進入情報站", color = Color(0xFF448AFF), fontSize = 14.sp) }
        }
        previewNews.forEachIndexed { index, news ->
            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
            Surface(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF121A21),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFF2979FF).copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        Icon(if (news.type == NewsType.TREND) Icons.Default.DataThresholding else Icons.Default.Newspaper, contentDescription = null, tint = Color(0xFF2979FF), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(news.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(news.summary, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun PromotionBanner() {
    val scamPrimary = Color(0xFF2979FF)
    Card(modifier = Modifier.fillMaxWidth().height(100.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF121A21))) {
        Box(modifier = Modifier.fillMaxSize().background(scamPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))) {
            Image(painter = painterResource(R.drawable.shield_banner), contentDescription = null, modifier = Modifier.size(120.dp).align(Alignment.CenterEnd), contentScale = ContentScale.Fit)
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).background(scamPrimary.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = scamPrimary, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("守護不中斷，安全每一步", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Scam Guard 持續保護您的數位生活", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, maxLines = 1)
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
