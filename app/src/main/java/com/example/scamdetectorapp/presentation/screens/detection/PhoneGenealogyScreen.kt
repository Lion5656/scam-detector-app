package com.example.scamdetectorapp.presentation.screens.detection

import android.app.Application
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.presentation.model.GenealogyNode
import com.example.scamdetectorapp.presentation.model.PhoneGenealogyData
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import com.example.scamdetectorapp.presentation.viewmodel.PhoneGenealogyUiState
import com.example.scamdetectorapp.util.LottieLoadingView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val AppDarkBg = Color(0xFF0B0F19)
private val AppLightBg = Color(0xFF121A21)
private val AppCardSurface = Color(0xFF1A2332)
private val AppCardBorder = Color(0xFF2E3B4E)
private val AppSubtleText = Color(0xFF94A3B8)

private val CyberBlue = Color(0xFF00E5FF)
private val CyberPink = Color(0xFFFF1744)
private val CyberAmber = Color(0xFFFFB300)
private val NodeInnerFill = lerp(AppDarkBg, Color(0xFF0D47A1), 0.25f)

private const val HIGH_RELATION_THRESHOLD = 0.8f

private fun nodeRingColor(connectionStrength: Float): Color =
    if (connectionStrength >= HIGH_RELATION_THRESHOLD) CyberPink else CyberAmber

private data class GenealogyTextCache(
    val rootText: TextLayoutResult,
    val nodeLabels: List<TextLayoutResult>
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppDarkBg, AppLightBg)))
    ) {
        GenealogyNetworkBackdrop(modifier = Modifier.fillMaxSize())

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("號碼關聯族譜", color = AppSubtleText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (uiState is PhoneGenealogyUiState.Success) (uiState as PhoneGenealogyUiState.Success).data.rootNumber else currentRoot,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
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
                        GenealogySatelliteContent(
                            innerPadding = innerPadding,
                            data = state.data,
                            onNodeClick = { selectedNode = it }
                        )
                    }
                }
            }
        }

        selectedNode?.let { node ->
            NodeDetailDialog(
                node = node,
                onDismiss = { selectedNode = null },
                onSwitchRoot = {
                    currentRoot = node.phoneNumber
                    selectedNode = null
                }
            )
        }
    }
}

private data class BackdropParticle(
    val x: Float,
    val y: Float,
    val phase: Float,
    val speed: Int
)

/**
 * 族譜背景：淡點陣格線 + 中央深藍光暈 + 緩慢漂移的星網節點（距離夠近時連線），呼應號碼關聯網路。
 * speed 為整數，動畫時間 0..2π 循環時位置可無縫銜接。
 */
