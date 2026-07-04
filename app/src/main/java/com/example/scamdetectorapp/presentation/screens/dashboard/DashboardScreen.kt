package com.example.scamdetectorapp.presentation.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.DataThresholding
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.presentation.model.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.drawText

@Composable
fun DashboardScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var learningProgress by remember { mutableIntStateOf(87) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF06090E))) {
        // 背景網格
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 40.dp.toPx()
            for (x in 0..size.width.toInt() step gridSize.toInt()) {
                drawLine(Color(0xFF2979FF).copy(alpha = 0.03f), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
            }
            for (y in 0..size.height.toInt() step gridSize.toInt()) {
                drawLine(Color(0xFF2979FF).copy(alpha = 0.03f), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoolTabButton("詐騙知識卡", selectedTab == 0) { selectedTab = 0 }
                CoolTabButton("風險儀表板", selectedTab == 1) { selectedTab = 1 }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (selectedTab) {
                0 -> KnowledgeCardTab(onLearned = { learningProgress = (learningProgress + 2).coerceAtMost(100) })
                1 -> RiskDashboardTab(learningProgress)
            }
        }
    }
}

@Composable
fun CoolTabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "alpha"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF1A237E).copy(alpha = 0.4f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF448AFF).copy(alpha = glowAlpha)) else androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.height(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = text,
                color = if (isSelected) Color(0xFF448AFF) else Color.Gray,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                style = TextStyle(shadow = if (isSelected) Shadow(Color(0xFF2979FF), blurRadius = 10f) else null)
            )
        }
    }
}

