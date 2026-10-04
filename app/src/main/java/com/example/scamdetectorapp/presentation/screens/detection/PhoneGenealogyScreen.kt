package com.example.scamdetectorapp.presentation.screens.detection

import android.app.Application
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.presentation.model.GenealogyNode
import com.example.scamdetectorapp.presentation.model.PhoneGenealogyData
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import com.example.scamdetectorapp.presentation.viewmodel.PhoneGenealogyUiState
import com.example.scamdetectorapp.ui.theme.AppBackgroundBrush
import com.example.scamdetectorapp.ui.theme.BrightRed
import com.example.scamdetectorapp.ui.theme.DeepDarkBlue
import com.example.scamdetectorapp.ui.theme.ElectricBlue
import com.example.scamdetectorapp.ui.theme.ScamCyan
import com.example.scamdetectorapp.ui.theme.SurfaceDark
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class GenealogyTextCache(
    val rootText: TextLayoutResult,
    val nodeLabels: List<TextLayoutResult>,
    val relationLabels: List<TextLayoutResult>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneGenealogyScreen(
    phoneNumber: String,
    onBack: () -> Unit,
    viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(LocalContext.current.applicationContext as Application))
) {
    val uiState by viewModel.phoneGenealogyState.collectAsStateWithLifecycle()
    var currentRoot by remember(phoneNumber) { mutableStateOf(phoneNumber) }
    var selectedNode by remember { mutableStateOf<GenealogyNode?>(null) }

    LaunchedEffect(currentRoot) {
        viewModel.loadPhoneGenealogy(currentRoot)
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackgroundBrush)) {
        val infiniteTransition = rememberInfiniteTransition(label = "bg")
        val gridAlpha by infiniteTransition.animateFloat(
            initialValue = 0.02f,
            targetValue = 0.08f,
            animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
            label = "grid"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 40.dp.toPx()
            for (x in 0..size.width.toInt() step gridSize.toInt()) {
                drawLine(ElectricBlue.copy(alpha = gridAlpha), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
            }
            for (y in 0..size.height.toInt() step gridSize.toInt()) {
                drawLine(ElectricBlue.copy(alpha = gridAlpha), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("號碼關聯族譜", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                viewModel.resetPhoneGenealogy()
                                onBack()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_back_less_than),
                                contentDescription = "返回",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            when (val state = uiState) {
                PhoneGenealogyUiState.Idle, PhoneGenealogyUiState.Loading -> LoadingGenealogyState(Modifier.padding(innerPadding))
                is PhoneGenealogyUiState.Error -> EmptyGenealogyState(
                    modifier = Modifier.padding(innerPadding),
                    message = state.message
                )
                is PhoneGenealogyUiState.Success -> {
                    if (state.data.relatedNodes.isEmpty()) {
                        EmptyGenealogyState(
                            modifier = Modifier.padding(innerPadding),
                            message = if (state.data.status == "black") "目前沒有可顯示的關聯號碼" else "此號碼不是黑名單，沒有族譜資料"
                        )
                    } else {
                        GenealogyContent(
                            innerPadding = innerPadding,
                            data = state.data,
                            onNodeClick = { selectedNode = it }
                        )
                    }
                }
            }
        }

        if (selectedNode != null) {
            NodeDetailDialog(
                node = selectedNode!!,
                onDismiss = { selectedNode = null },
                onSwitchRoot = {
                    currentRoot = selectedNode!!.phoneNumber
                    selectedNode = null
                }
            )
        }
    }
}

@Composable
private fun LoadingGenealogyState(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = ScamCyan)
        Spacer(modifier = Modifier.height(16.dp))
        Text("載入號碼關聯資料中...", color = Color.White, fontSize = 16.sp)
    }
}

