package com.example.scamdetectorapp.presentation.screens.detection

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.presentation.components.RiskScoreDashboard
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
        "騷擾", "個資蒐集", "企業假冒",
        "銀行信貸騷擾", "可疑電話", "未知詐騙"
    )
    var selectedType by remember { mutableStateOf("") }

    // 根據分數判定風險等級、顏色、圖示與嚴重度短標籤：全頁共用同一語意色
    val isUnknown = result.score == 0 && result.riskLevel == "UNKNOWN"
    data class RiskStatus(
        val text: String,
        val color: Color,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val severity: String
    )
    val status = when {
        isUnknown -> RiskStatus("未知", colorResource(id = R.color.scam_neutral_gray), Icons.Default.Info, "未知")
        result.score > 79 -> RiskStatus("高風險威脅", colorResource(id = R.color.scam_risk_red), Icons.Default.Warning, "高")
        result.score in 40..79 -> RiskStatus("中風險威脅", colorResource(id = R.color.scam_orange), Icons.Default.Warning, "中")
        else -> RiskStatus("低風險威脅", colorResource(id = R.color.scam_safe_green), Icons.Filled.VerifiedUser, "低")
    }

    val statusText = status.text
    val statusColor = status.color
    val statusIcon = status.icon
    val severityLabel = status.severity
    val isLowRisk = !isUnknown && result.score <= 39

    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)
    val textTertiary = colorResource(R.color.scam_text_tertiary)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val componentColor = MaterialTheme.colorScheme.surfaceVariant

    // 新增代碼：定義分享功能邏輯
    // 將檢測結果格式化為文字，並透過系統 Intent 呼叫外部 App (Line, FB, X, IG 等) 進行分享
    val onShare = {
        val shareText = buildString {
            appendLine("【Scam Guard 騙檢測報告 v1.0】")
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 頂部間距：避開系統欄
            Spacer(modifier = Modifier.height(60.dp))

            // 頂部導覽列
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                
                Spacer(Modifier.width(12.dp))
                
                val modeLabel = when (result.mode) {
                    DetectionMode.URL -> "網址"
                    DetectionMode.PHONE -> "電話"
                    DetectionMode.TEXT -> "簡訊"
                    DetectionMode.PRICE -> "購物"
                }
                Text("檢測結果・$modeLabel", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textWhite)

                Spacer(Modifier.weight(1f))
                
                // 右上角分享按鈕
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "分享", tint = textWhite)
                }
            }

            Spacer(Modifier.height(24.dp))

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

            // 新增：查看族譜按鈕 (僅在有提供 callback 時顯示)
            if (onViewGenealogy != null) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onViewGenealogy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("查看號碼關聯族譜", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Details Section
            if (result.riskLevel != "UNKNOWN") {
                Text("詳細資訊", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textWhite)
                Spacer(Modifier.height(16.dp))

                result.detailMap?.let { details ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        details.toList().forEach { pair ->
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
                                Text(
                                    pair.first,
                                    color = textGrey,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    pair.second.toString(),
                                    color = textWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // 分析詳情列表：逐項卡片，左側判斷依據、右側嚴重度標籤，同一語意色貫穿
            if (result.reasons.isNotEmpty()) {
                Text("分析詳情", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textWhite)
                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    result.reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(componentColor, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                reason,
                                color = textWhite,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                severityLabel,
                                color = statusColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // 原始文字內容顯示：收合式次要卡片，視覺上比儀表板安靜
            var contentExpanded by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(componentColor)
                    .clickable { contentExpanded = !contentExpanded }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("原始內容", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textWhite)
                    Icon(
                        if (contentExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = textGrey
                    )
                }
                AnimatedVisibility(visible = contentExpanded) {
                    Text(
                        originalText,
                        color = textGrey,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // 底部操作按鈕：低風險時「回報詐騙」降為中性次要樣式，避免過度強調
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, textTertiary)
                ) {
                    Text("再測一次", color = textWhite, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { if (!isLowRisk) showSheet = true else onBack() },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLowRisk) MaterialTheme.colorScheme.primary else statusColor
                    )
                ) {
                    Text(
                        if (isLowRisk) "完成" else "詐騙回報",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // 底部留白，避免被導覽列遮擋
            Spacer(Modifier.height(140.dp))
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
                                    .background(if (isSelected) statusColor else componentColor)
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
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("送出", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
