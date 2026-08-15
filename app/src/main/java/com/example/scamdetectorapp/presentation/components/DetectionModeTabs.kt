package com.example.scamdetectorapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.domain.model.DetectionMode

private fun labelFor(mode: DetectionMode) = when (mode) {
    DetectionMode.TEXT -> "簡訊"
    DetectionMode.PHONE -> "電話"
    DetectionMode.URL -> "URL"
    DetectionMode.PRICE -> "購物"
}

/**
 * 頂部膠囊分頁：切換簡訊／電話／URL／購物檢測四種偵測模式。
 * 純文字標籤，不搭配圖示；選中項目統一用系統藍凸顯。
 */
@Composable
fun DetectionModeTabs(
    selected: DetectionMode,
    onSelect: (DetectionMode) -> Unit,
    modifier: Modifier = Modifier,
    modes: List<DetectionMode> = listOf(DetectionMode.TEXT, DetectionMode.PHONE, DetectionMode.URL, DetectionMode.PRICE)
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        modes.forEach { mode ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) primaryColor.copy(alpha = 0.18f) else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    labelFor(mode),
                    color = if (isSelected) textWhite else textGrey,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
