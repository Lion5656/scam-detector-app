package com.example.scamdetectorapp.presentation.screens.detection

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
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
import com.example.scamdetectorapp.util.LottieLoadingView
import org.intellij.lang.annotations.Language
import com.valentinilk.shimmer.shimmer
import com.airbnb.lottie.compose.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// --- 優化點 1：文字測量快取結構 ---
private data class GenealogyTextCache(
    val rootText: TextLayoutResult,
    val nodeLabels: List<TextLayoutResult>,
    val relationLabels: List<TextLayoutResult>
)

@Language("AGSL")
private const val CYBER_SHADER_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    
    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution.xy;
        float3 color = float3(0.02, 0.04, 0.08);
        
        // 動態網格 (使用 Shader 計算比 Canvas 更省電)
        float2 gridUv = uv * float2(20.0, 20.0 * iResolution.y / iResolution.x);
        float2 gridLine = smoothstep(0.0, 0.05, abs(fract(gridUv - 0.5) - 0.5));
        float g = 1.0 - min(gridLine.x, gridLine.y);
        color += float3(0.16, 0.47, 1.0) * g * 0.1;
        
        // 全息掃描線
        float scan = sin(uv.y * 400.0 - iTime * 10.0) * 0.5 + 0.5;
        color += float3(0.0, 0.8, 1.0) * pow(scan, 30.0) * 0.05;
        
        // 中心發光
        float d = distance(uv, float2(0.5, 0.5));
        color += float3(0.1, 0.3, 0.6) * (1.0 - smoothstep(0.0, 0.6, d)) * 0.3;
        
        return half4(color, 1.0);
    }
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun CyberShaderBackground() {
    val shader = remember { RuntimeShader(CYBER_SHADER_SRC) }
    val infiniteTransition = rememberInfiniteTransition(label = "shader")
    val time by infiniteTransition.animateFloat(0f, 1000f, infiniteRepeatable(tween(100000, easing = LinearEasing)), label = "time")

    Canvas(modifier = Modifier.fillMaxSize()) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", time)
        drawContext.canvas.nativeCanvas.drawPaint(android.graphics.Paint().apply {
            this.shader = shader
        })
    }
}

