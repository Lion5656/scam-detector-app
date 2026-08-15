package com.example.scamdetectorapp.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scamdetectorapp.R

/**
 * 導覽項配置
 */
private data class NavItemConfig(
    val title: String,
    val shortLabel: String,
    val unselectedIcon: ImageVector? = null,
    val selectedIcon: ImageVector? = null,
    val resId: Int? = null,
    val selectedResId: Int? = null
)

/**
 * 膠囊型懸浮導覽列 (高明度實心風格)
 * 導覽項：首頁、檢測紀錄、設定
 */
@Composable
fun CustomBottomBar(currentTab: String, onTabSelected: (String) -> Unit) {
    val backgroundColor = Color(0xFF0D1117)
    val activeColor = Color(0xFF9333EA) // 恢復為原始魅影紫
    val inactiveColor = Color(0xFF94A3B8) // 恢復為原始冷灰

    val items = remember {
        listOf(
            NavItemConfig(
                title = "首頁",
                shortLabel = "首頁",
                resId = R.drawable.proicons__home,
                selectedResId = R.drawable.proicons__home_filled
            ),
            NavItemConfig(
                title = "檢測紀錄",
                shortLabel = "紀錄",
                unselectedIcon = Icons.Outlined.History,
                selectedIcon = Icons.Filled.History
            ),
            NavItemConfig(
                title = "設定",
                shortLabel = "設定",
                unselectedIcon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 32.dp, end = 32.dp, bottom = 24.dp)
    ) {
        // 外層陰影與微弱霓虹光暈 (Cyber Violet)
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    shadowElevation = 20f
                    shape = RoundedCornerShape(100)
                    clip = false
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(activeColor.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset.Unspecified,
                        radius = 600f
                    ),
                    shape = RoundedCornerShape(100)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(100))
                .background(backgroundColor)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (item in items) {
                val isSelected = currentTab == item.title
                
                val icon = when {
                    isSelected && item.selectedIcon != null -> item.selectedIcon
                    !isSelected && item.unselectedIcon != null -> item.unselectedIcon
                    isSelected && item.selectedResId != null -> ImageVector.vectorResource(item.selectedResId)
                    item.resId != null -> ImageVector.vectorResource(item.resId)
                    else -> androidx.compose.material.icons.Icons.Default.Settings // Fallback
                }

                BottomNavItem(
                    title = item.title,
                    shortLabel = item.shortLabel,
                    icon = icon,
                    isSelected = isSelected,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onClick = { onTabSelected(item.title) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    title: String,
    shortLabel: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(300),
        label = "color"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = shortLabel,
            color = iconColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