@Composable
private fun GenealogyContent(innerPadding: PaddingValues, data: PhoneGenealogyData, onNodeClick: (GenealogyNode) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GenealogyGraph(data, onNodeClick)
        Spacer(modifier = Modifier.height(40.dp))
        Text("號碼所屬標籤：${data.tagId}", color = ScamCyan, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun GenealogyGraph(data: PhoneGenealogyData, onNodeClick: (GenealogyNode) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "graph")
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing)),
        label = "rotate"
    )
    val currentRotation by rememberUpdatedState(orbitRotation)
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    val textCache = remember(data) {
        val numberStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        val technicalStyle = TextStyle(
            color = ScamCyan,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            shadow = Shadow(ScamCyan.copy(alpha = 0.5f), blurRadius = 5f)
        )

        GenealogyTextCache(
            rootText = textMeasurer.measure(data.rootNumber, numberStyle.copy(fontSize = 11.sp, color = Color(0xFFFF8A80))),
            nodeLabels = data.relatedNodes.map { textMeasurer.measure(it.phoneNumber, numberStyle.copy(fontSize = 8.sp)) },
            relationLabels = data.relatedNodes.map { textMeasurer.measure(it.relationship, technicalStyle) }
        )
    }

    Canvas(
        modifier = Modifier
            .size(340.dp)
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val orbitRadius = 115.dp.toPx()
                    val nodeRadius = 32.dp.toPx()
                    data.relatedNodes.forEachIndexed { index, node ->
                        val angle = Math.toRadians(index * (360.0 / data.relatedNodes.size) - 90.0 + currentRotation)
                        val nodeX = centerX + orbitRadius * cos(angle).toFloat()
                        val nodeY = centerY + orbitRadius * sin(angle).toFloat()
                        if (sqrt((offset.x - nodeX) * (offset.x - nodeX) + (offset.y - nodeY) * (offset.y - nodeY)) <= nodeRadius) {
                            onNodeClick(node)
                        }
                    }
                }
            }
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val orbitRadius = 115.dp.toPx()
        val rootRadius = 50.dp.toPx()
        val nodeRadius = 32.dp.toPx()

        drawCircle(ElectricBlue.copy(alpha = 0.1f), orbitRadius, Offset(centerX, centerY), style = Stroke(width = 1.dp.toPx()))

        data.relatedNodes.forEachIndexed { index, _ ->
            val angle = Math.toRadians(index * (360.0 / data.relatedNodes.size) - 90.0 + orbitRotation)
            val endX = centerX + orbitRadius * cos(angle).toFloat()
            val endY = centerY + orbitRadius * sin(angle).toFloat()
            drawLine(
                ElectricBlue.copy(alpha = 0.2f),
                Offset(centerX, centerY),
                Offset(endX, endY),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        drawCircle(BrightRed.copy(alpha = 0.08f), rootRadius * 1.6f, Offset(centerX, centerY))
        drawCircle(SurfaceDark, rootRadius, Offset(centerX, centerY))
        drawCircle(BrightRed, rootRadius, Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))
        drawText(textLayoutResult = textCache.rootText, topLeft = Offset(centerX - textCache.rootText.size.width / 2, centerY - textCache.rootText.size.height / 2))

        data.relatedNodes.forEachIndexed { index, _ ->
            val angle = Math.toRadians(index * (360.0 / data.relatedNodes.size) - 90.0 + orbitRotation)
            val nodeX = centerX + orbitRadius * cos(angle).toFloat()
            val nodeY = centerY + orbitRadius * sin(angle).toFloat()

            drawCircle(SurfaceDark, nodeRadius, Offset(nodeX, nodeY))
            drawCircle(ElectricBlue.copy(alpha = 0.5f), nodeRadius, Offset(nodeX, nodeY), style = Stroke(width = 1.5.dp.toPx()))
            drawArc(
                ScamCyan.copy(alpha = 0.7f),
                orbitRotation * 3 + index * 60,
                90f,
                false,
                Offset(nodeX - nodeRadius, nodeY - nodeRadius),
                androidx.compose.ui.geometry.Size(nodeRadius * 2, nodeRadius * 2),
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
        onDismissRequest = onDismiss,
        containerColor = DeepDarkBlue,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = ScamCyan)
                Spacer(modifier = Modifier.size(12.dp))
                Text("號碼關聯分析", color = Color.White)
            }
        },
        text = {
            Column {
                Text("標籤號碼：${node.phoneNumber}", color = Color.White, fontWeight = FontWeight.Bold)
                node.lastActive?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("最後回報：$it", color = ScamCyan, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("關聯原因：", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                node.reasons.forEach { reason ->
                    Text("• $reason", color = Color.LightGray, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { node.connectionStrength },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = ElectricBlue,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text("關聯強度：${(node.connectionStrength * 100).toInt()}%", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = onSwitchRoot,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.size(8.dp))
                Text("以此號碼重新分析", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("關閉", color = Color.LightGray)
            }
        }
    )
}

@Composable
private fun EmptyGenealogyState(modifier: Modifier, message: String) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceDark.copy(alpha = 0.72f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 3.dp.toPx()
                val panelWidth = size.width * 0.78f
                val panelHeight = size.height * 0.62f
                val left = (size.width - panelWidth) / 2
                val top = (size.height - panelHeight) / 2
                val corner = 20.dp.toPx()

                drawRoundRect(
                    color = ElectricBlue.copy(alpha = 0.16f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(panelWidth, panelHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
                )
                drawRoundRect(
                    color = ScamCyan.copy(alpha = 0.75f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(panelWidth, panelHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
                    style = Stroke(width = stroke)
                )

                val radarCenter = Offset(size.width / 2, size.height / 2 - 4.dp.toPx())
                val radarRadius = 28.dp.toPx()
                drawCircle(
                    color = ScamCyan.copy(alpha = 0.16f),
                    radius = radarRadius * 1.6f,
                    center = radarCenter
                )
                drawCircle(
                    color = ScamCyan.copy(alpha = 0.72f),
                    radius = radarRadius,
                    center = radarCenter,
                    style = Stroke(width = stroke)
                )
                drawLine(
                    color = ScamCyan.copy(alpha = 0.72f),
                    start = Offset(radarCenter.x - radarRadius, radarCenter.y),
                    end = Offset(radarCenter.x + radarRadius, radarCenter.y),
                    strokeWidth = stroke
                )
                drawLine(
                    color = ScamCyan.copy(alpha = 0.72f),
                    start = Offset(radarCenter.x, radarCenter.y - radarRadius),
                    end = Offset(radarCenter.x, radarCenter.y + radarRadius),
                    strokeWidth = stroke
                )

                val slashInset = 18.dp.toPx()
                drawLine(
                    color = BrightRed.copy(alpha = 0.9f),
                    start = Offset(left + slashInset, top + panelHeight - slashInset),
                    end = Offset(left + panelWidth - slashInset, top + slashInset),
                    strokeWidth = 4.dp.toPx()
                )

                val dotRadius = 4.dp.toPx()
                listOf(
                    Offset(left + 24.dp.toPx(), top + 22.dp.toPx()),
                    Offset(left + panelWidth - 28.dp.toPx(), top + panelHeight - 26.dp.toPx()),
                    Offset(left + panelWidth - 44.dp.toPx(), top + 26.dp.toPx())
                ).forEach { center ->
                    drawCircle(color = ElectricBlue.copy(alpha = 0.82f), radius = dotRadius, center = center)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            color = Color(0xFFB0BEC5),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF06090E)
@Composable
private fun PhoneGenealogyPreview() {
    GenealogyContent(
        innerPadding = PaddingValues(),
        data = PhoneGenealogyData(
            rootNumber = "0912345678",
            tagId = "假投資",
            relatedNodes = listOf(
                GenealogyNode(1, "0912345679", "靜態特徵", 0.95f, "2024-03-01 10:00:00", listOf("同號段且尾碼物理接近")),
                GenealogyNode(2, "0912345680", "靜態特徵", 0.72f, "2024-03-01 10:00:00", listOf("直接電話轉介"))
            )
        ),
        onNodeClick = {}
    )
}
