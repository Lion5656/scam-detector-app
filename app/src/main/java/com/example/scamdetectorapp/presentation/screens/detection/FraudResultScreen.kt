package com.example.scamdetectorapp.presentation.screens.detection

import android.content.Intent
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
fun FraudResultScreen(
    originalText: String, 
    result: ScanUiModel, 
    onBack: () -> Unit,
    onViewGenealogy: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val fraudTypes = listOf(
        "釣魚簡訊", "假投資", "網路購物", "假買家",
        "交友詐財", "偽造中獎", "求職陷阱", "假銀行貸款",
        "騙取金融帳戶", "假廣告", "假冒檢警", "遊戲詐騙"
    )
    var selectedType by remember { mutableStateOf("") }

    // 根據分數判定風險等級、顏色與圖示
    val statusData = when {
        result.score > 75 -> Triple("高風險威脅", colorResource(id = R.color.scam_risk_red), Icons.Default.Warning)
        result.score in 41..74 -> Triple("中風險威脅", colorResource(id = R.color.scam_orange), Icons.Default.Warning)
        result.score in 1..40 -> Triple("低風險威脅", colorResource(id = R.color.scam_safe_green), Icons.Filled.VerifiedUser)
        else -> Triple("查無資料", colorResource(id = R.color.scam_neutral_gray), Icons.Default.Search)
    }

    val statusText = statusData.first
    val statusColor = statusData.second
    val statusIcon = statusData.third

    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background

    // 新增代碼：定義分享功能邏輯
    // 將檢測結果格式化為文字，並透過系統 Intent 呼叫外部 App (Line, FB, X, IG 等) 進行分享
    val onShare = {
        val shareText = buildString {
            appendLine("【Scam Guard 詐騙檢測報告 v1.0】")
            appendLine("\n原始內容：")
            appendLine(originalText)
            if (result.reasons.isNotEmpty()) {
                appendLine("\n分析詳情：")
                result.reasons.forEach { appendLine("• $it") }
            }
            appendLine("\n風險指數：${result.score}%")
            appendLine("\n#防詐騙 #ScamGuard #安全守護")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "分享檢測結果"))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 頂部導覽列
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textWhite)
                }
                Spacer(Modifier.width(8.dp))
                Text("檢測結果", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textWhite)
                
                Spacer(Modifier.weight(1f))
                
                // 新增代碼：右上角分享按鈕
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "分享", tint = textWhite)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 指數顯示卡片
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
                    Text(statusText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = statusColor)
                    Spacer(Modifier.height(8.dp))

                    Text(if (result.score > 0) "風險指數" else "", color = textGrey, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))

                    if (result.score >= 0) {
                        // 新增代碼：自定義連貫進度條
                        // 使用 Box 堆疊取代 LinearProgressIndicator，解決 Material 3 預設元件的斷裂感問題
                        val barShape = RoundedCornerShape(4.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(statusColor.copy(alpha = 0.15f), barShape) // 進度條底色
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (result.score / 100f).coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(statusColor, barShape) // 實際進度顏色
                            )
                        }
                    }

                    if (result.score >= 0) {
                        Text("${result.score}%", color = textWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }

            // 新增：查看族譜按鈕 (僅在有提供 callback 時顯示)
            if (onViewGenealogy != null) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onViewGenealogy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF2979FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF448AFF), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("查看號碼關聯族譜", color = Color(0xFF448AFF), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 分析詳情列表
            Text("分析詳情", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textWhite)
            Spacer(Modifier.height(16.dp))

            result.reasons.forEach { reason ->
                Row(modifier = Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(reason, color = textGrey, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 原始文字內容顯示
            Text("原始內容", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textWhite)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(originalText, color = textGrey, fontSize = 14.sp)
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
