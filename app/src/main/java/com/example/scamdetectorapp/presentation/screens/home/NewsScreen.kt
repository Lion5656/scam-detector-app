package com.example.scamdetectorapp.presentation.screens.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.data.repository.NewsItem
import com.example.scamdetectorapp.data.repository.NewsRepository

/**
 * 新分頁，防詐資訊新聞列表螢幕
 * 展示近兩週真實發生的熱門反詐騙案例與查核資訊。
 */
@Composable
fun NewsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val textWhite = MaterialTheme.colorScheme.onBackground

    // 從 NewsRepository 獲取真實數據
    val newsList = NewsRepository.newsList

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 頂部間距：避開系統欄並留出更多空間
        Spacer(modifier = Modifier.height(80.dp))

        // --- 頂部標題欄 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = textWhite)
            }
            Text("防詐資訊看板", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textWhite)
        }

        // --- 滾動列表 ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 140.dp), // 增加底部邊距
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(newsList) { news ->
                NewsCard(news = news, onOpenUrl = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // 處理 URL 異常或找不到可處理 Intent 的 Activity（如無瀏覽器）
                        Toast.makeText(context, "無法開啟連結", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }
}

/**
 * 新聞卡片組件
 */
@Composable
fun NewsCard(news: NewsItem, onOpenUrl: () -> Unit) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = news.source,
                    fontSize = 12.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = news.date,
                    fontSize = 12.sp,
                    color = textGrey
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = news.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = news.summary,
                fontSize = 14.sp,
                color = textGrey,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onOpenUrl,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor.copy(alpha = 0.1f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.OpenInNew, 
                    contentDescription = null, 
                    modifier = Modifier.size(14.dp),
                    tint = primaryColor
                )
                Spacer(Modifier.width(4.dp))
                Text("查看全文", color = primaryColor, fontSize = 12.sp)
            }
        }
    }
}
