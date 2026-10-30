package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.FirebaseConfigManager
import com.example.data.model.PlatformItem
import com.example.data.verification.VerificationManager
import com.example.ui.components.AppTopBar
import com.example.ui.components.BottomNavBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminLoginDialog
import com.example.ui.screens.ArolinksVerificationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlatformsListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UpcomingScreen
import com.example.ui.screens.WebViewScreen
import com.example.ui.theme.AppBg
import com.example.ui.theme.GyanSarthiTheme
import com.example.ui.theme.ThemeManager
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.MainViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val adminViewModel: AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        FirebaseConfigManager.ensureFirebaseInitialized(applicationContext)
        handleDeepLink(intent)
        enableEdgeToEdge()

        setContent {
            GyanSarthiTheme(darkTheme = ThemeManager.isDarkTheme) {
                GyanSarthiApp(
                    mainViewModel = mainViewModel,
                    adminViewModel = adminViewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "gyansarthi" && uri.host == "verify") {
            val key = uri.getQueryParameter("key") ?: uri.getQueryParameter("code") ?: return
            if (VerificationManager.verifyPasscode(this, key)) {
                Toast.makeText(this, "24-Hour Access Verified Successfully!", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun GyanSarthiApp(
    mainViewModel: MainViewModel,
    adminViewModel: AdminViewModel
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showAdminLoginDialog by remember { mutableStateOf(false) }
    val isAdminLoggedIn by adminViewModel.isAdminLoggedIn.collectAsState()

    val onOpenPlatform: (PlatformItem) -> Unit = { platform ->
        if (platform.websiteUrl.isNotBlank()) {
            navController.navigate(Screen.WebView.createRoute(platform.websiteUrl, platform.name))
        }
    }

    val onDownloadApk: (PlatformItem) -> Unit = { platform ->
        mainViewModel.downloadPlatformApk(platform) { error ->
            // Toast or error handler
        }
    }

    val isSplash = currentRoute == Screen.Splash.route
    val isVerification = currentRoute == Screen.Verification.route
    val isWebView = currentRoute?.startsWith("webview") == true
    val isAdminDashboard = currentRoute == Screen.AdminDashboard.route

    val isTopLevelRoute = currentRoute in listOf(
        Screen.Home.route,
        Screen.Platforms.route,
        Screen.Upcoming.route,
        Screen.Settings.route
    ) && !isVerification

    // Expiration check: If on a normal screen but 24 hours expired, redirect to Verification
    LaunchedEffect(currentRoute) {
        if (!isSplash && !isVerification && currentRoute != null) {
            if (!VerificationManager.isVerified(context)) {
                Toast.makeText(context, "24-Hour Access expired. Please verify to continue.", Toast.LENGTH_SHORT).show()
                navController.navigate(Screen.Verification.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg),
        containerColor = AppBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            if (!isSplash && !isWebView && !isAdminDashboard && !isVerification) {
                AppTopBar(
                    title = when (currentRoute) {
                        Screen.Platforms.route -> "Directory"
                        Screen.Upcoming.route -> "Upcoming"
                        Screen.Settings.route -> "Settings"
                        else -> "GyanSarthi"
                    },
                    subtitle = when (currentRoute) {
                        Screen.Platforms.route -> "All Platforms"
                        Screen.Upcoming.route -> "Coming Soon"
                        Screen.Settings.route -> "App & Catalog"
                        else -> "Education & Platform Hub"
                    },
                    showAdminAction = true,
                    onAdminClick = {
                        if (isAdminLoggedIn) {
                            navController.navigate(Screen.AdminDashboard.route)
                        } else {
                            showAdminLoginDialog = true
                        }
                    },
                    showThemeToggle = true,
                    isDarkTheme = ThemeManager.isDarkTheme,
                    onToggleTheme = {
                        ThemeManager.toggleTheme(context)
                    }
                )
            }
        },
        bottomBar = {
            if (isTopLevelRoute) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val effectivePadding = if (isWebView || isSplash || isVerification) androidx.compose.foundation.layout.PaddingValues() else innerPadding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(effectivePadding)
                .consumeWindowInsets(effectivePadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route
            ) {
                composable(Screen.Splash.route) {
                    SplashScreen(
                        onSplashFinished = {
                            val verified = VerificationManager.isVerified(context)
                            if (verified) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Screen.Verification.route) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.Verification.route) {
                    ArolinksVerificationScreen(
                        onVerificationSuccess = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Verification.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = mainViewModel,
                        onOpenPlatform = onOpenPlatform,
                        onDownloadApk = onDownloadApk,
                        onNavigateToUpcoming = {
                            navController.navigate(Screen.Upcoming.route)
                        }
                    )
                }

                composable(Screen.Platforms.route) {
                    PlatformsListScreen(
                        viewModel = mainViewModel,
                        onOpenPlatform = onOpenPlatform,
                        onDownloadApk = onDownloadApk
                    )
                }

                composable(Screen.Upcoming.route) {
                    UpcomingScreen(viewModel = mainViewModel)
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateToAdmin = {
                            if (isAdminLoggedIn) {
                                navController.navigate(Screen.AdminDashboard.route)
                            } else {
                                showAdminLoginDialog = true
                            }
                        },
                        onNavigateToVerification = {
                            navController.navigate(Screen.Verification.route)
                        }
                    )
                }

                composable(Screen.AdminDashboard.route) {
                    AdminDashboardScreen(
                        viewModel = adminViewModel,
                        onBackToUserApp = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Screen.WebView.route,
                    arguments = listOf(
                        navArgument("encodedUrl") { type = NavType.StringType },
                        navArgument("title") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val encodedUrl = backStackEntry.arguments?.getString("encodedUrl") ?: ""
                    val encodedTitle = backStackEntry.arguments?.getString("title") ?: "Platform"
                    val decodedUrl = try {
                        val bytes = android.util.Base64.decode(encodedUrl, android.util.Base64.URL_SAFE)
                        String(bytes, StandardCharsets.UTF_8)
                    } catch (_: Exception) {
                        try {
                            URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                        } catch (_: Exception) {
                            encodedUrl
                        }
                    }
                    val decodedTitle = try {
                        val bytes = android.util.Base64.decode(encodedTitle, android.util.Base64.URL_SAFE)
                        String(bytes, StandardCharsets.UTF_8)
                    } catch (_: Exception) {
                        try {
                            URLDecoder.decode(encodedTitle, StandardCharsets.UTF_8.toString())
                        } catch (_: Exception) {
                            encodedTitle
                        }
                    }

                    WebViewScreen(
                        url = decodedUrl,
                        title = decodedTitle,
                        onClose = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }

    if (showAdminLoginDialog) {
        AdminLoginDialog(
            viewModel = adminViewModel,
            onDismiss = { showAdminLoginDialog = false },
            onLoginSuccess = {
                showAdminLoginDialog = false
                navController.navigate(Screen.AdminDashboard.route)
            }
        )
    }
}
