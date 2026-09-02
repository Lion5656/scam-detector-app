package com.example.scamdetectorapp.presentation.screens.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.presentation.model.*
import com.airbnb.lottie.compose.*
import androidx.compose.ui.layout.ContentScale
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.painterResource
import com.example.scamdetectorapp.R

@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel
) {
    val scrollState = rememberScrollState()
    
    // 計算標題列的透明度：隨滑動距離淡出
    val titleAlpha by remember {
        derivedStateOf {
            val progress = (scrollState.value / 400f).coerceIn(0f, 1f)
            1f - (progress * progress * progress)
        }
    }

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

        RiskDashboardTab(viewModel, scrollState)

        if (titleAlpha > 0f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = titleAlpha }
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back_less_than),
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "風險數據中心",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun RiskDashboardTab(viewModel: MainViewModel, scrollState: ScrollState) {
    val scamPrimary = Color(0xFF4F7CFF)
    val scamRed = Color(0xFFF05A5A)
    val scamYellow = Color(0xFFF2C94C)
    val surfaceDark = Color(0xFF171E26)

    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // 為了讓內容初始位置在標題下方，增加與標題列等高的間距
        Spacer(modifier = Modifier.height(160.dp))

        // --- 今日安全摘要 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scamPrimary.copy(alpha = 0.1f))
                .border(0.5.dp, scamPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                val riskTrend = if (stats.highRiskMessages > 0) "高" else "低"
                val riskColor = if (stats.highRiskMessages > 0) scamRed else Color.White
                Text("今日風險趨勢：$riskTrend", color = riskColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                val summaryText = if (stats.highRiskMessages > 0) {
                    "今日發現 ${stats.highRiskMessages} 筆高風險紀錄，建議立即檢查並封鎖。"
                } else {
                    "目前尚未發現針對您個人的緊急威脅，請保持警覺。"
                }
                Text(summaryText, color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- 詐騙風險趨勢 ---
        Text(
            text = "本週風險趨勢回顧",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            MultiRiskTrendChart(
                trendData = stats.trendData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceDark)
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ---  詐騙類型分佈比例 ---
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
                stats.typeDistribution.forEachIndexed { index, ratio ->
                    SimpleProgressBar(ratio)
                    if (index < stats.typeDistribution.size - 1) Spacer(Modifier.height(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // --- 5. 詐騙電話種類統計 ---
        Text(
            text = "詐騙電話種類統計",
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
                if (stats.phoneTypeDistribution.all { it.percentage == 0 }) {
                    Text("暫無高風險電話統計資料", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    stats.phoneTypeDistribution.forEachIndexed { index, ratio ->
                        SimpleProgressBar(ratio)
                        if (index < stats.phoneTypeDistribution.size - 1) Spacer(Modifier.height(16.dp))
                    }
                }
            }
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
fun MultiRiskTrendChart(
    trendData: RiskTrendData,
    modifier: Modifier = Modifier
) {
    val lowColor = Color(0xFF00C853)
    val mediumColor = Color(0xFFF2C94C)
    val highColor = Color(0xFFF05A5A)

    val transitionProgress = remember { Animatable(0f) }
    LaunchedEffect(trendData) {
        transitionProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier) {
        // 圖例
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("每日風險分佈", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChartLegendItem("高", highColor)
                Spacer(Modifier.width(8.dp))
                ChartLegendItem("中", mediumColor)
                Spacer(Modifier.width(8.dp))
                ChartLegendItem("低", lowColor)
            }
        }

        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val width = size.width
            val height = size.height
            val paddingX = 40.dp.toPx()
            val paddingY = 20.dp.toPx()
            val chartWidth = width - paddingX * 2
            val chartHeight = height - paddingY * 2

            // 計算最大總量以決定縮放比例
            val dayTotals = trendData.labels.indices.map { i ->
                trendData.lowRisk.getOrElse(i) { 0f } +
                trendData.mediumRisk.getOrElse(i) { 0f } +
                trendData.highRisk.getOrElse(i) { 0f }
            }
            val maxTotal = (dayTotals.maxOrNull() ?: 1f).coerceAtLeast(5f) * 1.1f
            
            val barWidth = (chartWidth / trendData.labels.size) * 0.6f
            val spacing = chartWidth / trendData.labels.size

            // 1. 繪製背景水平網格與數值標籤
            val gridLines = 4
            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 8.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            for (i in 0..gridLines) {
                val y = paddingY + (chartHeight / gridLines) * i
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(paddingX, y),
                    end = Offset(width - paddingX, y),
                    strokeWidth = 1.dp.toPx()
                )

                // 繪製 Y 軸數值標籤 (頂、中、底)
                if (i % 2 == 0) {
                    val value = (maxTotal * (gridLines - i) / gridLines).toInt()
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            value.toString(),
                            paddingX - 8.dp.toPx(),
                            y + 3.dp.toPx(), // 垂直微調對齊線條
                            labelPaint
                        )
                    }
                }
            }

            // 2. 繪製堆疊柱狀圖
            val valuePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            trendData.labels.forEachIndexed { index, label ->
                val centerX = paddingX + index * spacing + spacing / 2
                val lowRaw = trendData.lowRisk.getOrElse(index) { 0f }
                val medRaw = trendData.mediumRisk.getOrElse(index) { 0f }
                val highRaw = trendData.highRisk.getOrElse(index) { 0f }
                
                val lowVal = lowRaw * transitionProgress.value
                val medVal = medRaw * transitionProgress.value
                val highVal = highRaw * transitionProgress.value

                var currentY = paddingY + chartHeight
                val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                val gap = 2.dp.toPx()

                // --- 低風險 (底部) ---
                if (lowVal > 0) {
                    val segHeight = (lowVal / maxTotal) * chartHeight
                    drawRoundRect(
                        color = lowColor.copy(alpha = 0.8f),
                        topLeft = Offset(centerX - barWidth / 2, currentY - segHeight),
                        size = Size(barWidth, segHeight),
                        cornerRadius = cornerRadius
                    )
                    // 顯示數字 (當動畫完成且高度足夠時)
                    if (transitionProgress.value > 0.8f && segHeight > 12.dp.toPx()) {
                        drawIntoCanvas { it.nativeCanvas.drawText(lowRaw.toInt().toString(), centerX, currentY - segHeight / 2 + 4.dp.toPx(), valuePaint) }
                    }
                    currentY -= (segHeight + gap)
                }

                // --- 中風險 ---
                if (medVal > 0) {
                    val segHeight = (medVal / maxTotal) * chartHeight
                    drawRoundRect(
                        color = mediumColor.copy(alpha = 0.8f),
                        topLeft = Offset(centerX - barWidth / 2, currentY - segHeight),
                        size = Size(barWidth, segHeight),
                        cornerRadius = cornerRadius
                    )
                    if (transitionProgress.value > 0.8f && segHeight > 12.dp.toPx()) {
                        drawIntoCanvas { it.nativeCanvas.drawText(medRaw.toInt().toString(), centerX, currentY - segHeight / 2 + 4.dp.toPx(), valuePaint) }
                    }
                    currentY -= (segHeight + gap)
                }

                // --- 高風險 (頂部) ---
                if (highVal > 0) {
                    val segHeight = (highVal / maxTotal) * chartHeight
                    drawRoundRect(
                        color = highColor.copy(alpha = 0.9f),
                        topLeft = Offset(centerX - barWidth / 2, currentY - segHeight),
                        size = Size(barWidth, segHeight),
                        cornerRadius = cornerRadius
                    )
                    if (transitionProgress.value > 0.8f && segHeight > 12.dp.toPx()) {
                        drawIntoCanvas { it.nativeCanvas.drawText(highRaw.toInt().toString(), centerX, currentY - segHeight / 2 + 4.dp.toPx(), valuePaint) }
                    }
                }

                // 3. 繪製 X 軸標籤
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    canvas.nativeCanvas.drawText(label, centerX, height, paint)
                }
            }
        }
    }
}

@Composable
fun ChartLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