@Composable
fun KnowledgeCardTab(onLearned: () -> Unit) {
    val categories = listOf("全部", "投資詐騙", "假冒客服", "愛情詐騙", "網購詐騙", "公家機關", "釣魚簡訊")
    var selectedCategory by remember { mutableStateOf("全部") }
    
    val allCards = remember {
        listOf(
            KnowledgeCard(id = 1, category = "假冒客服", level = "高風險", title = "假冒台灣大哥大客服", content = "「您的帳號異常，請**立即操作 ATM** 解除設定，否則將扣除**違約金**」", detectionMethod = "1. 客服不會要求操作 ATM\n2. 檢查來電顯示是否有 +\n3. 撥打官方專線確認", tags = listOf("情緒勒索", "壓力誘導"), quiz = Quiz("接到自稱客服要求解除設定時，首要動作是？", listOf("立刻去 ATM", "撥打 165 或官網電話", "加對方 Line"), 1, "官方客服絕對不會要求民眾前往 ATM 操作任何設定。")),
            KnowledgeCard(id = 2, category = "投資詐騙", level = "極高風險", title = "飆股簡訊與飆股群組", content = "「保證**高獲利**、**穩賺不賠**！加老師 Line 領取內線交易清單」", detectionMethod = "1. 凡是保證獲利必是詐騙\n2. 不明來源的 Line 群組不要加\n3. 認明金管會核准之券商", tags = listOf("利益誘惑", "假冒名人")),
            KnowledgeCard(id = 3, category = "愛情詐騙", level = "高風險", title = "戰地軍官或跨國醫生", content = "「親愛的，我寄給你的**貴重禮物**被海關扣留，需要你幫忙代付**手續費**」", detectionMethod = "1. 從未見面卻談錢必是詐騙\n2. 視訊時對方總是鏡頭故障\n3. 關鍵字：寄禮物、卡在海關", tags = listOf("情感寄託", "虛假身份"), quiz = Quiz("網戀對象說有包裹卡在海關要求代墊費，該怎麼辦？", listOf("幫他付一點", "拒絕並報警", "跟家人借錢付"), 1, "這是標準的愛情詐騙腳本，海關不會私下收費。")),
            KnowledgeCard(id = 4, category = "網購詐騙", level = "中高風險", title = "FB 一頁式廣告", content = "「**限時出清**！原價 5 萬只賣 **2999**，貨到付款有保障」", detectionMethod = "1. 價格遠低於市價必有詐\n2. 網頁只有單一商品且無聯絡電話\n3. 認明官方商城，不私下交易", tags = listOf("貪小便宜", "急迫感"), quiz = Quiz("看到價差極大的「限時特賣」廣告時，應注意？", listOf("趕快搶購", "確認賣家真實性與電話", "直接貨到付款"), 1, "詐騙常利用超低價與限時壓力誘騙，貨到付款也可能寄來垃圾。")),
            KnowledgeCard(id = 5, category = "公家機關", level = "高風險", title = "假檢察官監管帳戶", content = "「您的帳戶涉及洗錢案，請將存款轉入**監管帳戶**配合調查，否則將**限制出境**」", detectionMethod = "1. 檢警絕對不會要求匯款或監管帳戶\n2. 法院公文不會透過 Line 傳送\n3. 關鍵字：監管帳戶、偵查不公開", tags = listOf("權威恐嚇", "法律威脅"), quiz = Quiz("檢警辦案時，是否會要求民眾將錢轉入「監管帳戶」？", listOf("會，這是程序", "不會，這是詐騙", "視案件大小而定"), 1, "檢警辦案有嚴格法律程序，絕對不會在電話中要求匯款或監管資產。")),
            KnowledgeCard(id = 6, category = "釣魚簡訊", level = "中高風險", title = "交通罰單逾期釣魚", content = "「您的**交通罰單**逾期未繳，請點擊連結查看並繳納：**fake.link/gov**」", detectionMethod = "1. 不要點擊簡訊連結\n2. 罰單請到「監理服務網」查詢\n3. 關鍵字：罰單未繳、補貼入帳", tags = listOf("時事釣魚", "急迫感"), quiz = Quiz("收到附帶連結的罰單簡訊，該如何處理？", listOf("趕快點進去繳費", "不理會，自行上官網查詢", "轉發給親友提醒"), 1, "官方簡訊不會隨意縮網址或要求線上刷卡繳罰單，應自行求證。"))
        )
    }

    val filteredCards = if (selectedCategory == "全部") allCards else allCards.filter { it.category == selectedCategory }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentCard = if (filteredCards.isNotEmpty()) filteredCards[currentIndex % filteredCards.size] else null

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category; currentIndex = 0 },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = Color.Transparent, selectedContainerColor = Color(0xFF1A237E), labelColor = Color.Gray, selectedLabelColor = Color(0xFF448AFF)),
                    border = FilterChipDefaults.filterChipBorder(borderColor = Color(0xFF2979FF).copy(alpha = 0.5f), selectedBorderColor = Color(0xFF2979FF), enabled = true, selected = selectedCategory == category)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (currentCard != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("← 上一張", color = Color.Gray, modifier = Modifier.clickable { if (currentIndex > 0) currentIndex-- })
                Text("第 ${currentIndex + 1} / ${filteredCards.size} 張", color = Color.Gray, fontSize = 14.sp)
                Text("下一張 →", color = Color.Gray, modifier = Modifier.clickable { if (currentIndex < filteredCards.size - 1) currentIndex++ })
            }
            Spacer(modifier = Modifier.height(16.dp))
            FlipCard(card = currentCard, onLearned = { if (!currentCard.isLearned) { currentCard.isLearned = true; onLearned() } })
        }
    }
}

@Composable
fun FlipCard(card: KnowledgeCard, onLearned: () -> Unit) {
    var flipped by remember { mutableStateOf(false) }
    var showQuiz by remember { mutableStateOf(false) }
    var isCollected by remember { mutableStateOf(card.isCollected) }
    var localIsLearned by remember { mutableStateOf(card.isLearned) }

    LaunchedEffect(card.id) { flipped = false; showQuiz = false; isCollected = card.isCollected; localIsLearned = card.isLearned }
    val rotation by animateFloatAsState(targetValue = if (flipped) 180f else 0f, animationSpec = tween(600, easing = FastOutSlowInEasing))

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).aspectRatio(0.82f).graphicsLayer { rotationY = rotation; cameraDistance = 12 * density }.clickable { flipped = !flipped }) {
        if (rotation <= 90f) {
            CardFace(card, false, { isCollected = !isCollected }, {}, isCollected, localIsLearned)
        } else {
            CardFace(card, true, {}, { if (card.quiz != null) showQuiz = true else { localIsLearned = true; onLearned() } }, isCollected, localIsLearned)
        }
    }
    if (showQuiz && card.quiz != null) {
        QuizDialog(quiz = card.quiz, onDismiss = { showQuiz = false }, onCorrect = { localIsLearned = true; onLearned() })
    }
}

