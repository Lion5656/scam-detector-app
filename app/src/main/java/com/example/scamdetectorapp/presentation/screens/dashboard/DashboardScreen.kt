package com.example.scamdetectorapp.presentation.screens.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.presentation.model.*
import com.airbnb.lottie.compose.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Typeface

@Composable
fun DashboardScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF06090E))) {
        // 使用 Lottie 全螢幕動態背景
        val composition by rememberLottieComposition(
            LottieCompositionSpec.Asset("Background Full Screen-Night.lottie")
        )
        
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.5f),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            // 標題列
            Text(
                text = "風險數據中心",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            RiskDashboardTab()
        }
    }
}

@Composable
fun RiskDashboardTab() {
    val scamPrimary = Color(0xFF4F7CFF)
    val scamRed = Color(0xFFF05A5A)
    val scamYellow = Color(0xFFF2C94C)
    val surfaceDark = Color(0xFF171E26)
    
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // --- 1. 今日安全摘要 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scamPrimary.copy(alpha = 0.1f))
                .border(0.5.dp, scamPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = scamPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("系統狀態：守護中", color = scamPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text("今日風險趨勢：低", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("目前尚未發現針對您個人的緊急威脅，請保持警覺。", color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. 最近檢測紀錄 ---
        Text(
            text = "最近檢測紀錄", 
            color = Color.White, 
            fontSize = 17.sp, 
            fontWeight = FontWeight.Bold, 
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        val recentEvents = listOf(
            RecentEvent("電話", "0912-334-456", "高風險｜冒充官署", "2小時前", scamRed),
            RecentEvent("網址", "https://bit.ly/secure-...", "中風險｜可疑連結", "昨天", scamYellow),
            RecentEvent("簡訊", "【包裹已配達...】", "安全｜一般物流", "2天前", Color(0xFF00C853))
        )
        
        recentEvents.forEach { event ->
            RecentEventItem(event, surfaceDark)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- 3. 詐騙風險趨勢 (自定義折線圖) ---
        Text(
            text = "本週風險趨勢回顧", 
            color = Color.White, 
            fontSize = 17.sp, 
            fontWeight = FontWeight.Bold, 
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            CyberpunkTrendChart(
                data = listOf(15f, 28f, 22f, 45f, 35f, 52f, 38f),
                labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                primaryColor = scamPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceDark)
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // --- 4. 詐騙類型分佈比例 ---
        Text(
            text = "詐騙類型分佈比例", 
            color = Color.White, 
            fontSize = 17.sp, 
            fontWeight = FontWeight.Bold, 
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceDark)
                    .padding(20.dp)
            ) {
                val scamTypeRatios = listOf(
                    ScamTypeRatio("假投資詐騙", 35, scamRed),
                    ScamTypeRatio("假網絡拍賣", 25, scamYellow),
                    ScamTypeRatio("解除分期付款", 20, scamPrimary),
                    ScamTypeRatio("猜猜我是誰", 20, Color.Gray)
                )
                scamTypeRatios.forEachIndexed { index, ratio ->
                    SimpleProgressBar(ratio)
                    if (index < scamTypeRatios.size - 1) Spacer(Modifier.height(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // 緊急操作
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            EmergencyActionSection()
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

data class RecentEvent(val type: String, val target: String, val result: String, val time: String, val color: Color)

@Composable
fun RecentEventItem(event: RecentEvent, backgroundColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(event.color.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(event.color.copy(alpha = 0.1f), CircleShape)
            )
            Icon(
                imageVector = when(event.type) {
                    "電話" -> Icons.Default.Phone
                    "網址" -> Icons.Default.Public
                    else -> Icons.AutoMirrored.Filled.Chat
                },
                contentDescription = null,
                tint = event.color,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.target, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(event.result, color = event.color.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Text(event.time, color = Color.Gray, fontSize = 11.sp)
    }
}

@Composable
fun SimpleProgressBar(ratio: ScamTypeRatio) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ratio.label, color = Color(0xFFCFD8DC), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("${ratio.percentage}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f))) {
            Box(modifier = Modifier.fillMaxWidth(ratio.percentage / 100f).fillMaxHeight().background(ratio.color))
        }
    }
}

@Composable
fun CyberpunkTrendChart(
    data: List<Float>,
    labels: List<String>,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val transitionProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        transitionProgress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 40.dp.toPx()
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2
        
        val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f) * 1.2f
        val stepX = chartWidth / (data.size - 1)

        // 1. 繪製背景網格
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = padding + (chartHeight / gridLines) * i
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 2. 準備路徑
        val path = Path()
        data.forEachIndexed { index, value ->
            val x = padding + index * stepX
            val y = padding + chartHeight - (value / maxVal) * chartHeight * transitionProgress.value
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        // 3. 繪製填滿漸層
        val fillPath = Path().apply {
            addPath(path)
            lineTo(padding + (data.size - 1) * stepX, padding + chartHeight)
            lineTo(padding, padding + chartHeight)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent),
                startY = padding,
                endY = padding + chartHeight
            )
        )

        // 4. 繪製主線條 (帶有發光感)
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // 5. 繪製資料點
        data.forEachIndexed { index, value ->
            val x = padding + index * stepX
            val y = padding + chartHeight - (value / maxVal) * chartHeight * transitionProgress.value
            
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = 6.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = primaryColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // 6. 繪製標籤
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 10.sp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            
            labels.forEachIndexed { index, label ->
                val x = padding + index * stepX
                val y = height - 10.dp.toPx()
                canvas.nativeCanvas.drawText(label, x, y, paint)
            }
        }
    }
}

@Composable
fun EmergencyActionSection() {
    val context = LocalContext.current
    Button(
        onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:165"))) },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
        Spacer(modifier = Modifier.width(12.dp))
        Text("緊急撥打：165 專線", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}
