package com.example.scamdetectorapp.presentation.screens.detection

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.presentation.model.GenealogyNode
import com.example.scamdetectorapp.presentation.model.PhoneGenealogyData
import androidx.compose.ui.text.drawText
import androidx.compose.ui.tooling.preview.Preview
import com.example.scamdetectorapp.BuildConfig
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// --- 優化點 1：文字測量快取結構 ---
private data class GenealogyTextCache(
    val rootText: TextLayoutResult,
    val nodeLabels: List<TextLayoutResult>,
    val relationLabels: List<TextLayoutResult>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneGenealogyScreen(phoneNumber: String, onBack: () -> Unit) {
    var currentRoot by remember { mutableStateOf(phoneNumber) }
    var selectedNode by remember { mutableStateOf<GenealogyNode?>(null) }
    var showMetricInfo by remember { mutableStateOf<String?>(null) }

    // --- 優化點 2：模擬數據封裝與 DEBUG 模式保護 ---
    val genealogyData = remember(currentRoot) {
        if (BuildConfig.DEBUG) {
            val scamTypes = listOf("假投資詐騙 ", "假網絡拍賣 ", "解除分期付款 / 假客服", "假愛情交友 ", "假冒機構 / 假檢警", "猜猜我是誰 / 盜用帳號", "假求職 / 騙取帳戶")
            PhoneGenealogyData(
                rootNumber = currentRoot,
                riskScore = 0.92f,
                associationScore = 0.84f,
                clusterId = scamTypes.random(),
                relatedNodes = listOf(
                    GenealogyNode(1, "0912-334-456", "相似號碼", 0.95f, "2026/06/26", "此號碼與主號碼由同一批次門號申請，具備極高同質性。"),
                    GenealogyNode(2, "02-2310-9981", "頻繁撥打", 0.75f, "2026/06/25", "該號碼近期在同一時段內，與主號碼具備重疊的受害者名單。"),
                    GenealogyNode(3, "0905-112-887", "同一主體", 0.88f, "2026/06/26", "根據數位足跡分析，此號碼與主號碼共用同一個註冊裝置。"),
                    GenealogyNode(4, "0988-776-334", "行為相似", 0.65f, "2026/06/24", "兩者均呈現隨機間隔撥打特性，符合機器人撥號特徵。"),
                    GenealogyNode(5, "0977-221-990", "報案關聯", 0.92f, "2026/06/26", "多名受害者回報，曾先後接到這兩個號碼的誘騙電話。"),
                    GenealogyNode(6, "0800-001-001", "偽冒客服", 0.82f, "2026/06/20", "此號碼常與主號碼搭配，作為二線解除分期付款之假銀行員使用。")
                )
            )
        } else {
            // 生產環境：回傳空資料或串接真實 API
            PhoneGenealogyData(currentRoot, 0f, 0f, "無分群資料", emptyList())
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF06090E))) {
        // 背景網格
        val infiniteTransition = rememberInfiniteTransition(label = "bg")
        val gridAlpha by infiniteTransition.animateFloat(0.02f, 0.08f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "grid")
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 40.dp.toPx()
            for (x in 0..size.width.toInt() step gridSize.toInt()) { drawLine(Color(0xFF2979FF).copy(alpha = gridAlpha), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height)) }
            for (y in 0..size.height.toInt() step gridSize.toInt()) { drawLine(Color(0xFF2979FF).copy(alpha = gridAlpha), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat())) }
        }
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("號碼關聯族譜", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White) } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            // --- 優化點 3：Empty State 判斷 ---
            if (genealogyData.relatedNodes.isEmpty()) {
                EmptyGenealogyState(Modifier.padding(innerPadding))
            } else {
                GenealogyContent(
                    innerPadding = innerPadding,
                    data = genealogyData,
                    onNodeClick = { selectedNode = it },
                    onInfoClick = { showMetricInfo = it }
                )
            }
        }

        if (selectedNode != null) {
            NodeDetailDialog(node = selectedNode!!, onDismiss = { selectedNode = null }, onSwitchRoot = { currentRoot = selectedNode!!.phoneNumber; selectedNode = null })
        }

        if (showMetricInfo != null) {
            MetricInfoDialog(type = showMetricInfo!!, onDismiss = { showMetricInfo = null })
        }
    }
}

