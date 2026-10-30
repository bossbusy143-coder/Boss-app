package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.verification.VerificationManager
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.AppBg
import com.example.ui.theme.AppCardBorder
import com.example.ui.theme.AppSurface
import com.example.ui.theme.AppSurfaceVariant
import com.example.ui.theme.AppTextMuted
import com.example.ui.theme.AppTextPrimary
import com.example.ui.theme.AppTextSecondary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import kotlinx.coroutines.launch

@Composable
fun VerificationRequiredScreen(
    onVerificationSuccess: () -> Unit,
    onOpenInAppVerification: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val verificationState by VerificationManager.verificationState.collectAsState()

    var isGeneratingLink by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var customApiKeyInput by remember { mutableStateOf("") }
    var showHowItWorks by remember { mutableStateOf(false) }

    // Automatically navigate to main content once valid pass is detected
    LaunchedEffect(verificationState.isValid) {
        if (verificationState.isValid) {
            onVerificationSuccess()
        }
    }

    // Prevent exiting to main content without passing verification
    BackHandler {
        // Can minimize app or show confirmation
        val activity = context as? android.app.Activity
        activity?.finish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // GyanSarthi App Icon with glowing status ring
        Box(
            modifier = Modifier
                .size(92.dp)
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
                    .size(80.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "GyanSarthi",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = AppTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Badge: Security Gate Status
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(RoseAccent.copy(alpha = 0.15f))
                .border(1.dp, RoseAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = RoseAccent,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Access Verification Required",
                color = RoseAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Verification Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AppSurface)
                .border(1.dp, AppCardBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Verify Your 24-Hour Access",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = AppTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "GyanSarthi uses AroLinks security verification to protect educational portals, video lectures, and batch materials from unauthorized bots.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = AppTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Feature Highlights
                VerificationBenefitRow(
                    icon = Icons.Default.Speed,
                    iconColor = CyanAccent,
                    title = "Single Quick Step",
                    description = "Takes less than 15 seconds to solve the link gate"
                )

                Spacer(modifier = Modifier.height(12.dp))

                VerificationBenefitRow(
                    icon = Icons.Default.Timer,
                    iconColor = AmberAccent,
                    title = "24-Hour Continuous Pass",
                    description = "Enjoy uninterrupted access to all batches without repeating verification"
                )

                Spacer(modifier = Modifier.height(12.dp))

                VerificationBenefitRow(
                    icon = Icons.Default.Security,
                    iconColor = EmeraldAccent,
                    title = "Secure & Automated",
                    description = "Returns you back to GyanSarthi automatically upon completion"
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Clock tamper alert banner if detected
        AnimatedVisibility(visible = verificationState.isClockTampered) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberAccent.copy(alpha = 0.15f))
                    .border(1.dp, AmberAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Device date or clock adjustment detected. Please tap 'Verify & Continue' to receive a fresh 24-hour access pass.",
                        color = AmberAccent,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error message banner if any
        AnimatedVisibility(visible = errorMessage != null) {
            errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoseAccent.copy(alpha = 0.12f))
                        .border(1.dp, RoseAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = error,
                            color = RoseAccent,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        if (error.contains("missing", ignoreCase = true) || error.contains("Token", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = { showConfigDialog = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text(
                                    text = "Configure Token",
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Primary Action: "Verify & Continue" Button
        Button(
            onClick = {
                errorMessage = null
                isGeneratingLink = true
                scope.launch {
                    val result = VerificationManager.generateVerificationLink(context)
                    isGeneratingLink = false
                    result.onSuccess { url ->
                        // Open in app's verified browser
                        onOpenInAppVerification(url)
                    }.onFailure { err ->
                        errorMessage = err.message ?: "Failed to generate verification link."
                    }
                }
            },
            enabled = !isGeneratingLink,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanAccent,
                contentColor = Color(0xFF0B0F19)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("verify_continue_button")
        ) {
            if (isGeneratingLink) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color(0xFF0B0F19),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Generating AroLinks Gate...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verify & Continue",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Action: Refresh / Check Pass
        OutlinedButton(
            onClick = {
                val isVerified = VerificationManager.checkVerification(context)
                if (isVerified) {
                    Toast.makeText(context, "Verification verified! Welcome to GyanSarthi.", Toast.LENGTH_SHORT).show()
                    onVerificationSuccess()
                } else if (verificationState.isClockTampered) {
                    Toast.makeText(context, "Device clock modification detected. Please tap 'Verify & Continue' to get a new 24-hour pass.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "24-hour pass has expired. Please tap 'Verify & Continue'.", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AppCardBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AppTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("check_verification_status_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Check Verification Status",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // How it works toggle
        TextButton(
            onClick = { showHowItWorks = !showHowItWorks }
        ) {
            Icon(
                imageVector = Icons.Default.HelpOutline,
                contentDescription = null,
                tint = AppTextMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (showHowItWorks) "Hide Instructions" else "How does verification work?",
                color = AppTextMuted,
                fontSize = 13.sp
            )
        }

        AnimatedVisibility(visible = showHowItWorks) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppSurfaceVariant)
                    .border(1.dp, AppCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Step-by-Step Instructions:",
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Click 'Verify & Continue' above to open the AroLinks gate.\n" +
                                "2. Complete the short link steps on the verification page.\n" +
                                "3. The page will automatically redirect and return you to GyanSarthi.\n" +
                                "4. Your temporary signed pass is issued for 24 hours of unlimited usage.",
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    // Token configuration dialog (for custom key or testing)
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            containerColor = AppSurface,
            title = {
                Text(
                    text = "AroLinks API Token",
                    color = AppTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your AroLinks API token or configure AROLINKS_API_KEY in the Secrets panel in AI Studio.",
                        color = AppTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customApiKeyInput,
                        onValueChange = { customApiKeyInput = it },
                        label = { Text("AroLinks API Token") },
                        placeholder = { Text("e.g. 5d8a9e2...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customApiKeyInput.isNotBlank()) {
                            VerificationManager.saveAroLinksApiKeyOverride(context, customApiKeyInput)
                            Toast.makeText(context, "API token saved. Tap Verify & Continue.", Toast.LENGTH_SHORT).show()
                            showConfigDialog = false
                            errorMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF0B0F19))
                ) {
                    Text("Save Token")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel", color = AppTextMuted)
                }
            }
        )
    }
}

@Composable
private fun VerificationBenefitRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = AppTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = AppTextMuted,
                lineHeight = 16.sp
            )
        }
    }
}
