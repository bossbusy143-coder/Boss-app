package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AppBg
import com.example.ui.theme.AppCardBorder
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThemeManager

@Composable
fun SettingsScreen(
    onNavigateToAdmin: () -> Unit,
    onNavigateToVerification: () -> Unit = {}
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(com.example.data.verification.VerificationManager.isVerified(context)) }
    var remainingTime by remember { mutableStateOf(com.example.data.verification.VerificationManager.getFormattedRemainingTime(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Settings & About",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = AppTextPrimary
            )
        )
        Text(
            text = "App configuration, legal details, and administrative controls",
            style = MaterialTheme.typography.bodySmall.copy(
                color = AppTextMuted,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 24-Hour Access Pass Card
        Text(
            text = "24-HOUR ACCESS PASS",
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppSurface)
                .border(1.dp, if (isVerified) EmeraldAccent.copy(alpha = 0.5f) else AmberAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(16.dp)
                .testTag("settings_verification_card")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isVerified) Icons.Default.Security else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isVerified) EmeraldAccent else AmberAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isVerified) "Arolinks 24H Pass: Active" else "Arolinks 24H Pass: Expired",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AppTextPrimary
                            )
                        )
                        Text(
                            text = if (isVerified) "Remaining access: $remainingTime" else "Complete verification to unlock app",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isVerified) EmeraldAccent else RoseAccent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToVerification,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("settings_renew_pass_btn"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = DarkBg
                        )
                    ) {
                        Text(
                            text = if (isVerified) "Renew Pass" else "Unlock Access",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            com.example.data.verification.VerificationManager.resetVerification(context)
                            isVerified = false
                            remainingTime = "Expired"
                            Toast.makeText(context, "Verification reset. App locked.", Toast.LENGTH_SHORT).show()
                            onNavigateToVerification()
                        },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("settings_reset_pass_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Relock", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // App Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppSurface)
                .border(1.dp, AppCardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GyanSarthi",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTextPrimary
                        )
                    )
                    Text(
                        text = "Version 1.0.0 (Production Release)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CyanAccent
                        )
                    )
                    Text(
                        text = "Powered by Google Cloud Firestore Backend",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppTextMuted
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Appearance & Theme Section
        Text(
            text = "APPEARANCE & THEME",
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppSurface)
                .border(1.dp, AppCardBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
                .testTag("settings_theme_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (ThemeManager.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = if (ThemeManager.isDarkTheme) AmberAccent else CyanAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (ThemeManager.isDarkTheme) "Dark Theme" else "Light Theme",
                            color = AppTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (ThemeManager.isDarkTheme) "Deep slate cyber aesthetic enabled" else "Crisp modern light aesthetic enabled",
                            color = AppTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Switch(
                    checked = !ThemeManager.isDarkTheme, // ON = Light Theme, OFF = Dark Theme
                    onCheckedChange = { isLight ->
                        ThemeManager.setTheme(context, !isLight)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppSurface,
                        checkedTrackColor = CyanAccent,
                        uncheckedThumbColor = AppTextMuted,
                        uncheckedTrackColor = AppSurfaceVariant
                    ),
                    modifier = Modifier.testTag("settings_theme_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Administration Section
        Text(
            text = "ADMINISTRATION",
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItemRow(
            icon = Icons.Default.AdminPanelSettings,
            title = "Admin Portal & Management",
            subtitle = "Add platforms, edit URLs, upload banners, & manage catalog",
            iconTint = CyanAccent,
            onClick = onNavigateToAdmin,
            testTag = "settings_admin_portal_button"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Storage Section
        Text(
            text = "STORAGE & DATA",
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItemRow(
            icon = Icons.Default.CleaningServices,
            title = "Clear In-App Browser Data",
            subtitle = "Remove saved cookies, session cache, and website storage",
            iconTint = RoseAccent,
            onClick = {
                try {
                    CookieManager.getInstance().removeAllCookies(null)
                    WebStorage.getInstance().deleteAllData()
                    Toast.makeText(context, "Browser cache and cookies cleared successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                }
            },
            testTag = "settings_clear_cache_button"
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Information & Legal
        Text(
            text = "LEGAL & CONTACT",
            color = CyanAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsItemRow(
            icon = Icons.Default.Info,
            title = "About GyanSarthi",
            subtitle = "Purpose, platform catalog model, and disclaimer",
            onClick = { showAboutDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsItemRow(
            icon = Icons.Default.Policy,
            title = "Privacy Policy",
            subtitle = "Data processing, zero-credential storage policy",
            onClick = { showPrivacyDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsItemRow(
            icon = Icons.Default.Description,
            title = "Terms of Service",
            subtitle = "Authorized catalog usage and safety guidelines",
            onClick = { showTermsDialog = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingsItemRow(
            icon = Icons.Default.Email,
            title = "Contact Administrator",
            subtitle = "support@gyansarthi.edu / Report an issue",
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:support@gyansarthi.edu")
                    putExtra(Intent.EXTRA_SUBJECT, "GyanSarthi Platform Inquiry")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Contact: support@gyansarthi.edu", Toast.LENGTH_LONG).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showAboutDialog) {
        LegalDialog(
            title = "About GyanSarthi",
            content = "GyanSarthi is a dynamic education platform launcher and catalog hub. It organizes premier educational portals, lectures, and academic batch rooms in a unified, accessible interface.\n\nGyanSarthi connects in real-time to Cloud Firestore, allowing new authorized platforms to be discovered and managed dynamically by administrators without requiring client APK updates.",
            onDismiss = { showAboutDialog = false }
        )
    }

    if (showPrivacyDialog) {
        LegalDialog(
            title = "Privacy Policy",
            content = "GyanSarthi does not harvest, store, or sell user personal browsing history or student credentials. When you open an educational platform via the in-app browser or external browser, your interactions occur directly with that platform's server in accordance with standard web protocols.\n\nAll cookies and cache remain on your device and can be cleared at any time via Settings.",
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showTermsDialog) {
        LegalDialog(
            title = "Terms of Service",
            content = "1. GyanSarthi functions solely as an indexing launcher and catalog for educational resources.\n2. The app does not bypass authentication, paywalls, DRM, or website security.\n3. Distribution of APKs and links is limited to platforms authorized by the platform administrator.\n4. Users agree to use platform resources in compliance with respective institution rules.",
            onDismiss = { showTermsDialog = false }
        )
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color = CyanAccent,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurface)
            .border(1.dp, AppCardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = AppTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = AppTextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun LegalDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Text(
                text = title,
                color = AppTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = content,
                color = AppTextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkBg)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}
