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
import com.example.scamdetectorapp.ui.theme.*
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

    val genealogyData = remember(currentRoot) {
        if (BuildConfig.DEBUG) {
            val scamTypes = listOf("假投資詐騙", "假網絡拍賣", "解除分期付款", "假愛情交友", "假冒機構", "猜猜我是誰", "假求職")
            PhoneGenealogyData(
                rootNumber = currentRoot,
                tagId = scamTypes.random(),
                relatedNodes = listOf(
                    GenealogyNode(1, "0912-334-456", "靜態特徵", 0.95f, "2026/06/26", listOf("前綴相似 (前7碼一致)", "號碼距離極近 (連號)", "電信商一致")),
                    GenealogyNode(2, "02-2310-9981", "行為共現", 0.75f, "2026/06/25", listOf("共同回報者 (3人以上)", "通聯密集度高", "話術相似度 85%")),
                    GenealogyNode(3, "0905-112-887", "靜態特徵", 0.88f, "2026/06/26", listOf("編輯距離 < 2", "電信商一致")),
                    GenealogyNode(4, "0988-776-334", "行為共現", 0.65f, "2026/06/24", listOf("共同回報者 (1人)", "通聯密集度中")),
                    GenealogyNode(5, "0977-221-990", "行為共現", 0.92f, "2026/06/26", listOf("共同回報者 (5人以上)", "話術相似度 92%")),
                    GenealogyNode(6, "0800-001-001", "靜態特徵", 0.82f, "2026/06/20", listOf("偽冒號碼特徵", "編輯距離 < 3"))
                )
            )
        } else {
            PhoneGenealogyData(currentRoot, "無標籤資料", emptyList())
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundBrush)) {
        // 背景網格
        val infiniteTransition = rememberInfiniteTransition(label = "bg")
        val gridAlpha by infiniteTransition.animateFloat(0.02f, 0.08f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "grid")
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 40.dp.toPx()
            for (x in 0..size.width.toInt() step gridSize.toInt()) { drawLine(ElectricBlue.copy(alpha = gridAlpha), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height)) }
            for (y in 0..size.height.toInt() step gridSize.toInt()) { drawLine(ElectricBlue.copy(alpha = gridAlpha), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat())) }
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
                    onNodeClick = { selectedNode = it }
                )
            }
        }

        if (selectedNode != null) {
            NodeDetailDialog(node = selectedNode!!, onDismiss = { selectedNode = null }, onSwitchRoot = { currentRoot = selectedNode!!.phoneNumber; selectedNode = null })
        }
    }
}

@Composable
private fun GenealogyContent(innerPadding: PaddingValues, data: PhoneGenealogyData, onNodeClick: (GenealogyNode) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        GenealogyGraph(data, onNodeClick)
        Spacer(modifier = Modifier.height(40.dp))
        Text("號碼所屬標籤：${data.tagId}", color = ScamCyan, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(60.dp))
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
        val technicalStyle = TextStyle(color = ScamCyan, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, shadow = Shadow(ScamCyan.copy(alpha = 0.5f), blurRadius = 5f))
        
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
        drawCircle(ElectricBlue.copy(alpha = 0.1f), orbitRadius, Offset(centerX, centerY), style = Stroke(width = 1.dp.toPx()))

        // 連接線
        data.relatedNodes.forEachIndexed { index, _ ->
            val angle = Math.toRadians(index * (360.0 / data.relatedNodes.size) - 90.0 + orbitRotation)
            val endX = centerX + orbitRadius * cos(angle).toFloat(); val endY = centerY + orbitRadius * sin(angle).toFloat()
            drawLine(ElectricBlue.copy(alpha = 0.2f), Offset(centerX, centerY), Offset(endX, endY), strokeWidth = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        }

        // 中心節點
        drawCircle(BrightRed.copy(alpha = 0.08f), rootRadius * 1.6f, Offset(centerX, centerY))
        drawCircle(SurfaceDark, rootRadius, Offset(centerX, centerY))
        drawCircle(BrightRed, rootRadius, Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))
        drawText(textLayoutResult = textCache.rootText, topLeft = Offset(centerX - textCache.rootText.size.width / 2, centerY - textCache.rootText.size.height / 2))

        // 衛星節點
        data.relatedNodes.forEachIndexed { index, _ ->
            val angle = Math.toRadians(index * (360.0 / data.relatedNodes.size) - 90.0 + orbitRotation)
            val nodeX = centerX + orbitRadius * cos(angle).toFloat(); val nodeY = centerY + orbitRadius * sin(angle).toFloat()
            
            drawCircle(SurfaceDark, nodeRadius, Offset(nodeX, nodeY))
            drawCircle(ElectricBlue.copy(alpha = 0.5f), nodeRadius, Offset(nodeX, nodeY), style = Stroke(width = 1.5.dp.toPx()))
            drawArc(ScamCyan.copy(alpha = 0.7f), (orbitRotation * 3 + index * 60), 90f, false, Offset(nodeX - nodeRadius, nodeY - nodeRadius), androidx.compose.ui.geometry.Size(nodeRadius * 2, nodeRadius * 2), style = Stroke(width = 2.dp.toPx()))

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
        onDismissRequest = onDismiss, containerColor = DeepDarkBlue,
        title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Hub, contentDescription = null, tint = VibrantBlue); Spacer(modifier = Modifier.width(12.dp)); Text("號碼關聯分析", color = Color.White) } },
        text = {
            Column {
                Text("標籤號碼：${node.phoneNumber}", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp)); Text("最後活躍：${node.lastActive}", color = ScamCyan, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("關聯原因：", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                node.reasons.forEach { reason ->
                    Text("• $reason", color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(16.dp)); LinearProgressIndicator(progress = { node.connectionStrength }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = ElectricBlue, trackColor = Color.White.copy(alpha = 0.1f))
                Text("關聯強度：${(node.connectionStrength * 100).toInt()}%", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = { Button(onClick = onSwitchRoot, colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue), shape = RoundedCornerShape(12.dp)) { Text("以此號碼重新分析", color = Color.White, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("關閉", color = Color.LightGray) } }
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

@Preview(showBackground = true, backgroundColor = 0xFF06090E)
@Composable
fun PhoneGenealogyPreview() {
    PhoneGenealogyScreen("0912-345-678") {}
}
