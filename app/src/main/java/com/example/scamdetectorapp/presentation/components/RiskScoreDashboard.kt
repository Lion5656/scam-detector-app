package com.example.scamdetectorapp.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 風險分數儀表板：弧形量表（大字級分數 + 說明文字置中）→ 風險徽章，
 * 全部共用同一個語意色，讓使用者不需要讀文字就能靠顏色判斷風險等級。
 */
@Composable
fun RiskScoreDashboard(
    score: Int,
    caption: String,
    badgeText: String,
    badgeIcon: ImageVector,
    color: Color,
    trackColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    maxValue: Int = 100,
    gaugeSize: Dp = 180.dp,
    strokeWidth: Dp = 14.dp,
    useGradient: Boolean = true,
) {
    val animatedScore = remember { Animatable(0f) }

    LaunchedEffect(score) {
        animatedScore.animateTo(
            targetValue = score.toFloat().coerceIn(0f, maxValue.toFloat()),
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(gaugeSize), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(gaugeSize)) {
                val strokeWidthPx = strokeWidth.toPx()
                val diameter = size.minDimension - strokeWidthPx
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)
                val startAngle = 135f
                val sweepAngle = 270f

                drawArc(
                    color = trackColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                val progressSweep = sweepAngle * (animatedScore.value / maxValue.toFloat()).coerceIn(0f, 1f)
                if (progressSweep > 0f) {
                    val brush = if (useGradient) {
                        val centerX = topLeft.x + diameter / 2f
                        val centerY = topLeft.y + diameter / 2f
                        val radius = diameter / 2f
                        val startRad = Math.toRadians(startAngle.toDouble())
                        val endRad = Math.toRadians((startAngle + progressSweep).toDouble())
                        val gradientStart = Offset(
                            centerX + radius * cos(startRad).toFloat(),
                            centerY + radius * sin(startRad).toFloat()
                        )
                        val gradientEnd = Offset(
                            centerX + radius * cos(endRad).toFloat(),
                            centerY + radius * sin(endRad).toFloat()
                        )
                        Brush.linearGradient(
                            colors = listOf(color.lighten(0.2f), color),
                            start = gradientStart,
                            end = gradientEnd
                        )
                    } else {
                        Brush.linearGradient(colors = listOf(color, color))
                    }
                    drawArc(
                        brush = brush,
                        startAngle = startAngle,
                        sweepAngle = progressSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = animatedScore.value.roundToInt().toString(),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "$caption / $maxValue",
                    fontSize = 12.sp,
                    color = labelColor
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(badgeIcon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(badgeText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

/** 將顏色與白色混合，產生同色相的較淺色調，用於量表的漸層起點。 */
private fun Color.lighten(fraction: Float): Color = Color(
    red = red + ((1f - red) * fraction),
    green = green + ((1f - green) * fraction),
    blue = blue + ((1f - blue) * fraction),
    alpha = alpha
)
