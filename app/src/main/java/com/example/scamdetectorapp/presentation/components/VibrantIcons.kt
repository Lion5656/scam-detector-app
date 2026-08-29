package com.example.scamdetectorapp.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp


object VibrantIcons {

    // 1. 網址檢測：瀏覽器視窗 + 網址列。
    val WebDetection: ImageVector = ImageVector.Builder(
        name = "WebDetection",
        defaultWidth = 64.dp,
        defaultHeight = 64.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath( // 視窗外框（亮藍主體）
        pathData = addPathNodes(
            "M4.5 2.5h15a3 3 0 0 1 3 3v13a3 3 0 0 1-3 3h-15a3 3 0 0 1-3-3v-13a3 3 0 0 1 3-3z"
        ),
        fill = SolidColor(Color(0xFF00A6ED))
    ).addPath( // 上方工具列（深藍陰影）
        pathData = addPathNodes("M4.5 2.5h15a3 3 0 0 1 3 3v3h-21v-3a3 3 0 0 1 3-3z"),
        fill = SolidColor(Color(0xFF0074BA))
    ).addPath( // 關閉鈕（紅）
        pathData = addPathNodes("M4.6 4.65a.85.85 0 1 0 0 1.7a.85.85 0 1 0 0-1.7z"),
        fill = SolidColor(Color(0xFFF92F60))
    ).addPath( // 最小化鈕（黃）
        pathData = addPathNodes("M7.1 4.65a.85.85 0 1 0 0 1.7a.85.85 0 1 0 0-1.7z"),
        fill = SolidColor(Color(0xFFF9C23C))
    ).addPath( // 最大化鈕（綠）
        pathData = addPathNodes("M9.6 4.65a.85.85 0 1 0 0 1.7a.85.85 0 1 0 0-1.7z"),
        fill = SolidColor(Color(0xFF00D26A))
    ).addPath( // 網址列
        pathData = addPathNodes("M13.4 4.4h6.3a1.1 1.1 0 0 1 0 2.2h-6.3a1.1 1.1 0 0 1 0-2.2z"),
        fill = SolidColor(Color(0xFFF3F5F7))
    ).addPath( // 網址列內：網域
        pathData = addPathNodes("M13.8 5.1h3.5v.8h-3.5z"),
        fill = SolidColor(Color(0xFF0074BA))
    ).addPath( // 網址列內：路徑
        pathData = addPathNodes("M17.9 5.1h1.9v.8h-1.9z"),
        fill = SolidColor(Color(0xFFA8D8F0))
    ).addPath( // 頁面
        pathData = addPathNodes(
            "M4.5 10h15a1.5 1.5 0 0 1 1.5 1.5v6.5a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 18v-6.5A1.5 1.5 0 0 1 4.5 10z"
        ),
        fill = SolidColor(Color(0xFFF3F5F7))
    ).addPath( // 頁面標題行
        pathData = addPathNodes("M5 11.8h9.5v1.7h-9.5z"),
        fill = SolidColor(Color(0xFF0074BA))
    ).addPath( // 頁面內文行
        pathData = addPathNodes("M5 15h13.5v1.3h-13.5zM5 17.2h9.5v1.3h-9.5z"),
        fill = SolidColor(Color(0xFF9DCFEA))
    ).build()

    // 2. 電話檢測：經典座機話筒 + 撥號鍵盤
    val PhoneDetection: ImageVector = ImageVector.Builder(
        name = "PhoneDetection",
        defaultWidth = 64.dp,
        defaultHeight = 64.dp,
        viewportWidth = 32f,
        viewportHeight = 32f
    ).addPath( // 話筒
        pathData = addPathNodes(
            "m29.961 14.1l-.33-1.47A7.18 7.18 0 0 0 22.621 7H9.38a7.2 7.2 0 0 0-7.021 5.63l-.32 1.47c-.22.97.52 1.9 1.52 1.9h5.62c.76 0 1.37-.61 1.37-1.37v-.7c0-.52.42-.94.94-.94H20.5c.52 0 .94.42.94.94v.7c0 .76.61 1.37 1.37 1.37h5.621c1.01 0 1.75-.93 1.53-1.9"
        ),
        fill = SolidColor(Color(0xFFCA0B4A))
    ).addPath( // 話筒兩端
        pathData = addPathNodes(
            "M3 14a1 1 0 1 0 0 2h6a1 1 0 1 0 0-2zm20 0a1 1 0 1 0 0 2h6a1 1 0 1 0 0-2z"
        ),
        fill = SolidColor(Color(0xFF990838))
    ).addPath( // 機身
        pathData = addPathNodes(
            "m28.12 19.5l-3.66-4.88c-.29-.39-.75-.62-1.24-.62h-.91c-.17 0-.31-.14-.31-.31v-2.38c0-.17-.14-.31-.31-.31h-1.38c-.17 0-.31.14-.31.31v2.38c0 .17-.14.31-.31.31h-7.38c-.17 0-.31-.14-.31-.31v-2.38c0-.17-.14-.31-.31-.31h-1.38c-.17 0-.31.14-.31.31v2.38c0 .17-.14.31-.31.31h-.91c-.49 0-.95.23-1.25.63L3.88 19.5A9.37 9.37 0 0 0 2 25.13v3.31C2 29.3 2.7 30 3.56 30h24.88c.86 0 1.56-.7 1.56-1.56v-3.31c0-2.03-.66-4.01-1.88-5.63"
        ),
        fill = SolidColor(Color(0xFFF92F60))
    ).addPath( // 機身底部陰影
        pathData = addPathNodes(
            "M28.44 27H3.56C2.7 27 2 26.36 2 25.5v2.94C2 29.3 2.7 30 3.56 30h24.88c.86 0 1.56-.7 1.56-1.56V25.5c0 .86-.7 1.5-1.56 1.5"
        ),
        fill = SolidColor(Color(0xFF990838))
    ).addPath( // 撥號按鍵（九宮格）
        pathData = addPathNodes(
            "M13.698 19h-1.376a.315.315 0 0 1-.322-.312v-1.376c0-.171.14-.312.312-.312h1.376c.17 0 .311.14.311.312v1.376a.3.3 0 0 1-.3.312m3 0h-1.376a.315.315 0 0 1-.322-.312v-1.376c0-.171.14-.312.312-.312h1.376c.17 0 .312.14.312.312v1.376a.3.3 0 0 1-.302.312m1.624 0h1.376a.3.3 0 0 0 .302-.312v-1.376a.314.314 0 0 0-.312-.312h-1.376a.313.313 0 0 0-.312.312v1.376c0 .171.14.312.322.312m-4.624 3h-1.376a.315.315 0 0 1-.322-.312v-1.376c0-.171.14-.312.312-.312h1.376c.17 0 .311.14.311.312v1.376a.3.3 0 0 1-.3.312m1.624 0H16.7a.3.3 0 0 0 .302-.312v-1.376A.314.314 0 0 0 16.69 20h-1.376a.313.313 0 0 0-.312.312v1.376c0 .171.14.312.322.312m4.376 0h-1.376a.315.315 0 0 1-.322-.312v-1.376c0-.171.14-.312.311-.312h1.377c.17 0 .312.14.312.312v1.376A.3.3 0 0 1 19.7 22m-7.376 3H13.7c.17 0 .311-.14.301-.312v-1.376A.313.313 0 0 0 13.69 23h-1.376a.313.313 0 0 0-.312.312v1.376c0 .171.14.312.322.312m4.376 0h-1.376a.315.315 0 0 1-.322-.312v-1.376c0-.171.14-.312.312-.312h1.376c.17 0 .312.14.312.312v1.376A.3.3 0 0 1 16.7 25m1.624 0H19.7a.3.3 0 0 0 .302-.312v-1.376A.314.314 0 0 0 19.69 23h-1.376a.313.313 0 0 0-.312.312v1.376c0 .171.14.312.322.312"
        ),
        fill = SolidColor(Color(0xFFE6E6E6))
    ).build()

    // 3. 簡訊檢測：方形對話框 + 左下尖尾 + 三行訊息文字
    val SmsDetection: ImageVector = ImageVector.Builder(
        name = "SmsDetection",
        defaultWidth = 64.dp,
        defaultHeight = 64.dp,
        viewportWidth = 32f,
        viewportHeight = 32f
    ).addPath( // 對話框主體（亮綠）
        pathData = addPathNodes(
            "M6.5 3h19A4.5 4.5 0 0 1 30 7.5v12a4.5 4.5 0 0 1-4.5 4.5H8l-6 5.5V7.5A4.5 4.5 0 0 1 6.5 3z"
        ),
        fill = SolidColor(Color(0xFF00D26A))
    ).addPath( // 內縮細邊框（沿對話框輪廓內縮 1.5，含尖尾）
        pathData = addPathNodes(
            "M6.5 4.5h19A3 3 0 0 1 28.5 7.5v12a3 3 0 0 1-3 3H7.414L3.5 26.09V7.5A3 3 0 0 1 6.5 4.5z"
        ),
        stroke = SolidColor(Color(0xFF00B159)),
        strokeLineWidth = 0.9f
    ).addPath( // 三行訊息文字（同色，長度遞減）
        pathData = addPathNodes(
            "M7.4 7h17.2a1.4 1.4 0 0 1 0 2.8H7.4a1.4 1.4 0 0 1 0-2.8zM7.4 12h12.2a1.4 1.4 0 0 1 0 2.8H7.4a1.4 1.4 0 0 1 0-2.8zM7.4 17h7.2a1.4 1.4 0 0 1 0 2.8H7.4a1.4 1.4 0 0 1 0-2.8z"
        ),
        fill = SolidColor(Color(0xFFDADDFF))
    ).build()

    // 4. 購物檢測：購物提袋。
    val PriceDetection: ImageVector = ImageVector.Builder(
        name = "PriceDetection",
        defaultWidth = 64.dp,
        defaultHeight = 64.dp,
        viewportWidth = 32f,
        viewportHeight = 32f
    ).addPath(
        pathData = addPathNodes(
            "M28.61 30H16.43c-.78 0-1.42-.632-1.42-1.424V15.424c0-.782.63-1.424 1.42-1.424h12.18c.78 0 1.42.632 1.42 1.424v13.162A1.42 1.42 0 0 1 28.61 30"
        ),
        fill = SolidColor(Color(0xFFF9C23C))
    ).addPath(
        pathData = addPathNodes(
            "M17.563 16.031a.5.5 0 0 1 .5.5v2.684c0 2.035 1.94 3.8 4.484 3.8c2.553 0 4.484-1.764 4.484-3.8V16.53a.5.5 0 0 1 1 0v2.684c0 2.721-2.522 4.8-5.484 4.8c-2.95 0-5.485-2.078-5.485-4.8V16.53a.5.5 0 0 1 .5-.5"
        ),
        fill = SolidColor(Color(0xFFFF6723))
    ).addPath(
        pathData = addPathNodes(
            "M18.13 27.966H3.73c-.95 0-1.73-.77-1.73-1.73V9.726c0-.95.77-1.73 1.73-1.73h14.4c.95 0 1.73.77 1.73 1.73v16.52c0 .95-.77 1.72-1.73 1.72"
        ),
        fill = SolidColor(Color(0xFF00A6ED))
    ).addPath(
        pathData = addPathNodes(
            "M11.016 2C7.746 2 5 4.375 5 7.425V10.5a.5.5 0 0 0 1 0V7.425C6 5.031 8.189 3 11.016 3s5.015 2.031 5.015 4.425V10.5a.5.5 0 0 0 1 0V7.425c0-3.05-2.746-5.425-6.015-5.425M2 14.69h17.86v2.33H2zm0 4.49h17.86v2.33H2zm17.86 4.49H2V26h17.86z"
        ),
        fill = SolidColor(Color(0xFF0074BA))
    ).build()
}
