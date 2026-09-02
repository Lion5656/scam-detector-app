package com.example.scamdetectorapp.util

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*

/**
 * 全局通用的 Lottie 載入動畫組件
 */
@Composable
fun LottieLoadingView(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("loading.lottie")
    )
    
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
