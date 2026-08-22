package com.example.scamdetectorapp.presentation.screens.detection

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.scamdetectorapp.R
import com.example.scamdetectorapp.domain.model.DetectionMode
import com.example.scamdetectorapp.presentation.components.DetectionModeTabs
import com.example.scamdetectorapp.presentation.viewmodel.MainViewModel
import com.example.scamdetectorapp.presentation.viewmodel.ScanUiState

/**
 * 定義畫面顯示的四個階段
 */
enum class ScreenStep { INPUT, SCANNING, RESULT, ERROR }

@Composable
fun GenericDetectionFlow(
    mode: DetectionMode,
    title: String,
    placeholder: String,
    desc: String,
    isMultiLine: Boolean = false,
    icon: Any = when (mode) {
        DetectionMode.URL -> Icons.Default.Language
        DetectionMode.PHONE -> Icons.Default.Call
        DetectionMode.TEXT -> Icons.AutoMirrored.Filled.Message
        DetectionMode.PRICE -> Icons.Default.PhotoCamera
    },
    accentColor: Color = Color(0xFFA78BFA),
    onNavigateToGenealogy: ((String) -> Unit)? = null,
    onSwitchMode: (DetectionMode) -> Unit = {},
    onExitFlow: () -> Unit = {}, // 新增退出回調
    viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(LocalContext.current.applicationContext as android.app.Application))
) {
    val maxChars = when (mode) {
        DetectionMode.TEXT -> 300
        DetectionMode.URL -> 100
        DetectionMode.PHONE -> 20
        DetectionMode.PRICE -> 100 
    }
    val stateFlow = viewModel.getState(mode)
    val uiState by stateFlow.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current


    // Handle System Back Button
    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            uiState is ScanUiState.Success || uiState is ScanUiState.Error -> {
                viewModel.resetState(mode)
                viewModel.setInput(mode, "")
            }
            uiState is ScanUiState.Loading -> {
                viewModel.resetState(mode)
                viewModel.setInput(mode, "")
            }
            else -> {
                viewModel.resetState(mode)
                viewModel.setInput(mode, "")
                onExitFlow()
            }
        }
    }

    var step by remember(mode) {
        val initialStep = when (stateFlow.value) {
            is ScanUiState.Loading -> ScreenStep.SCANNING
            is ScanUiState.Success -> ScreenStep.RESULT
            is ScanUiState.Error -> ScreenStep.ERROR
            else -> ScreenStep.INPUT
        }
        mutableStateOf(initialStep)
    }

    val viewModelInput by viewModel.getInput(mode).collectAsStateWithLifecycle()

    // 使用 TextFieldValue 替代 String，以支援實體鍵盤中文組合輸入
    var localTextValue by remember(mode) { mutableStateOf(TextFieldValue(viewModelInput)) }

    // 監聽全域輸入狀態，確保 reset 時 UI 同步清空
    LaunchedEffect(viewModelInput) {
        if (localTextValue.text != viewModelInput) {
            localTextValue = TextFieldValue(viewModelInput)
        }
    }

    // 當分頁切換（mode 改變）或元件銷毀時，確保收起鍵盤
    DisposableEffect(mode) {
        onDispose {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            is ScanUiState.Loading -> step = ScreenStep.SCANNING
            is ScanUiState.Success -> step = ScreenStep.RESULT
            is ScanUiState.Error -> step = ScreenStep.ERROR
            is ScanUiState.Idle -> {
                if (step != ScreenStep.INPUT) step = ScreenStep.INPUT
            }
        }
    }

    fun startScan() {
        focusManager.clearFocus()
        keyboardController?.hide()
        val input = if (mode == DetectionMode.PRICE) viewModelInput else localTextValue.text
        viewModel.scan(mode, input)
    }

    fun reset() {
        viewModel.resetState(mode)
        viewModel.setInput(mode, "")
        localTextValue = TextFieldValue("")
        step = ScreenStep.INPUT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(com.example.scamdetectorapp.ui.theme.AppBackgroundBrush)
    ) {
        AnimatedVisibility(visible = step == ScreenStep.INPUT, enter = fadeIn(), exit = fadeOut()) {
            if (mode == DetectionMode.PRICE) {
                    PriceInputScreen(
                        title = title,
                        desc = desc,
                        imageUri = if (viewModelInput.startsWith("uri:")) viewModelInput.removePrefix("uri:") else null,
                        onImageSelected = { uri ->
                            if (uri.isEmpty()) viewModel.setInput(mode, "")
                            else viewModel.setInput(mode, "uri:$uri")
                        },
                        onScan = { startScan() },
                        currentMode = mode,
                        onSwitchMode = onSwitchMode,
                        icon = icon,
                        accentColor = accentColor,
                        onExit = { 
                            reset()
                            onExitFlow() 
                        }
                    )
            } else {
                InputScreen(
                    title = title,
                    desc = desc,
                    placeholder = placeholder,
                    icon = icon,
                    accentColor = accentColor,
                    value = localTextValue,
                    onValueChange = { newValue ->
                        // 字數攔截邏輯
                        if (newValue.text.length <= maxChars) {
                            localTextValue = newValue
                            if (newValue.text != viewModelInput) {
                                viewModel.setInput(mode, newValue.text)
                            }
                        }
                    },
                    onScan = { if (localTextValue.text.isNotBlank()) startScan() },
                    keyboardType = when (mode) {
                        DetectionMode.URL -> KeyboardType.Uri
                        DetectionMode.PHONE -> KeyboardType.Number
                        DetectionMode.TEXT -> KeyboardType.Text
                        else -> KeyboardType.Text
                    },
                    isMultiLine = isMultiLine,
                    currentMode = mode,
                    onSwitchMode = onSwitchMode,
                    onExit = { 
                        reset()
                        onExitFlow() 
                    }
                )
            }
        }

        AnimatedVisibility(visible = step == ScreenStep.SCANNING, enter = fadeIn(), exit = fadeOut()) {
            ScanningScreen(onCancel = { viewModel.resetState(mode) })
        }

        AnimatedVisibility(visible = step == ScreenStep.RESULT, enter = fadeIn(), exit = fadeOut()) {
            if (uiState is ScanUiState.Success) {
                val result = (uiState as ScanUiState.Success).result
                if (mode == DetectionMode.PRICE) {
                    PriceResultScreen(
                        result = result,
                        onBack = { reset() }
                    )
                } else {
                    FraudResultScreen(
                        originalText = localTextValue.text,
                        result = result,
                        onBack = { reset() },
                        onViewGenealogy = if (mode == DetectionMode.PHONE) {
                            { onNavigateToGenealogy?.invoke(localTextValue.text) }
                        } else null
                    )
                }
            }
        }

        AnimatedVisibility(visible = step == ScreenStep.ERROR, enter = fadeIn(), exit = fadeOut()) {
            if (uiState is ScanUiState.Error) {
                ErrorScreen(
                    title = (uiState as ScanUiState.Error).title,
                    message = (uiState as ScanUiState.Error).message,
                    onBack = { reset() }
                )
            }
        }
    }
}

