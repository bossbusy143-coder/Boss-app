package com.example.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Verification : Screen("verification")
    object Home : Screen("home")
    object Platforms : Screen("platforms")
    object Upcoming : Screen("upcoming")
    object Settings : Screen("settings")
    object AdminDashboard : Screen("admin_dashboard")

    object WebView : Screen("webview/{encodedUrl}/{title}") {
        fun createRoute(url: String, title: String): String {
            val safeUrl = try {
                android.util.Base64.encodeToString(
                    url.toByteArray(StandardCharsets.UTF_8),
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                )
            } catch (_: Exception) {
                URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
            }
            val safeTitle = try {
                android.util.Base64.encodeToString(
                    title.toByteArray(StandardCharsets.UTF_8),
                    android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
                )
            } catch (_: Exception) {
                URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
            }
            return "webview/$safeUrl/$safeTitle"
        }
    }
}