@Composable
fun LegacyGridBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gridAlpha by infiniteTransition.animateFloat(0.02f, 0.08f, infiniteRepeatable(tween(3000), RepeatMode.Reverse), label = "grid")
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSize = 40.dp.toPx()
        for (x in 0..size.width.toInt() step gridSize.toInt()) { drawLine(Color(0xFF2979FF).copy(alpha = gridAlpha), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height)) }
        for (y in 0..size.height.toInt() step gridSize.toInt()) { drawLine(Color(0xFF2979FF).copy(alpha = gridAlpha), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat())) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneGenealogyScreen(phoneNumber: String, onBack: () -> Unit) {
    var currentRoot by remember { mutableStateOf(phoneNumber) }
    var selectedNode by remember { mutableStateOf<GenealogyNode?>(null) }
    var showMetricInfo by remember { mutableStateOf<String?>(null) }

    val genealogyData = remember(currentRoot) {
        if (BuildConfig.DEBUG) {
            val scamTypes = listOf("假投資詐騙 ", "假網絡拍賣 ", "解除分期付款 / 假客服", "假愛情交友 ", "假冒機構 / 假檢警", "猜猜我是誰 / 盜用帳號", "假求職 / 騙取帳戶")
            PhoneGenealogyData(
                rootNumber = currentRoot,
                riskScore = 0.92f,
                associationScore = 0.84f,
                clusterId = scamTypes.random(),
                relatedNodes = listOf(
                    GenealogyNode(1, "0912-334-456", "前綴相似", 0.95f, "2026/06/26", "【號碼格式特徵】前 6 碼與主號碼完全相同，判定為同一批次電信號段申請，具備極高物理接近度。"),
                    GenealogyNode(2, "02-2310-9981", "共同回報", 0.75f, "2026/06/25", "【行為共現性】該號碼與主號碼曾被同一批受害者回報，代表兩者屬於同一詐騙流程的不同階段（如：轉接客服）。"),
                    GenealogyNode(3, "0905-112-887", "編輯距離", 0.88f, "2026/06/26", "【號碼格式特徵】字串編輯距離 (Levenshtein) 僅為 1，僅有一碼之差，極大機率為號碼變體組合。"),
                    GenealogyNode(4, "0988-776-334", "通聯密集", 0.65f, "2026/06/24", "【行為共現性】在短時間內與主號碼先後撥打給同一目標群體，呈現典型的集團式掃號特徵。"),
                    GenealogyNode(5, "0977-221-990", "話術相似", 0.92f, "2026/06/26", "【行為共現性】使用者回報之詐騙內容（如：解除分期付款）與主號碼完全一致，高度疑似同集團運作。"),
                    GenealogyNode(6, "0800-001-001", "數字距離", 0.82f, "2026/06/20", "【號碼格式特徵】末位數與主號碼呈連號關係，符合集團大量連號撥打之規律。")
                )
            )
        } else {
            PhoneGenealogyData(currentRoot, 0f, 0f, "無分群資料", emptyList())
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF06090E))) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            CyberShaderBackground()
        } else {
            LegacyGridBackground()
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
            var isLoading by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1000)
                isLoading = false
            }

            if (genealogyData.relatedNodes.isEmpty()) {
                EmptyGenealogyState(Modifier.padding(innerPadding))
            } else {
                GenealogyContent(
                    innerPadding = innerPadding,
                    data = genealogyData,
                    isLoading = isLoading,
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
private fun GenealogyContent(
    innerPadding: PaddingValues, 
    data: PhoneGenealogyData, 
    isLoading: Boolean,
    onNodeClick: (GenealogyNode) -> Unit, 
    onInfoClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (isLoading) {
            LottieLoadingView(size = 200.dp) // 使用全局 Lottie 載入動畫
        } else {
            GenealogyGraph(data, onNodeClick)
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        Text("集團情資即時分析", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(20.dp))
        
        if (isLoading) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row {
                    Box(modifier = Modifier.weight(1f).height(125.dp).shimmer().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)))
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f).height(125.dp).shimmer().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)))
                }
                Box(modifier = Modifier.fillMaxWidth().height(80.dp).shimmer().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)))
            }
        } else {
            GenealogyMetricsSection(data, onInfoClick)
        }

        Spacer(modifier = Modifier.height(40.dp))
        InstructionCard(data.clusterId)
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun GenealogyGraph(data: PhoneGenealogyData, onNodeClick: (GenealogyNode) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "graph")
    val orbitRotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "rotate")
    val currentRotation by rememberUpdatedState(orbitRotation)
    val textMeasurer = rememberTextMeasurer()

    val textCache = remember(data) {
        val numberStyle = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        val technicalStyle = TextStyle(
            color = Color(0xFF00E5FF), 
            fontSize = 9.sp, 
            fontWeight = FontWeight.ExtraBold, 
            shadow = Shadow(Color(0xFF00E5FF).copy(alpha = 0.5f), blurRadius = 5f)
        )
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
        
        // 1. 繪製軌道
        drawCircle(Color(0xFF2979FF).copy(alpha = 0.1f), orbitRadius, Offset(centerX, centerY), style = Stroke(width = 1.dp.toPx()))
        
        // 2. 繪製紅藍漸層霓虹連線 (能量流動感)
        data.relatedNodes.forEachIndexed { index, _ ->
            val angle = Math.toRadians(index * (360.0 / data.relatedNodes.size) - 90.0 + orbitRotation)
            val endX = centerX + orbitRadius * cos(angle).toFloat(); val endY = centerY + orbitRadius * sin(angle).toFloat()
            
            val lineBrush = Brush.linearGradient(
                colors = listOf(Color(0xFFFF1744).copy(alpha = 0.6f), Color(0xFF2979FF).copy(alpha = 0.6f)),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY)
            )
            drawLine(lineBrush, Offset(centerX, centerY), Offset(endX, endY), strokeWidth = 1.5.dp.toPx())
        }

        // 3. 繪製中心「能量核心」
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFFF1744).copy(alpha = 0.2f), Color.Transparent)),
            radius = rootRadius * 1.8f, center = Offset(centerX, centerY)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFF1A1F2B), Color(0xFF0D1520))),
            radius = rootRadius, center = Offset(centerX, centerY)
        )
        drawCircle(Color(0xFFFF1744), rootRadius, Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))
        drawText(textLayoutResult = textCache.rootText, topLeft = Offset(centerX - textCache.rootText.size.width / 2, centerY - textCache.rootText.size.height / 2))

        // 4. 繪製衛星「能量球」節點
        data.relatedNodes.forEachIndexed { index, _ ->
            val angle = Math.toRadians(index * (360.0 / data.relatedNodes.size) - 90.0 + orbitRotation)
            val nodeX = centerX + orbitRadius * cos(angle).toFloat(); val nodeY = centerY + orbitRadius * sin(angle).toFloat()
            
            // 磨砂玻璃質感球體
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2196F3).copy(alpha = 0.8f), Color(0xFF0D47A1).copy(alpha = 0.9f)),
                    center = Offset(nodeX - 5.dp.toPx(), nodeY - 5.dp.toPx()),
                    radius = nodeRadius
                ),
                radius = nodeRadius, center = Offset(nodeX, nodeY)
            )
            // 霓虹光環
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.5f), nodeRadius, Offset(nodeX, nodeY), style = Stroke(width = 1.2.dp.toPx()))
            drawArc(
                Color(0xFF00E5FF), (orbitRotation * 3 + index * 60), 90f, false, 
                Offset(nodeX - nodeRadius, nodeY - nodeRadius), androidx.compose.ui.geometry.Size(nodeRadius * 2, nodeRadius * 2), 
                style = Stroke(width = 2.dp.toPx())
            )

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
                Spacer(modifier = Modifier.height(16.dp))
                val category = if (node.detailReason.contains("號碼格式特徵")) "號碼格式特徵" else "行為共現性"
                val categoryColor = if (category == "號碼格式特徵") Color(0xFF2979FF) else Color(0xFF00E5FF)
                Surface(color = categoryColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp), border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.5f))) {
                    Text(text = category, color = categoryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = node.detailReason.substringAfter("】"), color = Color.LightGray, fontSize = 14.sp, lineHeight = 22.sp)
                Spacer(modifier = Modifier.height(16.dp))
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
        text = { 
            Text(if(type == "risk") "此評分代表號碼本身的惡意程度。系統會分析該號碼是否曾被列入黑名單、是否為不法人頭卡、以及其發話頻率是否符合異常行為特徵。" 
            else "此分數代表該號碼與現有詐騙集團核心網絡的貼合程度。越高代表其在集團分工中的位置越明確，通常代表其為集團之撥號中心或轉接節點。", color = Color.LightGray, lineHeight = 22.sp) 
        },
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
    val reportCountAnim = remember { Animatable(0f) }
    val activeCountAnim = remember { Animatable(0f) }
    LaunchedEffect(data) {
        reportCountAnim.animateTo(1280f, tween(1500, easing = FastOutSlowInEasing))
        activeCountAnim.animateTo(42f, tween(1500, easing = FastOutSlowInEasing))
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCardWithInfo(Modifier.weight(1f), "用戶通報次數", "${reportCountAnim.value.toInt().toString().let { if(it.length > 3) it.take(1) + "," + it.drop(1) else it }} 次", Color(0xFFFF1744), Icons.Default.Whatshot) { onInfoClick("risk") }
            Spacer(modifier = Modifier.width(12.dp))
            MetricCardWithInfo(Modifier.weight(1f), "近 7 天活躍偵測", "+${activeCountAnim.value.toInt()} 次", Color(0xFFFFAB40), Icons.Default.History) { onInfoClick("assoc") }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(), 
            color = Color(0xFF121A21).copy(alpha = 0.7f), 
            shape = RoundedCornerShape(20.dp), 
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).background(Color(0xFF00E5FF).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { 
                    Icon(Icons.Default.Hub, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp)) 
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("所屬詐騙集團類型", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(data.clusterId, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(letterSpacing = 0.5.sp, shadow = Shadow(Color(0xFF00E5FF), blurRadius = 15f)))
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("核心成員", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
fun MetricCardWithInfo(modifier: Modifier, title: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, onInfo: () -> Unit) {
    Surface(
        modifier = modifier.height(130.dp), 
        color = Color(0xFF121A21).copy(alpha = 0.7f), 
        shape = RoundedCornerShape(20.dp), 
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                IconButton(onClick = onInfo, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp)) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(shadow = Shadow(color, blurRadius = 20f)))
            Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF06090E)
@Composable
fun PhoneGenealogyPreview() {
    PhoneGenealogyScreen("0912-345-678") {}
}
