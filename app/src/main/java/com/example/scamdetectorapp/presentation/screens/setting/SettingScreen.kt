package com.example.scamdetectorapp.presentation.screens.setting

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scamdetectorapp.manager.PermissionStatus
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import com.example.scamdetectorapp.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(LocalContext.current.applicationContext as android.app.Application))
) {
    
    // 從 ViewModel 取得狀態
    val isContactsEnabled by viewModel.isContactsEnabled.collectAsStateWithLifecycle()
    val isShareAutoInputEnabled by viewModel.isShareAutoInputEnabled.collectAsStateWithLifecycle()
    val protectedApps by viewModel.protectedApps.collectAsStateWithLifecycle()
    val customWhitelist by viewModel.customWhitelist.collectAsStateWithLifecycle()
    val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val highlightPermissionCenter by viewModel.highlightPermissionCenter.collectAsStateWithLifecycle()

    val isCorePermissionsGranted = viewModel.hasDisplayPermissions()

    val isGuiding = highlightPermissionCenter && !isCorePermissionsGranted

    // 控制 Dialog 顯示
    var showAppManagement by remember { mutableStateOf(false) }
    var showWhitelist by remember { mutableStateOf(false) }
    var showPermissionCenter by remember { mutableStateOf(false) }

    // 自動開啟權限中心並監控權限狀態
    LaunchedEffect(highlightPermissionCenter, isCorePermissionsGranted) {
        if (highlightPermissionCenter && !isCorePermissionsGranted) {
            delay(500)
            showPermissionCenter = true
        } else if (isCorePermissionsGranted && highlightPermissionCenter) {
            // 一旦權限全開，就重置 ViewModel 狀態，關閉亮點
            viewModel.setHighlightPermission(false)
        }
    }

    // 當從系統設定回來時自動刷新狀態
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.updatePermissionStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val accentIndigo = IndigoBlue 
    val securityBlue1 = SystemBlue 
    val securityBlue2 = Color(0xFF4895EF) // Cornflower Blue
    val securityBlue3 = Color(0xFF4CC9F0) // Sky Blue
    val featuresOrange = Color(0xFFFB8500)
    val systemTeal = Color(0xFF2EC4B6)
    val aboutPink = Color(0xFFF72585)

    val topGap = 70.dp
    val bottomGap = 110.dp

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. 上方標題區
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(top = 16.dp, start = 16.dp)
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = com.example.scamdetectorapp.R.drawable.ic_back_less_than),
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(topGap))

                Text(
                    text = "設定",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 4.sp
                    )
                )

                Spacer(modifier = Modifier.height(bottomGap))
            }
        }

        // 2. 下方功能卡片區
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // --- 安全與隱私權群組 ---
                SettingHeader("安全與隱私權", Color.White)
                SettingsGroupCard {
                    SettingSwitchItemContent(
                        title = "讀取聯絡人資料",
                        subtitle = "自動信任聯絡人",
                        icon = Icons.Default.ContactPhone,
                        iconColor = securityBlue1,
                        switchColor = accentIndigo,
                        checked = isContactsEnabled,
                        onCheckedChange = { viewModel.toggleContactsEnabled(it) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                        thickness = 2.dp,
                        color = Color.White.copy(alpha = 0.08f)
                    )
                    SettingClickableItemContent(
                        title = "防護 App 管理",
                        subtitle = "目前防護 ${protectedApps.size} 個 App",
                        icon = Icons.Default.AppRegistration,
                        iconColor = securityBlue2,
                        onClick = { showAppManagement = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                        thickness = 2.dp,
                        color = Color.White.copy(alpha = 0.08f)
                    )
                    SettingClickableItemContent(
                        title = "自定義白名單",
                        subtitle = "管理信任的電話號碼",
                        icon = Icons.Default.FactCheck,
                        iconColor = securityBlue3,
                        onClick = { showWhitelist = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 功能操作群組 ---
                SettingHeader("快捷操作", Color.White)
                SettingsGroupCard {
                    SettingSwitchItemContent(
                        title = "分享後快速檢測",
                        subtitle = "接收分享內容時直接檢測",
                        icon = Icons.Default.Share,
                        iconColor = featuresOrange,
                        switchColor = accentIndigo,
                        checked = isShareAutoInputEnabled,
                        onCheckedChange = { viewModel.toggleShareAutoInputEnabled(it) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 系統權限群組 ---
                SettingHeader("系統權限", Color.White)
                
                SettingsGroupCard {
                    SettingClickableItemContent(
                        title = "權限狀態中心",
                        subtitle = "管理系統權限",
                        icon = Icons.Default.AdminPanelSettings,
                        iconColor = systemTeal,
                        onClick = { showPermissionCenter = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 關於群組 ---
                SettingHeader("關於", Color.White)
                SettingsGroupCard {
                    SettingInfoItemContent(
                        title = "應用版本",
                        value = "v1.0.0",
                        icon = Icons.Default.Info,
                        iconColor = aboutPink
                    )
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Dialogs
    if (showAppManagement) {
        AppManagementDialog(
            protectedApps = protectedApps,
            onUpdate = { viewModel.updateProtectedApps(it) },
            onDismiss = { showAppManagement = false }
        )
    }

    if (showWhitelist) {
        WhitelistDialog(
            whitelist = customWhitelist.toList(),
            onAdd = { viewModel.addWhitelistNumber(it) },
            onRemove = { viewModel.removeWhitelistNumber(it) },
            onDismiss = { showWhitelist = false }
        )
    }

    if (showPermissionCenter) {
        PermissionCenterDialog(
            status = permissionStatus,
            isGuiding = isGuiding,
            onUpdateStatus = { viewModel.updatePermissionStatus() },
            onDismiss = { showPermissionCenter = false }
        )
    }
}

@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White.copy(alpha = 0.08f),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            content = content
        )
    }
}

@Composable
fun SettingHeader(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            color = color,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingSwitchItemContent(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    switchColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon, iconColor)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = switchColor,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray
            )
        )
    }
}

@Composable
fun SettingClickableItemContent(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingIcon(icon, iconColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun SettingInfoItemContent(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon, iconColor)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun AppManagementDialog(
    protectedApps: Set<String>,
    onUpdate: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    // 取得所有有圖示的 App
    val allInstalledApps = remember {
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(intent, 0).map {
            it.activityInfo.applicationInfo
        }.distinctBy { it.packageName }.sortedBy { packageManager.getApplicationLabel(it).toString() }
    }

    // 搜尋關鍵字狀態
    var searchQuery by remember { mutableStateOf("") }
    
    // 根據關鍵字過濾後的清單
    val filteredApps = remember(searchQuery, allInstalledApps) {
        if (searchQuery.isBlank()) {
            allInstalledApps
        } else {
            allInstalledApps.filter {
                packageManager.getApplicationLabel(it).toString().contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            color = SurfaceDark,
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜尋", fontWeight = FontWeight.SemiBold, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    } else null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = IndigoBlue
                    ),
                    shape = CircleShape
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "已選擇 ${protectedApps.size} 個 App",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                
                if (filteredApps.isNotEmpty()) {
                    SettingsGroupCard {
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 350.dp)
                                .padding(horizontal = 12.dp)
                        ) {
                            itemsIndexed(filteredApps) { index, app ->
                                val isChecked = protectedApps.contains(app.packageName)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newList = protectedApps.toMutableSet()
                                            if (isChecked) newList.remove(app.packageName) else newList.add(app.packageName)
                                            onUpdate(newList)
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        bitmap = app.loadIcon(packageManager).toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = packageManager.getApplicationLabel(app).toString(),
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = app.packageName,
                                            color = Color.Gray,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = IndigoBlue
                                        )
                                    )
                                }

                                if (index < filteredApps.size - 1) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("找不到相符的 App", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun WhitelistDialog(
    whitelist: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newNumber by remember { mutableStateOf("") }

    val formatNumber = { input: String ->
        val digits = input.filter { it.isDigit() }.take(10)
        buildString {
            if (digits.startsWith("09")) {
                for (i in digits.indices) {
                    append(digits[i])
                    if (i == 3 && digits.length > 4) append(" ")
                    else if (i == 6 && digits.length > 7) append(" ")
                }
            } else if (digits.startsWith("0")) {
                val isLong = digits.length == 10
                for (i in digits.indices) {
                    append(digits[i])
                    if (i == 1 && digits.length > 2) append(" ")
                    else if (isLong && i == 5) append(" ")
                    else if (!isLong && i == 4 && digits.length > 5) append(" ")
                }
            } else {
                append(digits)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            color = SurfaceDark,
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                TextField(
                    value = newNumber,
                    onValueChange = { newNumber = it },
                    placeholder = { Text("輸入電話號碼", color = Color.Gray, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = TextStyle(color = Color.White),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (newNumber.isNotBlank()) {
                                onAdd(newNumber)
                                newNumber = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = IndigoBlue)
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = IndigoBlue
                    ),
                    shape = CircleShape
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "目前共有 ${whitelist.size} 筆號碼",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                if (whitelist.isNotEmpty()) {
                    SettingsGroupCard {
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 350.dp)
                                .padding(horizontal = 12.dp)
                        ) {
                            itemsIndexed(whitelist) { index, number ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = number,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    IconButton(onClick = { onRemove(number) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (index < whitelist.size - 1) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = Color.White.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("白名單目前為空", color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCenterDialog(
    status: PermissionStatus,
    isGuiding: Boolean = false,
    onUpdateStatus: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            color = SurfaceDark,
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PermissionItem(
                    title = "懸浮窗權限",
                    desc = "用於通話中顯示警告視窗",
                    isGranted = status.hasOverlay,
                    isHighlighted = isGuiding && !status.hasOverlay,
                    onClick = {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                )
                PermissionItem(
                    title = "使用量存取權限",
                    desc = "用於偵測目前開啟的 App",
                    isGranted = status.hasUsageStats,
                    isHighlighted = isGuiding && !status.hasUsageStats,
                    onClick = {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    }
                )
                PermissionItem(
                    title = "通話狀態權限",
                    desc = "用於偵測通話狀態",
                    isGranted = status.hasPhoneState,
                    isHighlighted = isGuiding && !status.hasPhoneState,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                )
                PermissionItem(
                    title = "通話紀錄權限",
                    desc = "用於通話中辨識來電號碼",
                    isGranted = status.hasCallLogs,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                )
                PermissionItem(
                    title = "聯絡人權限",
                    desc = "用於自動識別信任聯絡人",
                    isGranted = status.hasContacts,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String, 
    desc: String, 
    isGranted: Boolean, 
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val highlightAlpha = remember { Animatable(0.05f) }

    // 當引導標記觸發時，執行一次性的「點擊反白動畫」
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            // 快速閃爍並淡出，模擬點擊感
            highlightAlpha.animateTo(0.35f, tween(500))
            highlightAlpha.animateTo(0.05f, tween(1400))
        }
    }

    Surface(
        color = Color.White.copy(alpha = highlightAlpha.value),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(desc, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF10B981) else Color(0xFFF72585)
            )
        }
    }
}