@Composable
private fun GenealogyNetworkBackdrop(modifier: Modifier = Modifier) {
    val particles = remember {
        val random = Random(20261005)
        List(36) {
            BackdropParticle(
                x = random.nextFloat(),
                y = random.nextFloat(),
                phase = random.nextFloat() * 2f * PI.toFloat(),
                speed = 1 + random.nextInt(2)
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "backdrop")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "drift"
    )

    Canvas(modifier = modifier) {
        val gridStep = 28.dp.toPx()
        val dotRadius = 1.dp.toPx()
        var gx = gridStep / 2
        while (gx < size.width) {
            var gy = gridStep / 2
            while (gy < size.height) {
                drawCircle(AppCardBorder.copy(alpha = 0.35f), dotRadius, Offset(gx, gy))
                gy += gridStep
            }
            gx += gridStep
        }

        val glowCenter = Offset(size.width / 2, size.height * 0.4f)
        val glowRadius = size.width * 0.9f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0D47A1).copy(alpha = 0.28f), Color.Transparent),
                center = glowCenter,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = glowCenter
        )

        val drift = 14.dp.toPx()
        val points = particles.map { p ->
            Offset(
                p.x * size.width + drift * cos(time * p.speed + p.phase),
                p.y * size.height + drift * sin(time * p.speed + p.phase)
            )
        }
        val linkDistance = 110.dp.toPx()
        val lineWidth = 0.8.dp.toPx()
        for (i in points.indices) {
            for (j in i + 1 until points.size) {
                val distance = (points[i] - points[j]).getDistance()
                if (distance < linkDistance) {
                    drawLine(
                        color = CyberBlue.copy(alpha = 0.14f * (1f - distance / linkDistance)),
                        start = points[i],
                        end = points[j],
                        strokeWidth = lineWidth
                    )
                }
            }
        }
        val particleRadius = 1.8.dp.toPx()
        points.forEach { point ->
            drawCircle(CyberBlue.copy(alpha = 0.08f), particleRadius * 3f, point)
            drawCircle(CyberBlue.copy(alpha = 0.4f), particleRadius, point)
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
        LottieLoadingView(size = 130.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("載入號碼關聯族譜中...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GenealogySatelliteContent(
    innerPadding: PaddingValues,
    data: PhoneGenealogyData,
    onNodeClick: (GenealogyNode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        GenealogySatelliteGraph(
            data = data,
            onNodeClick = onNodeClick
        )

        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            color = AppCardSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "號碼分類標籤：${data.tagId}",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private data class GenealogyLayout(
    val orbitRadius: Float,
    val nodeRadius: Float,
    val touchRadius: Float
)

/** 單一軌道版面：節點數多時自動縮小節點半徑，避免相鄰節點重疊。 */
private fun Density.genealogyLayout(count: Int): GenealogyLayout {
    val orbitRadius = 150.dp.toPx()
    val maxNodeRadius = 28.dp.toPx()
    val fitRadius = if (count > 1) orbitRadius * sin(PI / count).toFloat() - 4.dp.toPx() else maxNodeRadius
    val nodeRadius = fitRadius.coerceIn(18.dp.toPx(), maxNodeRadius)
    return GenealogyLayout(
        orbitRadius = orbitRadius,
        nodeRadius = nodeRadius,
        touchRadius = maxOf(nodeRadius, 22.dp.toPx())
    )
}

private fun nodeCenter(center: Offset, orbitRadius: Float, index: Int, count: Int, rotation: Float): Offset {
    val angle = Math.toRadians(index * (360.0 / count) - 90.0 + rotation)
    return Offset(
        center.x + orbitRadius * cos(angle).toFloat(),
        center.y + orbitRadius * sin(angle).toFloat()
    )
}

@Composable
fun GenealogySatelliteGraph(
    data: PhoneGenealogyData,
    onNodeClick: (GenealogyNode) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbit")
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing)),
        label = "rotate"
    )
    // pointerInput 只以 data 為 key，旋轉角度透過 rememberUpdatedState 讀取，避免每幀重啟手勢偵測
    val currentRotation by rememberUpdatedState(orbitRotation)
    val currentOnNodeClick by rememberUpdatedState(onNodeClick)

    val count = data.relatedNodes.size
    val useShortLabel = with(LocalDensity.current) { genealogyLayout(count).nodeRadius < 24.dp.toPx() }

    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    val textCache = remember(data, useShortLabel) {
        val rootStyle = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        val nodeStyle = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)

        GenealogyTextCache(
            rootText = textMeasurer.measure(data.rootNumber, rootStyle),
            nodeLabels = data.relatedNodes.map {
                val label = if (useShortLabel) "…${it.phoneNumber.takeLast(4)}" else it.phoneNumber
                textMeasurer.measure(label, nodeStyle)
            }
        )
    }

    Box(
        modifier = Modifier.size(380.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        val layout = genealogyLayout(count)
                        val center = Offset(size.width / 2f, size.height / 2f)
                        data.relatedNodes.indices
                            .firstOrNull { index ->
                                val nodePos = nodeCenter(center, layout.orbitRadius, index, count, currentRotation)
                                (offset - nodePos).getDistance() <= layout.touchRadius
                            }
                            ?.let { currentOnNodeClick(data.relatedNodes[it]) }
                    }
                }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val rootRadius = 46.dp.toPx()
            val ringWidth = 2.5.dp.toPx()
            val layout = genealogyLayout(count)

            drawCircle(
                color = AppCardBorder,
                radius = layout.orbitRadius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
            )

            drawCircle(NodeInnerFill, rootRadius, center)
            drawCircle(CyberBlue, rootRadius, center, style = Stroke(width = ringWidth))
            drawText(
                textLayoutResult = textCache.rootText,
                topLeft = Offset(center.x - textCache.rootText.size.width / 2, center.y - textCache.rootText.size.height / 2)
            )

            data.relatedNodes.forEachIndexed { index, node ->
                val nodePos = nodeCenter(center, layout.orbitRadius, index, count, orbitRotation)

                drawCircle(NodeInnerFill, layout.nodeRadius, nodePos)
                drawCircle(nodeRingColor(node.connectionStrength), layout.nodeRadius, nodePos, style = Stroke(width = ringWidth))

                val label = textCache.nodeLabels[index]
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(nodePos.x - label.size.width / 2, nodePos.y - label.size.height / 2)
                )
            }
        }
    }
}

