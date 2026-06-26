package com.example.scamdetectorapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.colorResource
import com.example.scamdetectorapp.data.SettingsManager
import com.example.scamdetectorapp.presentation.screens.home.MainAppScreen
import com.example.scamdetectorapp.presentation.screens.splash.SplashScreen
import com.example.scamdetectorapp.service.MonitorService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 啟動自動防護服務 (若設定為開啟)
        startMonitorServiceIfEnabled()

        setContent {
            ScamGuardTheme {
                AppEntry()
            }
        }
    }

    private fun startMonitorServiceIfEnabled() {
        val settingsManager = SettingsManager(this)
        if (settingsManager.isProtectionEnabled) {
            val intent = Intent(this, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }
}

// ==================== 1. 主題設定 (讀取 XML Colors) ====================
@Composable
fun ScamGuardTheme(content: @Composable () -> Unit) {
    // 從 res/values/colors.xml 讀取顏色
    val darkBackground = colorResource(id = R.color.scam_background)
    val surfaceCard = colorResource(id = R.color.scam_surface)
    val primaryBlue = colorResource(id = R.color.scam_primary)
    val textWhite = colorResource(id = R.color.scam_text_white)

    // 設定 Material Theme 配色
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = darkBackground,
            surface = surfaceCard,
            primary = primaryBlue,
            onBackground = textWhite,
            onSurface = textWhite
        )
    ) {
        content()
    }
}

// ==================== 2. App 入口與閃屏頁邏輯 ====================
@Composable
fun AppEntry() {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
    } else {
        MainAppScreen()
    }
}