@Composable
fun ErrorScreen(title: String, message: String, onBack: () -> Unit) {
    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // 頂部間距
        Spacer(modifier = Modifier.height(60.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back_less_than),
                    contentDescription = "返回輸入", 
                    tint = textWhite, 
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text("檢測失敗", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = textWhite)
        }
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text(message, color = textGrey, textAlign = TextAlign.Center)
            }
        }
        
        Spacer(modifier = Modifier.height(60.dp)) // 增加間距將按鈕往上提
        
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = surfaceColor)
        ) {
            Text("返回", color = textWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

    }
}

@Composable
fun InputScreen(
    title: String,
    desc: String,
    placeholder: String,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onScan: () -> Unit,
    keyboardType: KeyboardType,
    isMultiLine: Boolean,
    icon: Any = Icons.Default.Search,
    accentColor: Color = Color(0xFFA78BFA),
    currentMode: DetectionMode = DetectionMode.URL,
    onSwitchMode: (DetectionMode) -> Unit = {},
    onExit: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)
    // 面版顏色：比背景稍微亮一點點，營造軟質表面感
    val panelColor = Color(0xFF1E2630) 
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 頂部間距
            Spacer(modifier = Modifier.height(60.dp))

            // 頂部導覽列與返回鍵
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back_less_than),
                        contentDescription = "返回", 
                        tint = textWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "開始檢測",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textWhite
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            com.example.scamdetectorapp.presentation.components.DetectionModeTabs(
                selected = currentMode,
                onSelect = onSwitchMode,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp)) // icon 與上方膠囊標籤的間距

            // 有機 Squircle 面版
            Surface(
                color = panelColor,
                shape = RoundedCornerShape(36.dp), // 大圓角營造 Squircle 有機感
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 將圖示整合進面版頂部
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (icon) {
                            is androidx.compose.ui.graphics.vector.ImageVector -> {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            is androidx.compose.ui.graphics.painter.Painter -> {
                                Image(
                                    painter = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                            // 處理以 Int 傳入的資源 ID
                            is Int -> {
                                Image(
                                    painter = androidx.compose.ui.res.painterResource(icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textWhite)
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // 無框輸入框
                    TextField(
                        value = value,
                        onValueChange = onValueChange,
                        placeholder = { 
                            Text(
                                placeholder, 
                                color = textGrey.copy(alpha = 0.6f),
                                textAlign = if (isMultiLine) TextAlign.Start else TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) 
                        },
                        trailingIcon = {
                            if (value.text.isNotEmpty()) {
                                IconButton(onClick = { onValueChange(TextFieldValue("")) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清空內容",
                                        tint = textGrey.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (isMultiLine) 120.dp else 56.dp, max = if (isMultiLine) 160.dp else 56.dp)
                            .focusRequester(focusRequester),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent, // 移除底線
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = textWhite,
                            unfocusedTextColor = textWhite,
                            cursorColor = accentColor
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = if (isMultiLine) TextAlign.Start else TextAlign.Center,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = if(isMultiLine) ImeAction.Default else ImeAction.Done,
                            autoCorrect = true
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (value.text.isNotBlank()) onScan()
                                else {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        singleLine = !isMultiLine
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        // 底部操作按鈕區域：固定在底部
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onScan,
                enabled = value.text.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor, 
                    disabledContainerColor = panelColor
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("開始檢測", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(32.dp))
                }
            }
            
            // 增加與 PriceInputScreen 對齊的間距 (補足下方提示文字的高度)
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun PriceInputScreen(
    title: String,
    desc: String,
    imageUri: String?,
    onImageSelected: (String) -> Unit,
    onScan: () -> Unit,
    currentMode: DetectionMode = DetectionMode.PRICE,
    onSwitchMode: (DetectionMode) -> Unit = {},
    icon: Any = R.drawable.ic_solid_shopping_v2,
    accentColor: Color = Color(0xFFA78BFA),
    onExit: () -> Unit = {}
) {
    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)
    val panelColor = Color(0xFF1E2630) 
    var showSourceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null){
            val contentType = context.contentResolver.getType(uri)
            val isSupported = contentType in listOf("image/jepg", "image/png", "image/webp")
            if (isSupported){
                onImageSelected(uri.toString())
            } else {
                Toast.makeText(context, "不支援的檔案格式，請選擇 JPG, PNG 或 WEBP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onImageSelected("bitmap_placeholder")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 頂部間距
            Spacer(modifier = Modifier.height(60.dp))

            // 頂部導覽列與返回鍵
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back_less_than),
                        contentDescription = "返回", 
                        tint = textWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "開始檢測",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textWhite
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            DetectionModeTabs(
                selected = currentMode,
                onSelect = onSwitchMode,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 有機 Squircle 面版
            Surface(
                color = panelColor,
                shape = RoundedCornerShape(36.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 將圖示整合進面版頂部
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (icon) {
                            is Int -> {
                                Image(
                                    painter = painterResource(icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                            is ImageVector -> {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            is Painter -> {
                                Image(
                                    painter = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title, 
                            fontSize = 22.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = textWhite
                        )
                        if (imageUri != null) {
                            IconButton(
                                onClick = { onImageSelected("") },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "清空圖片",
                                    tint = textGrey.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Image Upload Area (內嵌在面版中)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 300.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { showSourceDialog = true },
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (imageUri == null) {
                                        Modifier.drawBehind {
                                            val stroke = Stroke(
                                                width = 1.5.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                            )
                                            drawRoundRect(
                                                color = accentColor.copy(alpha = 0.3f),
                                                style = stroke,
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx())
                                            )
                                        }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CameraAlt, 
                                        contentDescription = null, 
                                        tint = accentColor.copy(alpha = 0.6f), 
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text("點擊拍照或上傳圖片", color = textGrey, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        // 底部操作按鈕區域：固定在底部
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onScan,
                enabled = imageUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    disabledContainerColor = panelColor
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("開始檢測", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.width(32.dp))
                }
            }

            Text(
                "圖片分析可能需要 10~30 秒，請耐心等候",
                color = textGrey,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            containerColor = panelColor,
            shape = RoundedCornerShape(32.dp),
            title = { 
                Text(
                    "選擇圖片來源", 
                    color = textWhite, 
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) 
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = {
                            showSourceDialog = false
                            cameraLauncher.launch(null)
                        },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("開啟相機拍照", color = textWhite, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Surface(
                        onClick = {
                            showSourceDialog = false
                            galleryLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("從相簿挑選", color = textWhite, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showSourceDialog = false },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text("取消", color = textGrey, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ScanningScreen(onCancel: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textGrey = colorResource(R.color.scam_text_grey)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(120.dp),
                color = primaryColor,
                strokeWidth = 8.dp
            )
            Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor, modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("正在分析威脅...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text("正在掃描", fontSize = 14.sp, color = textGrey, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(48.dp))
        TextButton(onClick = onCancel) {
            Text("取消", color = textGrey)
        }
    }
}
