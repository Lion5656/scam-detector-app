package com.example.scamdetectorapp.presentation.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    // 載入 Lottie 組成
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("ECM - Nominal Case.lottie")
    )
    
    // 控制動畫播放進度
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1, // 只播一次
        speed = 1f
    )

    // 當動畫進度達到 1.0 (播完) 時，觸發進入主頁
    LaunchedEffect(progress) {
        if (progress == 1f) {
            delay(500) // 播完稍微停 0.5 秒讓畫面穩定
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06090E)),
        contentAlignment = Alignment.Center
    ) {
        // --- ECM 全息啟動動畫 ---
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier
                .size(320.dp) // <--- 【在此修改大小】原本是 fillMaxSize()，現在改為 320dp
                .offset(y = (-40).dp), // 稍微往上偏一點，留空間給下方文字
            contentScale = ContentScale.Fit
        )

        // 下方品牌文字
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SCAM GUARD",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "SYSTEM INITIALIZING...",
                color = Color(0xFF448AFF).copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}
