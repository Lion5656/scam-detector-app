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
import androidx.lifecycle.lifecycleScope
import com.example.scamdetectorapp.data.SettingsManager
import com.example.scamdetectorapp.presentation.model.SharedContent
import com.example.scamdetectorapp.presentation.model.SharedType
import com.example.scamdetectorapp.presentation.screens.home.MainAppScreen
import com.example.scamdetectorapp.presentation.screens.splash.SplashScreen
import com.example.scamdetectorapp.service.MonitorService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var sharedContentState by mutableStateOf<SharedContent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        // 啟動自動防護服務 (若設定為開啟)
        startMonitorServiceIfEnabled()

        setContent {
            ScamGuardTheme {
                AppEntry(sharedContent = sharedContentState, onSharedContentHandled = { sharedContentState = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val type = intent.type
            if (type?.startsWith("text/") == true) {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                    sharedContentState = SharedContent(SharedType.TEXT, it)
                }
            } else if (type?.startsWith("image/") == true) {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                uri?.let {
                    sharedContentState = SharedContent(SharedType.IMAGE, it.toString())
                }
            }
        }
    }

    private fun startMonitorServiceIfEnabled() {
        val settingsManager = SettingsManager(this)
        lifecycleScope.launch {
            // 取得第一筆資料後判斷是否啟動
            if (settingsManager.isProtectionEnabled.first()) {
                val intent = Intent(this@MainActivity, MonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
    }
}

// ==================== 1. 主題設定 (讀取 XML Colors) ====================
@Composable
fun ScamGuardTheme(content: @Composable () -> Unit) {
    // 從 res/values/colors.xml 讀取顏色：頁面／卡片／元件三層明度堆疊
    val darkBackground = colorResource(id = R.color.scam_background)
    val surfaceCard = colorResource(id = R.color.scam_surface)
    val surfaceComponent = colorResource(id = R.color.scam_component)
    val primaryBlue = colorResource(id = R.color.scam_primary)
    val textWhite = colorResource(id = R.color.scam_text_white)
    val textGrey = colorResource(id = R.color.scam_text_grey)
    val riskRed = colorResource(id = R.color.scam_risk_red)

    // 設定 Material Theme 配色
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = darkBackground,
            surface = surfaceCard,
            surfaceVariant = surfaceComponent,
            primary = primaryBlue,
            onBackground = textWhite,
            onSurface = textWhite,
            onSurfaceVariant = textGrey,
            error = riskRed
        )
    ) {
        content()
    }
}

// ==================== 2. App 入口與閃屏頁邏輯 ====================
@Composable
fun AppEntry(sharedContent: SharedContent?, onSharedContentHandled: () -> Unit) {
    var showSplash by remember { mutableStateOf(sharedContent == null) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
    } else {
        MainAppScreen(sharedContent = sharedContent, onSharedContentHandled = onSharedContentHandled)
    }
}
