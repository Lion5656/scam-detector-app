package com.example.scamdetectorapp.presentation.screens.home

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.DataThresholding
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.data.SettingsManager
import com.example.scamdetectorapp.data.repository.NewsRepository
import com.example.scamdetectorapp.data.repository.NewsType
import com.example.scamdetectorapp.service.MonitorService

/**
 * 首頁螢幕組件
 * 提供應用程式的主要入口，讓使用者選擇不同的檢測功能，並查看最新的防詐新聞預覽。
 *
 * @param onNavigateTo 導覽回呼函式，傳入目標頁面名稱
 */
@Composable
fun HomeScreen(onNavigateTo: (String) -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    // 基礎權限請求發射器 (電話、通知)
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // 基礎權限拿到後，接著引導特殊權限
            handleSpecialPermissions(context)
        } else {
            Log.d("HomeScreen", "部分基礎權限被拒絕")
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // 讓首頁可以捲動，以容納下方的新聞預覽
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 頂部 Logo 區域 ---
        Spacer(modifier = Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(surfaceColor, CircleShape)
                .border(2.dp, primaryColor.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = "Logo",
                tint = primaryColor,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SCAM GUARD",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = onSurfaceColor
        )
        Text(
            text = "您的全方位防詐護盾",
            fontSize = 14.sp,
            color = colorResource(R.color.scam_text_grey)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- 主動防護 ---
        Text(
            text = "主動防護",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = onSurfaceColor,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 使用新的 ProtectionFeatureCard 處理啟用狀態
        var isProtectionEnabled by remember { mutableStateOf(settingsManager.isProtectionEnabled) }
        ProtectionFeatureCard(
            title = if (isProtectionEnabled) "即時防護中" else "防護未啟動",
            desc = "通話中檢測，敏感操作防護",
            icon = if (isProtectionEnabled) Icons.Outlined.VerifiedUser else Icons.Outlined.Shield,
            isEnabled = isProtectionEnabled,
            onCheckedChange = { checked ->
                if (checked) {
                    // 開啟防護時，觸發權限導覽
                    val permissions = mutableListOf(Manifest.permission.READ_PHONE_STATE)
                    
                    // ANSWER_PHONE_CALLS 需要 API 26 (Android 8.0) 以上
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    
                    val needsBasePermissions = permissions.any {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (needsBasePermissions) {
                        permissionsLauncher.launch(permissions.toTypedArray())
                    } else {
                        // 如果基礎權限已有，檢查特殊權限
                        handleSpecialPermissions(context)
                    }
                }

                isProtectionEnabled = checked
                settingsManager.isProtectionEnabled = checked // 1. 儲存設定到 SharedPreferences
                
                if (checked) {
                    // 2. 如果使用者開啟開關，立即啟動背景服務以獲得前景豁免
                    val intent = Intent(context, MonitorService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    // 3. 如果使用者關閉開關，立即停止背景服務
                    val intent = Intent(context, MonitorService::class.java)
                    context.stopService(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- 功能選擇區域 ---
        Text(
            text = "快速檢測",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = onSurfaceColor,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        FeatureCard(
            title = "網址檢測",
            desc = "檢查釣魚網站與惡意連結",
            icon = Icons.Outlined.Public,
            onClick = { onNavigateTo("網址") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureCard(
            title = "電話檢測",
            desc = "辨識騷擾與詐騙來電",
            icon = Icons.Outlined.Phone,
            onClick = { onNavigateTo("電話") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureCard(
            title = "簡訊檢測",
            desc = "分析可疑簡訊內容",
            icon = Icons.AutoMirrored.Outlined.Message,
            onClick = { onNavigateTo("簡訊") }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // --- 防詐新聞預覽區塊 ---
        NewsPreviewSection(onClick = { onNavigateTo("新聞") })
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 具有開關狀態的主動防護卡片
 */
@Composable
private fun ProtectionFeatureCard(
    title: String,
    desc: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val safeGreen = colorResource(R.color.scam_safe_green)
    val textGrey = colorResource(R.color.scam_text_grey)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        (if (isEnabled) safeGreen else primaryColor).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isEnabled) safeGreen else primaryColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = textGrey
                )
            }

            // 右側開關狀態
            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = safeGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = textGrey.copy(alpha = 0.5f)
                )
            )
        }
    }
}

/**
 * 功能卡片組件
 */
@Composable
private fun FeatureCard(
    title: String,
    desc: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = desc, fontSize = 12.sp, color = colorResource(R.color.scam_text_grey))
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = colorResource(R.color.scam_text_grey)
            )
        }
    }
}

/**
 * 首頁底部的新聞預覽區塊
 */
@Composable
private fun NewsPreviewSection(onClick: () -> Unit) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val textGrey = colorResource(R.color.scam_text_grey)

    // 修改：從共用 Repository 取得前兩則新聞
    val previewNews = NewsRepository.getPreviewNews()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "防詐資訊專區",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor
            )
            TextButton(onClick = onClick) {
                Text("查看更多", color = primaryColor, fontSize = 14.sp)
            }
        }

        // 根據取得的資料動態渲染卡片
        previewNews.forEachIndexed { index, news ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(10.dp))
            }

            Card(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                primaryColor.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 根據新聞類型顯示不同的圖示
                        val icon = if (news.type == NewsType.TREND) Icons.Default.DataThresholding else Icons.Default.Newspaper
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = news.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurfaceColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = news.summary,
                            fontSize = 13.sp,
                            color = textGrey,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 處理特殊權限導覽 (懸浮窗、使用量存取)
 */
private fun handleSpecialPermissions(context: Context) {
    if (!Settings.canDrawOverlays(context)) {
        // 引導至懸浮窗設定
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    } else if (!hasUsageStatsPermission(context)) {
        // 引導至使用量存取設定
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        context.startActivity(intent)
    }
}

/**
 * 檢查是否有「使用量存取」權限
 */
private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = @Suppress("DEPRECATION") appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