@Composable
fun CardFace(card: KnowledgeCard, isBack: Boolean, onCollectClick: () -> Unit, onLearnClick: () -> Unit, isCollected: Boolean, isLearned: Boolean) {
    Surface(
        modifier = Modifier.fillMaxSize().graphicsLayer { if (isBack) rotationY = 180f },
        color = Color(0xFF121A21),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF2979FF).copy(alpha = 0.6f), Color(0xFF00E5FF).copy(alpha = 0.2f))))
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            if (!isBack) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFFE53935).copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFE53935).copy(alpha = 0.4f))) {
                        Text(card.level, color = Color(0xFFFF8A80), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    IconButton(onClick = onCollectClick) {
                        Icon(if (isCollected) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = null, tint = if (isCollected) Color(0xFFFFD54F) else Color.Gray)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(card.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, style = TextStyle(shadow = Shadow(Color(0xFF2979FF), blurRadius = 15f)))
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = highlightKeywords(card.content), textAlign = TextAlign.Center, lineHeight = 26.sp, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { card.tags.forEach { tag -> Text("#$tag", color = Color(0xFF448AFF), fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
                }
                Text("點擊看破詐騙陷阱", color = Color(0xFF2979FF).copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("防禦對策", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(card.detectionMethod, color = Color.LightGray, fontSize = 16.sp, lineHeight = 30.sp, textAlign = TextAlign.Center) }
                if (!isLearned) {
                    Button(onClick = onLearnClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A237E)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("我學會了", color = Color.White, fontWeight = FontWeight.Bold) }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("已存入防禦資料庫", color = Color(0xFF00C853), fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

fun highlightKeywords(text: String) = buildAnnotatedString {
    val parts = text.split("**")
    parts.forEachIndexed { index, part ->
        if (index % 2 == 1) { withStyle(style = SpanStyle(color = Color(0xFFFF1744), fontWeight = FontWeight.ExtraBold, shadow = Shadow(Color(0xFFFF1744).copy(alpha = 0.5f), blurRadius = 8f))) { append(part) } }
        else { withStyle(style = SpanStyle(color = Color(0xFFCFD8DC))) { append(part) } }
    }
}

@Composable
fun QuizDialog(quiz: Quiz, onDismiss: () -> Unit, onCorrect: () -> Unit) {
    var selectedOption by remember { mutableIntStateOf(-1) }
    var isAnswered by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Color(0xFF0A0E14),
        title = { Text("防禦測試", color = Color(0xFF448AFF), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(quiz.question, color = Color.White, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(20.dp))
                quiz.options.forEachIndexed { index, option ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { if (!isAnswered) selectedOption = index },
                        color = when { isAnswered && index == quiz.correctAnswerIndex -> Color(0xFF00C853).copy(alpha = 0.15f); isAnswered && index == selectedOption -> Color(0xFFFF1744).copy(alpha = 0.15f); selectedOption == index -> Color(0xFF1A237E).copy(alpha = 0.6f); else -> Color(0xFF121A21) },
                        shape = RoundedCornerShape(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedOption == index) Color(0xFF2979FF) else Color.White.copy(alpha = 0.1f))
                    ) { Text(option, color = Color.White, modifier = Modifier.padding(14.dp), fontSize = 14.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (!isAnswered) { isAnswered = true; if (selectedOption == quiz.correctAnswerIndex) onCorrect() } else onDismiss() }, enabled = selectedOption != -1) { Text(if (isAnswered) "確定" else "檢查答案", color = Color(0xFF448AFF), fontWeight = FontWeight.Bold) } }
    )
}

@Composable
fun RiskDashboardTab(learningProgress: Int) {
    val stats = DashboardStats(highRiskMessages = 3, interceptedCount = 12, learningProgress = learningProgress, reportedCases = 4, typeDistribution = emptyList())
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScamTicker(listOf("[警報] 實時掃描啟動中...", "[數據] 今日已成功攔截 2,415 筆威脅", "[系統] 第 4 層防禦已開啟"))
        Spacer(modifier = Modifier.height(32.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) { AnimatedStatCard(Modifier.weight(1f), "3", "本月高風險訊息", Color(0xFFFF1744)); Spacer(modifier = Modifier.width(12.dp)); AnimatedStatCard(Modifier.weight(1f), "12", "成功攔截總數", Color(0xFF00C853)) }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) { AnimatedStatCard(Modifier.weight(1f), "$learningProgress%", "知識等級", Color(0xFF2979FF)); Spacer(modifier = Modifier.width(12.dp)); AnimatedStatCard(Modifier.weight(1f), "4", "已回報案例", Color(0xFF448AFF)) }
            Spacer(modifier = Modifier.height(40.dp))
            Text("詐騙類型分佈", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(20.dp))
            val scamTypes = listOf(ScamTypeRatio("投資詐騙", 30, Color(0xFFFF1744)), ScamTypeRatio("假冒客服", 20, Color(0xFFFF9100)), ScamTypeRatio("愛情詐騙", 15, Color(0xFFD500F9)), ScamTypeRatio("網購詐騙", 15, Color(0xFF2979FF)), ScamTypeRatio("公家機關", 10, Color(0xFF00E5FF)), ScamTypeRatio("釣魚簡訊", 10, Color(0xFF00C853)))
            scamTypes.forEach { NeonProgressBar(it); Spacer(modifier = Modifier.height(16.dp)) }
            Spacer(modifier = Modifier.height(32.dp))
            EmergencyActionSection()
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun AnimatedStatCard(modifier: Modifier, value: String, label: String, color: Color) {
    Surface(modifier = modifier.aspectRatio(1.4f), color = Color(0xFF121A21), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Brush.linearGradient(listOf(color.copy(alpha = 0.4f), Color.Transparent)))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.Center) {
            Text(value, color = color, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, style = TextStyle(shadow = Shadow(color, blurRadius = 20f)))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun NeonProgressBar(ratio: ScamTypeRatio) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(ratio.label, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("${ratio.percentage}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
        Spacer(modifier = Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f))) { Box(modifier = Modifier.fillMaxWidth(ratio.percentage / 100f).fillMaxHeight().background(Brush.horizontalGradient(listOf(ratio.color, ratio.color.copy(alpha = 0.5f))))) }
    }
}

@Composable
fun ScamTicker(messages: List<String>) {
    val fullText = messages.joinToString("        ") { it }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = Color(0xFF448AFF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val textLayoutResult = textMeasurer.measure(fullText, textStyle)
    val textWidthPx = textLayoutResult.size.width.toFloat()
    var scrollOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(fullText) { while (true) { scrollOffset -= 0.8f; if (scrollOffset < -textWidthPx) scrollOffset = 0f; kotlinx.coroutines.delay(10) } }
    Surface(modifier = Modifier.fillMaxWidth().height(38.dp), color = Color(0xFF0D1520).copy(alpha = 0.8f), border = androidx.compose.foundation.BorderStroke(0.5.dp, Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF2979FF), Color.Transparent)))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawText(textLayoutResult = textLayoutResult, topLeft = Offset(scrollOffset, size.height / 2 - textLayoutResult.size.height / 2))
            drawText(textLayoutResult = textLayoutResult, topLeft = Offset(scrollOffset + textWidthPx, size.height / 2 - textLayoutResult.size.height / 2))
        }
    }
}

@Composable
fun EmergencyActionSection() {
    val context = LocalContext.current
    Button(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:165"))) }, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White); Spacer(modifier = Modifier.width(12.dp)); Text("緊急撥打：165 專線", color = Color.White, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp) }
}
