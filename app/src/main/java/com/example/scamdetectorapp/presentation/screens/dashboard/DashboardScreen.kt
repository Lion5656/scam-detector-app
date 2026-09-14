package com.example.scamdetectorapp.presentation.screens.dashboard

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.presentation.model.*
import com.airbnb.lottie.compose.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.toArgb
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
    
    // 計算標題列的透明度：隨滑動距離快速淡出
    val titleAlpha by remember {
        derivedStateOf {
            val progress = (scrollState.value / 100f).coerceIn(0f, 1f)
            1f - progress
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

        // --- 詐騙電話種類統計 ---
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
                stats.phoneTypeDistribution.forEachIndexed { index, ratio ->
                    SimpleProgressBar(ratio)
                    if (index < stats.phoneTypeDistribution.size - 1) {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
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
    // 固定的日期顏色
    val dayLabelColor = Color(0xFFBCD4E7)

    // 風險等級顏色 (用於長條圖)
    val riskHighColor = Color(0xFFF05A5A)   // 高風險 - 紅
    val riskMediumColor = Color(0xFFF2C94C) // 中風險 - 黃
    val riskLowColor = Color(0xFF00C853)    // 低風險 - 綠

    val transitionProgress = remember { Animatable(0f) }
    LaunchedEffect(trendData) {
        transitionProgress.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("本週風險筆數分佈", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            
            // 右上角圖例
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChartLegendItem("高 ", riskHighColor)
                Spacer(Modifier.width(8.dp))
                ChartLegendItem("中 ", riskMediumColor)
                Spacer(Modifier.width(8.dp))
                ChartLegendItem("低 ", riskLowColor)
            }
        }

        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val width = size.width
            val height = size.height
            val paddingX = 20.dp.toPx()
            val paddingY = 20.dp.toPx()
            val chartWidth = width - paddingX * 2
            val chartHeight = height - paddingY * 2

            // 計算每日總筆數
            val dailyTotals = trendData.labels.indices.map { i ->
                ((trendData.lowRisk.getOrNull(i) ?: 0f) +
                 (trendData.mediumRisk.getOrNull(i) ?: 0f) +
                 (trendData.highRisk.getOrNull(i) ?: 0f)).toInt()
            }
            // 決定最大縮放比例，預設至少顯示到 5
            val maxTotal = (dailyTotals.maxOrNull() ?: 1).coerceAtLeast(5).toFloat() * 1.2f
            
            val barWidth = (chartWidth / 7) * 0.5f
            val spacing = chartWidth / 7

            // 1. 繪製背景水平線
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = paddingY + (chartHeight / gridLines) * i
                drawLine(
                    color = Color.White.copy(alpha = 0.05f),
                    start = Offset(paddingX, y),
                    end = Offset(width - paddingX, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 2. 準備文字畫筆
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            // 3. 繪製長條圖、中心數字、底部日期
            dailyTotals.forEachIndexed { index, total ->
                val centerX = paddingX + index * spacing + spacing / 2
                val barHeightValue = (total / maxTotal) * chartHeight * transitionProgress.value
                val currentY = paddingY + chartHeight
                
                val highCount = trendData.highRisk.getOrNull(index) ?: 0f
                val mediumCount = trendData.mediumRisk.getOrNull(index) ?: 0f
                val lowCount = trendData.lowRisk.getOrNull(index) ?: 0f

                // 決定長條顏色：出現過高風險(紅色)、出現過中風險(黃色)、只出現過低風險(綠色)
                val color = when {
                    highCount > 0f -> riskHighColor
                    mediumCount > 0f -> riskMediumColor
                    lowCount > 0f -> riskLowColor
                    else -> Color.Transparent
                }

                if (total > 0) {
                    // 繪製一體化長條圖
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(centerX - barWidth / 2, currentY - barHeightValue),
                        size = Size(barWidth, barHeightValue),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // 在長條圖【正中央】顯示數字
                    if (transitionProgress.value > 0.8f && barHeightValue > 12.dp.toPx()) {
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawText(
                                total.toString(),
                                centerX,
                                currentY - barHeightValue / 2 + 4.dp.toPx(), // 垂直置中
                                textPaint
                            )
                        }
                    }
                }

                // 繪製底部日期文字 (彩虹色且單一顏色顯示)
                val label = trendData.labels.getOrNull(index) ?: ""
                val labelPaint = android.graphics.Paint().apply {
                    this.color = dayLabelColor.toArgb()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(label, centerX, height, labelPaint)
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

