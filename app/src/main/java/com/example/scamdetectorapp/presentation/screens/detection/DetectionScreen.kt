package com.example.scamdetectorapp.presentation.screens.detection

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ContentPaste
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
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
    viewModel: MainViewModel = viewModel(factory = MainViewModel.provideFactory(LocalContext.current.applicationContext as android.app.Application))
) {
    val stateFlow = viewModel.getState(mode)
    val uiState by stateFlow.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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

    // 【關鍵修正】使用 TextFieldValue 替代 String，以支援實體鍵盤中文組合輸入
    var localTextValue by remember(mode) { mutableStateOf(TextFieldValue(viewModelInput)) }

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
        viewModel.scan(mode, localTextValue.text)
    }

    fun reset() {
        viewModel.setInput(mode, "")
        localTextValue = TextFieldValue("")
        viewModel.resetState(mode)
        step = ScreenStep.INPUT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // 點擊背景收起鍵盤
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                })
            }
    ) {
        AnimatedVisibility(visible = step == ScreenStep.INPUT, enter = fadeIn(), exit = fadeOut()) {
            if (mode == DetectionMode.PRICE) {
                PriceInputScreen(
                    title = title,
                    desc = desc,
                    imageUri = if (viewModelInput.startsWith("uri:")) viewModelInput.removePrefix("uri:") else null,
                    onImageSelected = { uri ->
                        viewModel.setInput(mode, "uri:$uri")
                    },
                    onScan = { startScan() }
                )
            } else {
                InputScreen(
                    title = title,
                    desc = desc,
                    placeholder = placeholder,
                    value = localTextValue,
                    onValueChange = {
                        localTextValue = it
                        viewModel.setInput(mode, it.text)
                    },
                    onScan = { if (localTextValue.text.isNotBlank()) startScan() },
                    keyboardType = when (mode) {
                        DetectionMode.URL -> KeyboardType.Uri
                        DetectionMode.PHONE -> KeyboardType.Number
                        DetectionMode.TEXT -> KeyboardType.Text
                    },
                    isMultiLine = isMultiLine
                )
            }
        }

        AnimatedVisibility(visible = step == ScreenStep.SCANNING, enter = fadeIn(), exit = fadeOut()) {
            ScanningScreen(onCancel = { viewModel.resetState(mode) })
        }

        AnimatedVisibility(visible = step == ScreenStep.RESULT, enter = fadeIn(), exit = fadeOut()) {
            if (uiState is ScanUiState.Success) {
                FraudResultScreen(
                    originalText = localTextValue.text,
                    result = (uiState as ScanUiState.Success).result,
                    onBack = { reset() }
                )
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
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = textWhite)
            }
            Spacer(Modifier.width(8.dp))
            Text("檢測失敗", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textWhite)
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
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = surfaceColor)
        ) {
            Text("返回", color = textWhite)
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
    isMultiLine: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background
    val focusRequester = remember { FocusRequester() }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textWhite)
        Spacer(modifier = Modifier.height(8.dp))
        Text(desc, color = textGrey, fontSize = 14.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = { Text(placeholder, color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isMultiLine) 150.dp else 60.dp)
                        .focusRequester(focusRequester),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = textWhite,
                        unfocusedTextColor = textWhite,
                        focusedContainerColor = backgroundColor,
                        unfocusedContainerColor = backgroundColor,
                        disabledContainerColor = backgroundColor,
                        errorContainerColor = backgroundColor,
                    ),
                    shape = RoundedCornerShape(12.dp),
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

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        clipboardManager.getText()?.text?.let { onValueChange(TextFieldValue(it)) }
                    },
                    modifier = Modifier.align(Alignment.End),
                    border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor)
                ) {
                    Icon(Icons.Outlined.ContentPaste, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("貼上內容", color = primaryColor)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onScan,
            enabled = value.text.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                disabledContainerColor = surfaceColor
            )
        ) {
            Text("立即檢測", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PriceInputScreen(
    title: String,
    desc: String,
    imageUri: String?,
    onImageSelected: (String) -> Unit,
    onScan: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textWhite = MaterialTheme.colorScheme.onBackground
    val textGrey = colorResource(R.color.scam_text_grey)
    val surfaceColor = MaterialTheme.colorScheme.surface

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onImageSelected(it.toString()) }
    }

    // Camera launcher placeholder - in real app would need a File provider
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // Simplified: in a real app, save bitmap to file and get URI
        if (bitmap != null) {
            onImageSelected("bitmap_placeholder")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textWhite
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = desc,
            color = textGrey,
            fontSize = 14.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Step 1: Upload Image (只有在沒圖片時顯示文字標題)
        if (imageUri == null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(primaryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text("上傳商品圖片 (OCR 自動辨識)", color = textWhite, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Image Upload Area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 250.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { 
                    galleryLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    ) 
                },
            color = surfaceColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (imageUri == null) {
                            Modifier.drawBehind {
                                val stroke = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                                drawRoundRect(
                                    color = primaryColor,
                                    style = stroke,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
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
                        contentScale = ContentScale.Fit, // 使用 Fit 以免裁剪到文字內容
                        onState = { state ->
                            if (state is coil.compose.AsyncImagePainter.State.Error) {
                                android.util.Log.e("Coil", "Load failed for URI: $imageUri", state.result.throwable)
                            }
                        }
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(surfaceColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = primaryColor, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("點擊拍照或上傳圖片", color = textWhite, fontWeight = FontWeight.Bold)
                        Text("支援 JPG / PNG 格式", color = textGrey, fontSize = 12.sp)
                        
                        Spacer(Modifier.height(24.dp))
                        Row {
                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, primaryColor),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("拍照上傳", color = primaryColor, fontSize = 14.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = { 
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    ) 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = BorderStroke(1.dp, primaryColor),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("從相簿選擇", color = primaryColor, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
        
        if (imageUri == null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = textGrey, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("請確保圖片清晰，包含商品、價格、賣場資訊等內容", color = textGrey, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onScan,
            enabled = imageUri != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryColor,
                disabledContainerColor = surfaceColor
            )
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("開始 AI 分析", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        
        Text(
            "AI 分析可能需要 10~30 秒，請耐心等候",
            color = textGrey,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
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
        Text("正在比對雲端詐騙資料庫", fontSize = 14.sp, color = textGrey, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(48.dp))
        TextButton(onClick = onCancel) {
            Text("取消", color = textGrey)
        }
    }
}
