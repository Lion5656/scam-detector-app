package com.example.scamdetectorapp.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.example.scamdetectorapp.R

@Composable
fun CustomBottomBar(currentTab: String, onTabSelected: (String) -> Unit) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary
    val greyColor = colorResource(R.color.scam_text_grey)

    NavigationBar(
        containerColor = surfaceColor,
        tonalElevation = 8.dp
    ) {
        // 更新列表：加入「首頁」，「新聞」頁面不加入導覽列
        val items = listOf(
            Triple("首頁", Icons.Filled.Home, Icons.Outlined.Home),
            Triple("網址", Icons.Filled.Public, Icons.Outlined.Public),
            Triple("電話", Icons.Filled.Phone, Icons.Outlined.Phone),
            Triple("簡訊", Icons.AutoMirrored.Filled.Message, Icons.AutoMirrored.Outlined.Message),
            Triple("購物檢測", ImageVector.vectorResource(R.drawable.shopping_cart_filled_24dp),
                ImageVector.vectorResource(R.drawable.shopping_cart_24dp))
        )

        items.forEach { (title, selectedIcon, unselectedIcon) ->
            val isSelected = currentTab == title
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(title) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) selectedIcon else unselectedIcon,
                        contentDescription = title,
                        tint = if (isSelected) primaryColor else greyColor
                    )
                },
                label = {
                    Text(title, color = if (isSelected) primaryColor else greyColor)
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