@Composable
fun NodeDetailDialog(
    node: GenealogyNode,
    onDismiss: () -> Unit,
    onSwitchRoot: () -> Unit
) {
    val themeColor = nodeRingColor(node.connectionStrength)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppLightBg,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(themeColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "號碼關聯詳情",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Surface(
                    color = AppCardSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "目標號碼：${node.phoneNumber}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        node.lastActive?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "最後回報時間：$it",
                                color = AppSubtleText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "關聯原因與特徵：",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                node.reasons.forEach { reason ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "• ", color = themeColor, fontWeight = FontWeight.Bold)
                        Text(text = reason, color = Color(0xFFD0D0D5), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "關聯強度", color = AppSubtleText, fontSize = 12.sp)
                    Text(
                        text = "${(node.connectionStrength * 100).toInt()}%",
                        color = themeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { node.connectionStrength },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = themeColor,
                    trackColor = AppCardBorder
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSwitchRoot,
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "以此號碼重新分析",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "關閉", color = AppSubtleText)
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
        Surface(
            color = AppCardSurface,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AppCardBorder),
            modifier = Modifier.size(180.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 2.5.dp.toPx()
                    val radarCenter = Offset(size.width / 2, size.height / 2)
                    val radarRadius = 32.dp.toPx()

                    drawCircle(
                        color = CyberBlue,
                        radius = radarRadius,
                        center = radarCenter,
                        style = Stroke(width = stroke)
                    )
                    drawLine(
                        color = AppCardBorder,
                        start = Offset(radarCenter.x - radarRadius, radarCenter.y),
                        end = Offset(radarCenter.x + radarRadius, radarCenter.y),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = AppCardBorder,
                        start = Offset(radarCenter.x, radarCenter.y - radarRadius),
                        end = Offset(radarCenter.x, radarCenter.y + radarRadius),
                        strokeWidth = stroke
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            color = AppSubtleText,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun previewNodes(count: Int): List<GenealogyNode> =
    List(count) { i ->
        GenealogyNode(
            id = i + 1,
            phoneNumber = "09123456${(80 + i).toString().padStart(2, '0')}",
            relationship = "靜態特徵",
            connectionStrength = if (i % 3 == 0) 0.92f else 0.65f,
            lastActive = "2024-03-01 10:00:00",
            reasons = listOf("同號段且尾碼物理接近")
        )
    }

@Preview(showBackground = true, backgroundColor = 0xFF0B0F19)
@Composable
private fun PhoneGenealogyPreview() {
    GenealogySatelliteContent(
        innerPadding = PaddingValues(),
        data = PhoneGenealogyData(rootNumber = "0912345678", tagId = "假投資", relatedNodes = previewNodes(8)),
        onNodeClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F19)
@Composable
private fun PhoneGenealogyDensePreview() {
    GenealogySatelliteContent(
        innerPadding = PaddingValues(),
        data = PhoneGenealogyData(rootNumber = "0912345678", tagId = "假投資", relatedNodes = previewNodes(20)),
        onNodeClick = {}
    )
}
