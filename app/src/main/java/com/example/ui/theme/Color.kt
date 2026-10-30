package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fixed Dark Palette Colors
val DarkBg = Color(0xFF0B0F19)
val DarkSurface = Color(0xFF131B2E)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkCardBorder = Color(0xFF2E3D56)

// Fixed Light Palette Colors
val LightBg = Color(0xFFF1F5F9)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE2E8F0)
val LightCardBorder = Color(0xFFCBD5E1)

// Dynamic Colors (switch automatically between Dark and Light mode)
val AppBg: Color
    @Composable
    get() = if (ThemeManager.isDarkTheme) DarkBg else LightBg

val AppSurface: Color
    @Composable
    get() = if (ThemeManager.isDarkTheme) DarkSurface else LightSurface

val AppSurfaceVariant: Color
    @Composable
    get() = if (ThemeManager.isDarkTheme) DarkSurfaceVariant else LightSurfaceVariant

val AppCardBorder: Color
    @Composable
    get() = if (ThemeManager.isDarkTheme) DarkCardBorder else LightCardBorder

// Accent Colors
val CyanAccent = Color(0xFF38BDF8)
val CyanAccentDark = Color(0xFF0284C7)
val IndigoAccent = Color(0xFF818CF8)
val AmberAccent = Color(0xFFF59E0B)
val EmeraldAccent = Color(0xFF10B981)
val RoseAccent = Color(0xFFF43F5E)

val AppCyanAccent: Color
    @Composable
    get() = if (ThemeManager.isDarkTheme) CyanAccent else CyanAccentDark

// Text Colors
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF64748B)

val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF64748B)

val AppTextPrimary: Color
    @Composable
    get() = if (ThemeManager.isDarkTheme) DarkTextPrimary else LightTextPrimary

val AppTextSecondary: Color
    @Composable
    get() = if (ThemeManager.isDarkTheme) DarkTextSecondary else LightTextSecondary

val AppTextMuted: Color
    @Composable
    get() = if (ThemeManager.isDarkTheme) DarkTextMuted else LightTextMuted

// Backward compatible aliases
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

val CardGradientStart = Color(0xFF162036)
val CardGradientEnd = Color(0xFF111827)
