package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private const val TAG = "WebViewScreen"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    title: String = "",
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var reloadKey by remember { mutableIntStateOf(0) }

    // Fullscreen video state
    var isCustomViewShowing by remember { mutableStateOf(false) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var fullscreenContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var originalOrientation by remember { mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) }

    val exitFullscreen: () -> Unit = {
        val activity = context as? Activity
        val window = activity?.window
        val decorView = window?.decorView as? ViewGroup

        fullscreenContainer?.let { container ->
            container.removeAllViews()
            decorView?.removeView(container)
        }
        fullscreenContainer = null
        customView = null

        if (window != null && decorView != null) {
            val insetsController = WindowCompat.getInsetsController(window, decorView)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        if (activity != null && originalOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            activity.requestedOrientation = originalOrientation
        }

        try {
            customViewCallback?.onCustomViewHidden()
        } catch (e: Exception) {
            Log.w(TAG, "Error invoking onCustomViewHidden: ${e.message}")
        }
        customViewCallback = null
        isCustomViewShowing = false
    }

    val showFullscreenView: (View?, WebChromeClient.CustomViewCallback?, Int) -> Unit = { view, callback, targetOrientation ->
        val activity = context as? Activity
        val window = activity?.window
        val decorView = window?.decorView as? ViewGroup

        if (activity != null && window != null && decorView != null && view != null) {
            if (customView != null) {
                exitFullscreen()
            }

            customView = view
            customViewCallback = callback
            originalOrientation = activity.requestedOrientation

            val container = FrameLayout(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.BLACK)
                (view.parent as? ViewGroup)?.removeView(view)
                addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                    )
                )
            }
            fullscreenContainer = container

            decorView.addView(
                container,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )

            // Hide system bars for true immersive fullscreen video
            val insetsController = WindowCompat.getInsetsController(window, decorView)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Prevent screen dimming while video is playing in fullscreen
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Auto-switch to landscape for optimal widescreen video playback
            activity.requestedOrientation = targetOrientation
            isCustomViewShowing = true
        }
    }


    // Hardware and gesture back navigation
    BackHandler {
        if (isCustomViewShowing) {
            exitFullscreen()
        } else {
            val wb = webViewInstance
            if (wb != null && wb.canGoBack()) {
                wb.goBack()
            } else {
                onClose()
            }
        }
    }

    // Lifecycle observer to pause/resume video playback when app is backgrounded/foregrounded
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webViewInstance?.onPause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webViewInstance?.onResume()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("webview_screen")
    ) {
        // App Bar Header with navigation, title, refresh, browser launcher, and close
        if (!isCustomViewShowing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(DarkSurface)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val wb = webViewInstance
                        if (wb != null && wb.canGoBack()) {
                            wb.goBack()
                        } else {
                            onClose()
                        }
                    },
                    modifier = Modifier.testTag("webview_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = title.ifBlank { "GyanSarthi" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "GyanSarthi",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = CyanAccent,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = {
                        if (hasError) {
                            hasError = false
                            isLoading = true
                            progress = 0f
                            reloadKey++
                        } else {
                            webViewInstance?.reload()
                        }
                    },
                    modifier = Modifier.testTag("webview_reload_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = TextSecondary
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("webview_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted
                    )
                }
            }
        }

        // Progress Indicator
        if (isLoading && progress < 1.0f && !hasError) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = CyanAccent,
                trackColor = Color.Transparent
            )
        }

        // Main Web / Error Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (!hasError) {
                key(reloadKey) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
                                isVerticalScrollBarEnabled = false
                                isHorizontalScrollBarEnabled = false

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    // Require user gesture for media playback to prevent resource exhaustion and renderer crashes
                                    mediaPlaybackRequiresUserGesture = true
                                    javaScriptCanOpenWindowsAutomatically = false
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        safeBrowsingEnabled = false
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    }
                                }

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    cookieManager.setAcceptThirdPartyCookies(this, true)
                                }

                                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                                    handleFileDownload(
                                        context = ctx,
                                        url = downloadUrl,
                                        userAgent = userAgent,
                                        contentDisposition = contentDisposition,
                                        mimetype = mimetype
                                    )
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress / 100f
                                        isLoading = newProgress < 100
                                    }

                                    override fun getDefaultVideoPoster(): Bitmap? {
                                        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                                    }

                                    override fun getVideoLoadingProgressView(): View {
                                        return FrameLayout(ctx).apply {
                                            layoutParams = ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                            addView(
                                                ProgressBar(ctx),
                                                FrameLayout.LayoutParams(
                                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                                    Gravity.CENTER
                                                )
                                            )
                                        }
                                    }

                                    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                        showFullscreenView(view, callback, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
                                    }

                                    override fun onShowCustomView(
                                        view: View?,
                                        requestedOrientation: Int,
                                        callback: CustomViewCallback?
                                    ) {
                                        val targetOrientation = if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                                            requestedOrientation
                                        } else {
                                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        }
                                        showFullscreenView(view, callback, targetOrientation)
                                    }

                                    override fun onHideCustomView() {
                                        exitFullscreen()
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                                        isLoading = true
                                        hasError = false
                                    }

                                    override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                        isLoading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            hasError = true
                                            errorMessage = error?.description?.toString() ?: "Failed to connect to platform server."
                                        }
                                    }

                                    override fun onReceivedHttpError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        errorResponse: WebResourceResponse?
                                    ) {
                                        if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                                            if (errorResponse?.statusCode in 401..403) {
                                                hasError = true
                                                errorMessage = "This portal requires authentication (HTTP ${errorResponse?.statusCode})."
                                            }
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val destination = request?.url?.toString() ?: return false
                                        val lower = destination.lowercase()

                                        // Intercept direct downloadable documents and video files
                                        val isDownloadable = lower.endsWith(".pdf") ||
                                                lower.contains(".pdf?") ||
                                                lower.endsWith(".mp4") ||
                                                lower.contains(".mp4?") ||
                                                lower.endsWith(".mkv") ||
                                                lower.contains(".mkv?") ||
                                                lower.endsWith(".webm") ||
                                                lower.contains(".webm?") ||
                                                lower.endsWith(".apk") ||
                                                lower.endsWith(".zip") ||
                                                lower.endsWith(".docx") ||
                                                lower.endsWith(".xlsx") ||
                                                lower.contains("download=true") ||
                                                lower.contains("download=1") ||
                                                lower.contains("action=download")

                                        if (isDownloadable) {
                                            handleFileDownload(
                                                context = ctx,
                                                url = destination,
                                                userAgent = settings.userAgentString,
                                                contentDisposition = null,
                                                mimetype = when {
                                                    lower.contains(".pdf") -> "application/pdf"
                                                    lower.contains(".mp4") -> "video/mp4"
                                                    lower.contains(".mkv") -> "video/x-matroska"
                                                    lower.contains(".webm") -> "video/webm"
                                                    lower.contains(".apk") -> "application/vnd.android.package-archive"
                                                    lower.contains(".zip") -> "application/zip"
                                                    else -> null
                                                }
                                            )
                                            return true
                                        }

                                        if (destination.startsWith("http://") || destination.startsWith("https://")) {
                                            return false // Keep web links inside WebView
                                        }

                                        // Handle intent:, market:, mailto:, tel:
                                        try {
                                            val intent = if (destination.startsWith("intent:")) {
                                                Intent.parseUri(destination, Intent.URI_INTENT_SCHEME)
                                            } else {
                                                Intent(Intent.ACTION_VIEW, Uri.parse(destination))
                                            }
                                            ctx.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.w(TAG, "Cannot launch external uri: $destination")
                                        }
                                        return true
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        val didCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            detail?.didCrash() == true
                                        } else false
                                        Log.e(TAG, "onRenderProcessGone called: didCrash=$didCrash")

                                        // Immediately switch state so Compose safely unmounts AndroidView
                                        hasError = true
                                        errorMessage = if (didCrash) {
                                            "The web platform encountered a rendering issue in this environment. You can open it in your device browser or tap Retry."
                                        } else {
                                            "The web view was suspended by the system to save memory. Tap Retry to reload."
                                        }

                                        // Safely post destruction after unmounting
                                        view?.post {
                                            try {
                                                (view.parent as? ViewGroup)?.removeView(view)
                                                view.destroy()
                                            } catch (e: Exception) {
                                                Log.w(TAG, "Error destroying crashed WebView: ${e.message}")
                                            }
                                        }
                                        webViewInstance = null

                                        // Return true to prevent Android OS from killing the host app
                                        return true
                                    }
                                }

                                loadUrl(url)
                                webViewInstance = this
                            }
                        },
                        onRelease = { wb ->
                            exitFullscreen()
                            try {
                                (wb.parent as? ViewGroup)?.removeView(wb)
                                wb.stopLoading()
                                wb.clearHistory()
                                wb.destroy()
                            } catch (e: Exception) {
                                Log.w(TAG, "Error in onRelease: ${e.message}")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Error State Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBg)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = RoseAccent,
                            modifier = Modifier.size(54.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Platform View Notice",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = errorMessage.ifBlank {
                                "The platform server may be temporarily unreachable. You can open the platform in your browser or retry."
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onClose,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Go Back", color = TextMuted)
                            }

                            Button(
                                onClick = {
                                    hasError = false
                                    isLoading = true
                                    progress = 0f
                                    reloadKey++
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = DarkBg
                                )
                            ) {
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleFileDownload(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimetype: String?
) {
    try {
        if (url.startsWith("blob:") || url.startsWith("data:")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Opening file...", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)

        var filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
        if (filename.isBlank() || filename == "downloadfile.bin") {
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
                filename = lastSegment
            }
        }
        if (mimetype == "application/pdf" && !filename.endsWith(".pdf", ignoreCase = true)) {
            filename += ".pdf"
        }
        if ((mimetype == "video/mp4" || url.contains(".mp4", ignoreCase = true)) && !filename.endsWith(".mp4", ignoreCase = true)) {
            filename += ".mp4"
        }

        filename = filename.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        val cookies = CookieManager.getInstance().getCookie(url)
        if (!cookies.isNullOrBlank()) {
            request.addRequestHeader("Cookie", cookies)
        }
        if (!userAgent.isNullOrBlank()) {
            request.addRequestHeader("User-Agent", userAgent)
        }

        request.setTitle("Downloading $filename")
        request.setDescription("Saving file to Downloads")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager != null) {
            downloadManager.enqueue(request)
            Toast.makeText(context, "Downloading: $filename", Toast.LENGTH_LONG).show()
        } else {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        Log.e("WebViewScreen", "Download failed: ${e.message}")
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Opening download via browser...", Toast.LENGTH_SHORT).show()
        } catch (ex: Exception) {
            Toast.makeText(context, "Unable to download file: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }
}
