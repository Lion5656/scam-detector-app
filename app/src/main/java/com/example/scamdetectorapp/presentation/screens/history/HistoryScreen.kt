package com.example.scamdetectorapp.presentation.screens.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.data.local.HistoryEntity
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.scamdetectorapp.ui.theme.AppBackgroundBrush
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(onBack: () -> Unit, viewModel: MainViewModel) {
    val textWhite = Color.White
    val textGrey = colorResource(R.color.scam_text_grey)
    val panelColor = Color(0xFF1E2630)
    
    // 使用資料庫中的真實數據
    val allItems by viewModel.allHistory.collectAsStateWithLifecycle()

    val filters = listOf("全部", "低風險", "中風險", "高風險", "未知")
    var selectedFilter by remember { mutableStateOf("全部") }

    val filteredItems = remember(selectedFilter, allItems) {
        when (selectedFilter) {
            "低風險" -> allItems.filter { it.riskLevel == "LOW" || it.riskLevel == "SAFE" }
            "中風險" -> allItems.filter { it.riskLevel == "MEDIUM" }
            "高風險" -> allItems.filter { it.riskLevel == "HIGH" }
            "未知" -> allItems.filter { it.riskLevel == "UNKNOWN" }
            else -> allItems
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundBrush)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 頂部間距
            Spacer(modifier = Modifier.height(60.dp))

            // 頂部標題欄 (統一風格)
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
                        tint = textWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "檢測紀錄",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textWhite
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 過濾菜單列
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) Color(0xFFA78BFA).copy(alpha = 0.2f) else panelColor,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFA78BFA)) else null
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color(0xFFA78BFA) else textGrey,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 紀錄列表
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = Color(0xFF666279).copy(alpha = 0.5f), // 使用紫色調，增加亮度
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暫無紀錄",
                            color = textWhite.copy(alpha = 0.5f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // 進一步縮小列表間距
                ) {
                    items(filteredItems) { item ->
                        HistoryCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: HistoryEntity
) {
    val riskColor = when (item.riskLevel) {
        "HIGH" -> Color(0xFFF60018)    // 玫瑰深紅
        "MEDIUM" -> Color(0xFFFFA905)    // 琥珀暗黃
        "LOW", "SAFE" -> Color(0xFF4ADE80) // 翡翠深綠
        else -> Color(0xFF64748B)      // 石板冷藍
    }

    val dateStr = remember(item.timestamp) {
        val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("GMT+8") // 指定 UTC+8 時區顯示
        }
        dateFormat.format(Date(item.timestamp))
    }

    Surface(
        color = Color(0xFF1E2630), // 改為單一面板色，拿掉漸層
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 最左邊：圓圈圖示表示種類
            val typeIcon = when (item.type) {
                "URL" -> Icons.Default.Link
                "電話" -> Icons.Default.Phone
                "圖片" -> Icons.Default.Image
                else -> Icons.Default.TextFields
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(riskColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (item.type == "URL") {
                    Icon(
                        painter = painterResource(id = R.drawable.link_fill),
                        contentDescription = "URL",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = item.type,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 中間：內容與日期
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.content.length > 25) item.content.take(25) + "..." else item.content,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}