@Composable
private fun GenealogyContent(innerPadding: PaddingValues, data: PhoneGenealogyData, onNodeClick: (GenealogyNode) -> Unit, onInfoClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        GenealogyGraph(data, onNodeClick)
        Spacer(modifier = Modifier.height(40.dp))
        Text("號碼風險分析數據", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(20.dp))
        GenealogyMetricsSection(data, onInfoClick)
        Spacer(modifier = Modifier.height(40.dp))
        InstructionCard(data.clusterId)
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun GenealogyGraph(data: PhoneGenealogyData, onNodeClick: (GenealogyNode) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "graph")
    val orbitRotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "rotate")
    
    // --- 關鍵修正：使用 rememberUpdatedState 避免 pointerInput 頻繁重啟 ---
    val currentRotation by rememberUpdatedState(orbitRotation)

    val textMeasurer = rememberTextMeasurer()

    // --- 優化點 4：效能。將文字測量移出 draw 區塊，僅在 data 改變時重新計算 ---
    val textCache = remember(data) {
        val numberStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        val technicalStyle = TextStyle(color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, shadow = Shadow(Color(0xFF00E5FF).copy(alpha = 0.5f), blurRadius = 5f))
        
        GenealogyTextCache(
            rootText = textMeasurer.measure(data.rootNumber, numberStyle.copy(fontSize = 11.sp, color = Color(0xFFFF8A80))),
            nodeLabels = data.relatedNodes.map { textMeasurer.measure(it.phoneNumber, numberStyle.copy(fontSize = 8.sp)) },
            relationLabels = data.relatedNodes.map { textMeasurer.measure(it.relationship, technicalStyle) }
        )
    }

    Canvas(modifier = Modifier.size(340.dp).pointerInput(data) {
        detectTapGestures { offset ->
            val centerX = size.width / 2; val centerY = size.height / 2; val orbitRadius = 115.dp.toPx(); val nodeRadius = 32.dp.toPx()
            data.relatedNodes.forEachIndexed { index, node ->
                val angle = Math.toRadians(index * (360.0 / data.relatedNodes.size) - 90.0 + currentRotation)
                val nodeX = centerX + orbitRadius * cos(angle).toFloat(); val nodeY = centerY + orbitRadius * sin(angle).toFloat()
                if (sqrt((offset.x - nodeX) * (offset.x - nodeX) + (offset.y - nodeY) * (offset.y - nodeY)) <= nodeRadius) onNodeClick(node)
            }
        }
    }) {
        val centerX = size.width / 2; val centerY = size.height / 2; val orbitRadius = 115.dp.toPx(); val rootRadius = 50.dp.toPx(); val nodeRadius = 32.dp.toPx()
        
        // 軌道
        drawCircle(Color(0xFF2979FF).copy(alpha = 0.1f), orbitRadius, Offset(centerX, centerY), style = Stroke(width = 1.dp.toPx()))

        // 連接線
        data.relatedNodes.forEachIndexed { index, _ ->
            val angle = Math.toRadians(index * (360.0 / data.relatedNodes.size) - 90.0 + orbitRotation)
            val endX = centerX + orbitRadius * cos(angle).toFloat(); val endY = centerY + orbitRadius * sin(angle).toFloat()
            drawLine(Color(0xFF2979FF).copy(alpha = 0.2f), Offset(centerX, centerY), Offset(endX, endY), strokeWidth = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        }

        // 中心節點
        drawCircle(Color(0xFFFF1744).copy(alpha = 0.08f), rootRadius * 1.6f, Offset(centerX, centerY))
        drawCircle(Color(0xFF121A21), rootRadius, Offset(centerX, centerY))
        drawCircle(Color(0xFFFF1744), rootRadius, Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))
        drawText(textLayoutResult = textCache.rootText, topLeft = Offset(centerX - textCache.rootText.size.width / 2, centerY - textCache.rootText.size.height / 2))

        // 衛星節點
        data.relatedNodes.forEachIndexed { index, _ ->
            val angle = Math.toRadians(index * (360.0 / data.relatedNodes.size) - 90.0 + orbitRotation)
            val nodeX = centerX + orbitRadius * cos(angle).toFloat(); val nodeY = centerY + orbitRadius * sin(angle).toFloat()
            
            drawCircle(Color(0xFF121A21), nodeRadius, Offset(nodeX, nodeY))
            drawCircle(Color(0xFF2979FF).copy(alpha = 0.5f), nodeRadius, Offset(nodeX, nodeY), style = Stroke(width = 1.5.dp.toPx()))
            drawArc(Color(0xFF00E5FF).copy(alpha = 0.7f), (orbitRotation * 3 + index * 60), 90f, false, Offset(nodeX - nodeRadius, nodeY - nodeRadius), androidx.compose.ui.geometry.Size(nodeRadius * 2, nodeRadius * 2), style = Stroke(width = 2.dp.toPx()))

            val label = textCache.nodeLabels[index]
            drawText(textLayoutResult = label, topLeft = Offset(nodeX - label.size.width / 2, nodeY - label.size.height / 2))
            
            val rel = textCache.relationLabels[index]
            drawText(textLayoutResult = rel, topLeft = Offset(nodeX - rel.size.width / 2, nodeY + nodeRadius + 8.dp.toPx()))
        }
    }
}

