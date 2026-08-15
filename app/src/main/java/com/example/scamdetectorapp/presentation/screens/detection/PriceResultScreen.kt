package com.example.scamdetectorapp.presentation.screens.detection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.presentation.components.RiskScoreDashboard
import com.example.scamdetectorapp.presentation.model.ScanUiModel

@Composable
fun PriceResultScreen(
    result: ScanUiModel,
    onBack: () -> Unit
) {
    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)
    // 購物檢測維持原本的元件色，不隨其他頁面的新配色調整
    val componentColor = Color(0xFF1E1E1C)

    // 根據分數判定風險等級、顏色與圖示：與其他結果頁共用同一套語意色邏輯
    val isUnknown = result.score == 0 && result.riskLevel == "UNKNOWN"
    val statusData = when {
        isUnknown -> Triple("未知", colorResource(id = R.color.scam_neutral_gray), Icons.Default.Info)
        result.score > 79 -> Triple("高風險威脅", colorResource(id = R.color.scam_risk_red), Icons.Default.Warning)
        result.score in 40..79 -> Triple("中風險威脅", colorResource(id = R.color.scam_orange), Icons.Default.Warning)
        else -> Triple("低風險威脅", colorResource(id = R.color.scam_safe_green), Icons.Filled.VerifiedUser)
    }

    val statusText = statusData.first
    val statusColor = statusData.second
    val statusIcon = statusData.third

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 頂部導覽列
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textWhite)
            }
            Spacer(Modifier.width(8.dp))
            Text("檢測結果・購物", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textWhite)
        }

        Spacer(Modifier.height(8.dp))

        // 風險儀表板：分數 → 說明文字 → 風險徽章 → 語意色進度條，直接置於頁面背景上
        RiskScoreDashboard(
            score = result.score,
            caption = "風險分數",
            badgeText = statusText,
            badgeIcon = statusIcon,
            color = statusColor,
            trackColor = statusColor.copy(alpha = 0.15f),
            labelColor = textGrey,
            useGradient = !isUnknown,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Spacer(Modifier.height(24.dp))

        // 商品資訊標題
        Text("商品資訊", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textWhite)
        Spacer(Modifier.height(16.dp))

        // 資訊列表
        val details = result.detailMap ?: emptyMap()

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailPill("商品名稱", details["商品名稱"]?.toString() ?: "未知", statusColor, textWhite, textGrey, componentColor)
            
            // 商品狀態自定義標籤 (Badge 樣式)
            val rawStatus = details["商品狀態"]?.toString()?.lowercase() ?: "unknown"
            val (statusLabel, badgeBaseColor) = when (rawStatus) {
                "new" -> "全新" to Color(0xFF2E7D32)    // 綠色表示全新
                "used" -> "二手" to Color(0xFFFF9800)   // 橘色表示二手
                else -> "未知" to Color(0xFF757575)      // 灰色表示未知
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(componentColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(Modifier.width(10.dp))
                Text("商品狀態", color = textGrey, fontSize = 14.sp, modifier = Modifier.weight(1f))
                
                Surface(
                    color = badgeBaseColor.copy(alpha = 0.8f), // 背景微透明
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // 價格資訊與位置條：保留原本的商品價格對比圖，僅套用新配色
            val listedPriceStr = details["商品價格"]?.toString()?.filter { it.isDigit() } ?: "0"
            val marketPriceStr = details["市場價格"]?.toString()?.filter { it.isDigit() } ?: "0"
            val listedPrice = listedPriceStr.toDoubleOrNull() ?: 0.0
            val marketPrice = marketPriceStr.toDoubleOrNull() ?: 0.0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(componentColor, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                PriceSection(
                    listedPriceLabel = details["商品價格"]?.toString() ?: "NT$0",
                    marketPriceLabel = details["市場價格"]?.toString() ?: "NT$0",
                    listedPrice = listedPrice,
                    marketPrice = marketPrice,
                    statusColor = statusColor
                )
            }

            DetailPill("賣家名稱", details["賣家名稱"]?.toString() ?: "未知", statusColor, textWhite, textGrey, componentColor)

            // 結果說明：收合式次要卡片同款風格，視覺上比儀表板安靜
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(componentColor)
                    .padding(16.dp)
            ) {
                Text("結果說明", color = textGrey, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    details["結果說明"]?.toString() ?: "分析完成，請參考上述價格資訊。",
                    color = textWhite,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // 再次檢測按鈕
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("再次檢測", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DetailPill(
    label: String,
    value: String,
    dotColor: Color,
    textColor: Color,
    labelColor: Color,
    backgroundColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = labelColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PriceSection(
    listedPriceLabel: String,
    marketPriceLabel: String,
    listedPrice: Double,
    marketPrice: Double,
    statusColor: Color
) {
    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)

    // 市場價格基準點固定在 75% 的位置
    val marketRatio = 0.75f
    // 計算商品價格相對於市場價格的比例
    val productRatio = if (marketPrice > 0) {
        (marketRatio * (listedPrice / marketPrice)).toFloat().coerceIn(0f, 1f)
    } else 0f

    // 動態推進動畫
    val animatedRatio = remember { Animatable(0f) }
    LaunchedEffect(productRatio) {
        animatedRatio.animateTo(
            targetValue = productRatio,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    val triangleShape = remember {
        GenericShape { size, _ ->
            moveTo(size.width / 2f, 0f) // 頂部尖端 (向上指)
            lineTo(0f, size.height) // 左下
            lineTo(size.width, size.height) // 右下
            close()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("商品價格", color = textGrey, fontSize = 14.sp)
                Text(listedPriceLabel, color = statusColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("市場價格", color = textGrey, fontSize = 14.sp)
                Text(marketPriceLabel, color = textWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(32.dp))

        // 價格位置條容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp) // 增加高度以容納更大的三角形
        ) {
            // 背景條 (中線)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(textGrey.copy(alpha = 0.2f), CircleShape)
                    .align(Alignment.Center)
            )

            // 市場價格標記 (圓點)
            Box(
                modifier = Modifier
                    .fillMaxWidth(marketRatio)
                    .align(Alignment.CenterStart)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(textWhite, CircleShape)
                        .align(Alignment.CenterEnd)
                )
            }

            // 商品價格標記 (正三角形從下方往上指)
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedRatio.value)
                    .align(Alignment.CenterStart)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    // 頂部短線 (增加視覺指引，跨過中線)
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(8.dp)
                            .background(statusColor)
                    )
                    Spacer(Modifier.height(8.dp))
                    // 正三角形 (向上指)
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 12.dp) // 從 12x10 放大至 16x12
                            .background(statusColor, triangleShape)
                    )
                }
            }
        }

        // 位置條下方標籤
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("偏低", color = textGrey, fontSize = 12.sp)
            Box(modifier = Modifier.fillMaxWidth(marketRatio)) {
                Text("市場價格", color = textWhite, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterEnd))
            }
        }

        // 價格差異百分比
        if (marketPrice > 0) {
            val diffPercent = (((listedPrice - marketPrice) / marketPrice) * 100).toInt()
            val absDiff = kotlin.math.abs(diffPercent)
            val resultText = when {
                diffPercent < 0 -> "低於目前市場價格約 $absDiff%"
                diffPercent > 0 -> "高於目前市場價格約 $absDiff%"
                else -> "與市場價格持平"
            }
            
            Spacer(Modifier.height(16.dp))
            Text(
                resultText,
                color = if (diffPercent <= 0) statusColor else colorResource(R.color.scam_risk_red),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
