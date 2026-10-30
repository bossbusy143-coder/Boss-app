package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ArolinksVerificationScreen(
    onVerificationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var passcode by remember { mutableStateOf("") }
    var isFetchingLink by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var shortenedUrl by remember { mutableStateOf<String?>(null) }

    // In-App verification webview state
    var showInAppWebView by remember { mutableStateOf(false) }

    // Pre-sync remote config & generate initial link in background
    LaunchedEffect(Unit) {
        VerificationManager.syncRemoteConfig(context)
        isFetchingLink = true
        shortenedUrl = VerificationManager.prepareArolinksVerificationLink(context)
        isFetchingLink = false
    }

    // Auto-detect deep links or copied codes on app resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 1. Check if deep link unlocked the app
                if (VerificationManager.isVerified(context)) {
                    isSuccess = true
                    Toast.makeText(context, "Access Verified! Unlocking...", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        delay(600)
                        onVerificationSuccess()
                    }
                } else {
                    // 2. Auto-detect copied 6-digit code from clipboard
                    val autoUnlocked = VerificationManager.checkAndAutoUnlockFromClipboard(context)
                    if (autoUnlocked) {
                        isSuccess = true
                        Toast.makeText(context, "Copied code auto-verified! 24H unlocked.", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            delay(600)
                            onVerificationSuccess()
                        }
                    } else {
                        // Auto-fill clipboard if it contains 6 digits
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                        val match = clip?.let { Regex("\\b\\d{6}\\b").find(it)?.value }
                        if (!match.isNullOrBlank() && passcode.isBlank()) {
                            passcode = match
                            errorMessage = null
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val verifyCodeAction = {
        keyboardController?.hide()
        if (passcode.isBlank()) {
            errorMessage = "Please enter the 6-digit code received from the verification page."
        } else {
            isVerifying = true
            errorMessage = null
            scope.launch {
                delay(300)
                val success = VerificationManager.verifyPasscode(context, passcode)
                isVerifying = false
                if (success) {
                    isSuccess = true
                    Toast.makeText(context, "24-Hour Access Activated!", Toast.LENGTH_LONG).show()
                    delay(800)
                    onVerificationSuccess()
                } else {
                    errorMessage = "Invalid access code. Please complete Arolinks verification to view your 6-digit code."
                }
            }
        }
    }

    val openInExternalBrowserAction = {
        scope.launch {
            isFetchingLink = true
            val targetUrl = VerificationManager.prepareArolinksVerificationLink(context)
            shortenedUrl = targetUrl
            isFetchingLink = false
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Opening Arolinks... Code will auto-verify on return!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openInAppVerificationAction = {
        scope.launch {
            isFetchingLink = true
            val targetUrl = VerificationManager.prepareArolinksVerificationLink(context)
            shortenedUrl = targetUrl
            isFetchingLink = false
            showInAppWebView = true
        }
    }

    // In-App Browser Fullscreen Verification View
    if (showInAppWebView) {
        val target = shortenedUrl ?: VerificationManager.FALLBACK_SHORT_URL
        InAppArolinksBrowser(
            url = target,
            onDismiss = { showInAppWebView = false },
            onAutoUnlocked = {
                showInAppWebView = false
                isSuccess = true
                Toast.makeText(context, "Verification Complete! 24H Access Unlocked.", Toast.LENGTH_LONG).show()
                scope.launch {
                    delay(600)
                    onVerificationSuccess()
                }
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("arolinks_verification_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Glowing Lock Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                CyanAccent.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, CyanAccent.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = if (isSuccess) EmeraldAccent else CyanAccent,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Easy 24H Verification",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Arolinks verification complete karein aur app agle 24 ghante ke liye direct unlock ho jayega.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AppTextSecondary,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Card 1: Fast 1-Tap Verification
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppSurface)
                    .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Recommended Method",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AppTextPrimary
                                )
                            )
                            Text(
                                text = "Auto-unlocks as soon as you finish the shortener",
                                style = MaterialTheme.typography.bodySmall.copy(color = AppTextMuted, fontSize = 12.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Button: In-App Fast Verification
                    Button(
                        onClick = { openInAppVerificationAction() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("in_app_verify_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanAccent,
                            contentColor = DarkBg
                        )
                    ) {
                        if (isFetchingLink) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = DarkBg,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Loading Link...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Fast Verification",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Secondary Option: Open in External Chrome
                    OutlinedButton(
                        onClick = { openInExternalBrowserAction() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("browser_verify_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            tint = AppTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Or Open in Chrome / Browser",
                            color = AppTextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 2: Manual Passcode Box (with Auto-Paste & Auto-Detect)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppSurface)
                    .border(1.dp, AppCardBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Already have the 6-digit code?",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AppTextPrimary
                        )
                    )
                    Text(
                        text = "Agar aapne Chrome mein link open kiya hai, code copy karke yahan paste karein:",
                        style = MaterialTheme.typography.bodySmall.copy(color = AppTextMuted, fontSize = 12.sp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = passcode,
                        onValueChange = {
                            passcode = it.trim()
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passcode_input_field"),
                        placeholder = { Text("Enter 6-digit access code", color = AppTextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = CyanAccent
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                    val match = clip?.let { Regex("\\b\\d{6}\\b").find(it)?.value } ?: clip
                                    if (!match.isNullOrBlank()) {
                                        passcode = match
                                        errorMessage = null
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste",
                                    tint = CyanAccent
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { verifyCodeAction() }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextPrimary,
                            unfocusedTextColor = AppTextPrimary,
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = AppCardBorder,
                            focusedContainerColor = AppSurfaceVariant,
                            unfocusedContainerColor = AppSurfaceVariant
                        )
                    )

                    AnimatedVisibility(visible = errorMessage != null) {
                        errorMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg,
                                color = RoseAccent,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { verifyCodeAction() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("verify_unlock_btn"),
                        enabled = !isVerifying,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldAccent,
                            contentColor = DarkBg,
                            disabledContainerColor = EmeraldAccent.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = DarkBg,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying Code...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verify & Unlock (24 Hours)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notice Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppSurfaceVariant)
                    .border(1.dp, AppCardBorder.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AmberAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Auto-Detect Feature: Webpage se code copy karke GyanSarthi kholne par app apne aap code detect karke 24 ghante ke liye unlock ho jayega.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AppTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Embedded in-app browser for instant, seamless Arolinks verification.
 * Automatically catches destination URL completion and grants 24H verification.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InAppArolinksBrowser(
    url: String,
    onDismiss: () -> Unit,
    onAutoUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // In-app Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(DarkSurface)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Arolinks Quick Verification",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Auto-unlocks on 'Get Link'",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CyanAccent,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            IconButton(onClick = { webViewInstance?.reload() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = TextSecondary
                )
            }
        }

        if (isLoading && progress < 1f) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = CyanAccent,
                trackColor = Color.Transparent
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mediaPlaybackRequiresUserGesture = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = false
                            }
                        }

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                        }

                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                                isLoading = newProgress < 100
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                                isLoading = true
                                pageUrl?.let { currentUrl ->
                                    // Automatic detection of destination completion!
                                    if (VerificationManager.checkAndAutoUnlockFromUrl(context, currentUrl)) {
                                        onAutoUnlocked()
                                    }
                                }
                            }

                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                isLoading = false
                                pageUrl?.let { currentUrl ->
                                    if (VerificationManager.checkAndAutoUnlockFromUrl(context, currentUrl)) {
                                        onAutoUnlocked()
                                    }
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val destination = request?.url?.toString() ?: return false
                                // Catch completion or deep link
                                if (VerificationManager.checkAndAutoUnlockFromUrl(context, destination)) {
                                    onAutoUnlocked()
                                    return true
                                }
                                return false
                            }
                        }

                        loadUrl(url)
                        webViewInstance = this
                    }
                },
                onRelease = { wb ->
                    try {
                        (wb.parent as? ViewGroup)?.removeView(wb)
                        wb.stopLoading()
                        wb.destroy()
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