@Composable
fun NodeDetailDialog(node: GenealogyNode, onDismiss: () -> Unit, onSwitchRoot: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color(0xFF0D1520),
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF448AFF)); Spacer(modifier = Modifier.width(12.dp)); Text("號碼詳細分析", color = Color.White) } },
        text = {
            Column {
                Text("標籤號碼：${node.phoneNumber}", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp)); Text("最後活躍：${node.lastActive}", color = Color(0xFF00E5FF), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp)); Text(node.detailReason, color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(16.dp)); LinearProgressIndicator(progress = { node.connectionStrength }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Color(0xFF2979FF), trackColor = Color.White.copy(alpha = 0.1f))
                Text("關聯強度：${(node.connectionStrength * 100).toInt()}%", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = { Button(onClick = onSwitchRoot, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF)), shape = RoundedCornerShape(12.dp)) { Text("以此號碼重新分析", color = Color.White, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("關閉", color = Color.LightGray) } }
    )
}

@Composable
private fun MetricInfoDialog(type: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color(0xFF0D1520),
        title = { Text(if(type == "risk") "規則式風險評分" else "號碼關聯分數", color = Color.White) },
        text = { Text(if(type == "risk") "此評分代表號碼本身的惡意特徵強度。包含是否為人頭、有無黑名單紀錄、以及發話行為是否異常。" else "此分數代表該號碼與現有詐騙集團核心網絡的貼合程度。越高代表其在集團分工中的位置越明確。", color = Color.LightGray) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("了解", color = Color(0xFF448AFF)) } }
    )
}

@Composable
private fun EmptyGenealogyState(modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("目前無關聯資料", color = Color.Gray, fontSize = 16.sp)
    }
}

@Composable
private fun InstructionCard(clusterId: String) {
    Surface(color = Color(0xFF121A21).copy(alpha = 0.8f), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2979FF).copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("提示：系統偵測到該號碼與 $clusterId 集團具備極高關聯。所有節點已進行脫敏處理，以符合資安規範。", color = Color.LightGray, fontSize = 13.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
fun GenealogyMetricsSection(data: PhoneGenealogyData, onInfoClick: (String) -> Unit) {
    val riskAnim = remember { Animatable(0f) }
    val associationAnim = remember { Animatable(0f) }
    LaunchedEffect(data) {
        riskAnim.animateTo(data.riskScore, tween(1500, easing = FastOutSlowInEasing))
        associationAnim.animateTo(data.associationScore, tween(1500, easing = FastOutSlowInEasing))
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCardWithInfo(Modifier.weight(1f), "規則式風險評分", "${(riskAnim.value * 100).toInt()}%", Color(0xFFFF1744), Icons.Default.SettingsSuggest) { onInfoClick("risk") }
            Spacer(modifier = Modifier.width(12.dp))
            MetricCardWithInfo(Modifier.weight(1f), "號碼關聯分數", "${(associationAnim.value * 100).toInt()}/100", Color(0xFF448AFF), Icons.Default.Hub) { onInfoClick("assoc") }
        }
        Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF121A21), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.2f))) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).background(Color(0xFF00E5FF).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp)) }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("所屬詐騙集團類型", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(data.clusterId, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(letterSpacing = 0.5.sp, shadow = Shadow(Color(0xFF00E5FF), blurRadius = 10f)))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("核心成員", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun MetricCardWithInfo(modifier: Modifier, title: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onInfo: () -> Unit) {
    Surface(modifier = modifier.height(125.dp), color = Color(0xFF121A21), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                IconButton(onClick = onInfo, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = color, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(shadow = Shadow(color, blurRadius = 15f)))
            Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF06090E)
@Composable
fun PhoneGenealogyPreview() {
    PhoneGenealogyScreen("0912-345-678") {}
}
