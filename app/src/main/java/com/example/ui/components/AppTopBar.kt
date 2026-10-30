package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AppBg
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.ThemeManager

@Composable
fun AppTopBar(
    title: String = "GyanSarthi",
    subtitle: String? = "Education & Platform Hub",
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    showAdminAction: Boolean = false,
    onAdminClick: () -> Unit = {},
    showThemeToggle: Boolean = true,
    isDarkTheme: Boolean = ThemeManager.isDarkTheme,
    onToggleTheme: () -> Unit = {
        // default toggle
    }
) {
    val context = LocalContext.current
    val handleThemeToggle = if (onToggleTheme == {}) {
        { ThemeManager.toggleTheme(context) }
    } else {
        onToggleTheme
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (canNavigateBack) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("top_bar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            // App Logo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(CyanAccent, IndigoAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.gyansarthi_icon),
                    contentDescription = "GyanSarthi Logo",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = AppTextPrimary
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AppTextMuted,
                        fontSize = 12.sp
                    )
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Theme Mode Switcher Toggle Button placed right next to the Admin Login Panel
            if (showThemeToggle) {
                IconButton(
                    onClick = handleThemeToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("top_bar_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                        tint = if (isDarkTheme) AmberAccent else CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (showAdminAction) {
                IconButton(
                    onClick = onAdminClick,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("top_bar_admin_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Panel",
                        tint = CyanAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
