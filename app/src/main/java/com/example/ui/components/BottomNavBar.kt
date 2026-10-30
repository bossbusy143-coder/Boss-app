package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.theme.AppCyanAccent
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavItem(
            route = Screen.Home.route,
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            testTag = "nav_item_home"
        ),
        NavItem(
            route = Screen.Platforms.route,
            label = "Platforms",
            selectedIcon = Icons.Filled.Apps,
            unselectedIcon = Icons.Outlined.Apps,
            testTag = "nav_item_platforms"
        ),
        NavItem(
            route = Screen.Upcoming.route,
            label = "Upcoming",
            selectedIcon = Icons.Filled.Schedule,
            unselectedIcon = Icons.Outlined.Schedule,
            testTag = "nav_item_upcoming"
        ),
        NavItem(
            route = Screen.Settings.route,
            label = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            testTag = "nav_item_settings"
        )
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        containerColor = AppSurface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                modifier = Modifier.testTag(item.testTag),
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route) {
                        onNavigate(item.route)
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (isSelected) AppCyanAccent else AppTextMuted
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        color = if (isSelected) AppCyanAccent else AppTextMuted
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AppSurfaceVariant,
                    selectedIconColor = AppCyanAccent,
                    unselectedIconColor = AppTextMuted,
                    selectedTextColor = AppCyanAccent,
                    unselectedTextColor = AppTextMuted
                )
            )
        }
    }
}
