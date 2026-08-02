package com.example.scamdetectorapp.presentation.screens.detection

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.presentation.model.ScanUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneResultScreen(originalText: String, result: ScanUiModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val fraudTypes = listOf(
        "騷擾", "個資蒐集", "企業假冒",
        "銀行信貸騷擾", "可疑電話", "未知詐騙"
    )
    var selectedType by remember { mutableStateOf("") }

    val statusData = when {
        result.score > 79 -> Triple("高風險威脅", colorResource(id = R.color.scam_risk_red), Icons.Default.Warning)
        result.score in 40..79 -> Triple("中風險威脅", colorResource(id = R.color.scam_orange), Icons.Default.Warning)
        result.score in 0..39 -> Triple("低風險威脅", colorResource(id = R.color.scam_safe_green), Icons.Filled.VerifiedUser)
        else -> Triple("查無資料", colorResource(id = R.color.scam_neutral_gray), Icons.Default.Search)
    }

    val statusText = statusData.first
    val statusColor = statusData.second
    val statusIcon = statusData.third

    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Nav
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textWhite)
            }
            Spacer(Modifier.width(8.dp))
            Text("電話查詢結果", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textWhite)
        }

        Spacer(Modifier.height(24.dp))

        // Main Risk Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(originalText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textWhite)
                Text(statusText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = statusColor)
                
                if (result.score > 0) {
                    Spacer(Modifier.height(16.dp))
                    Text("風險指數: ${result.score}%", color = textGrey)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = (result.score / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(statusColor, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Details Section
        Text("詳細資訊", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textWhite)
        Spacer(Modifier.height(16.dp))

        result.detailMap?.let { details ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val detailList = details.toList()
                    detailList.forEachIndexed { index, pair ->
                        DetailItem(label = pair.first, value = pair.second.toString(), color = textWhite, labelColor = textGrey)
                        if (index < detailList.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = textWhite.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        } ?: run {
            Text("暫無詳細資訊", color = textGrey, fontSize = 14.sp)
        }

        Spacer(Modifier.height(24.dp))

        // Analysis reasons
        Text("分析摘要", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textWhite)
        Spacer(Modifier.height(12.dp))
        result.reasons.forEach { reason ->
            Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(reason, color = textGrey, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(32.dp))

        // 底部操作按鈕
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = surfaceColor)
            ) {
                Text("再測一次", color = textWhite)
            }

            Button(
                onClick = { if (result.score > 0) showSheet = true else onBack() },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = statusColor)
            ) {
                Text(if(result.score > 0) "回報詐騙" else "完成", color = Color.White)
            }
        }

        // 回報詐騙的底部彈窗 (BottomSheet)
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = surfaceColor,
                dragHandle = { BottomSheetDefaults.DragHandle(color = textGrey) },
                modifier = Modifier.fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "你遇到的詐騙類型是?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(fraudTypes) { type ->
                            val isSelected = selectedType == type
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .aspectRatio(1.2f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) statusColor else backgroundColor)
                                    .clickable { selectedType = type }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = type,
                                    color = if (isSelected) Color.White else textWhite,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedType.isNotBlank()){
                                Toast.makeText(context, "送出成功", Toast.LENGTH_SHORT).show()
                                showSheet = false
                                onBack()
                            } else {
                                Toast.makeText(context, "請先選擇詐騙類型", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = statusColor),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("送出", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, color: Color, labelColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = labelColor, fontSize = 14.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
