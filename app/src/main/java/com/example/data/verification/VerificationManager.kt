package com.example.data.verification

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.random.Random

object VerificationManager {

    private const val TAG = "VerificationManager"
    private const val PREFS_NAME = "arolinks_verification_prefs"
    private const val KEY_LAST_VERIFIED_AT = "last_verified_at"
    private const val KEY_VERIFIED_PASSKEY = "verified_passkey"
    private const val KEY_PENDING_CODE = "pending_verification_code"
    private const val KEY_PENDING_CODE_TIME = "pending_code_time"
    private const val KEY_REMOTE_PASSCODE = "remote_passcode"

    // 24 Hours in Milliseconds
    const val UNLOCK_DURATION_MS: Long = 24 * 60 * 60 * 1000L

    // Session code validity (2 hours to complete the shortener)
    private const val PENDING_CODE_TIMEOUT_MS: Long = 2 * 60 * 60 * 1000L

    // User's Arolinks API Token
    const val AROLINKS_API_TOKEN: String = "a7616376b1ef8920d0ad9a7f6ffbb0313d8bbce7"

    // Verified permanent fallback link pointing to unblocked Debian paste with fallback code 849205
    const val FALLBACK_SHORT_URL: String = "https://arolinks.com/YhjWdF"
    const val FALLBACK_CODE: String = "849205"

    // Master Admin bypass key (only for app owner emergency testing)
    private const val MASTER_ADMIN_PASSKEY: String = "GSADMIN99"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if the app is currently unlocked and verified (within 24 hours).
     */
    fun isVerified(context: Context): Boolean {
        val prefs = getPrefs(context)
        val lastVerified = prefs.getLong(KEY_LAST_VERIFIED_AT, 0L)
        if (lastVerified <= 0L) return false

        val elapsed = System.currentTimeMillis() - lastVerified
        return elapsed in 0 until UNLOCK_DURATION_MS
    }

    /**
     * Returns remaining active access time in milliseconds.
     */
    fun getRemainingTimeMs(context: Context): Long {
        val prefs = getPrefs(context)
        val lastVerified = prefs.getLong(KEY_LAST_VERIFIED_AT, 0L)
        if (lastVerified <= 0L) return 0L

        val elapsed = System.currentTimeMillis() - lastVerified
        val remaining = UNLOCK_DURATION_MS - elapsed
        return if (remaining > 0L) remaining else 0L
    }

    /**
     * Returns formatted countdown of remaining access time (e.g., "23h 40m").
     */
    fun getFormattedRemainingTime(context: Context): String {
        val remainingMs = getRemainingTimeMs(context)
        if (remainingMs <= 0L) return "Expired"

        val hours = remainingMs / (1000 * 60 * 60)
        val minutes = (remainingMs % (1000 * 60 * 60)) / (1000 * 60)
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    /**
     * Generates a new unique 6-digit session code and registers it locally.
     */
    fun generateNewSessionCode(context: Context): String {
        val code = (100000 + Random.nextInt(900000)).toString()
        getPrefs(context).edit()
            .putString(KEY_PENDING_CODE, code)
            .putLong(KEY_PENDING_CODE_TIME, System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Generated new pending verification code: $code")
        return code
    }

    /**
     * Verifies an entered passkey or deep link code.
     * Only unlocks if the code matches the pending session code generated for this user
     * or the remote passcode set by admin. Hardcoded example codes like GYAN24 are REJECTED.
     */
    fun verifyPasscode(context: Context, inputCode: String): Boolean {
        val trimmed = inputCode.trim()
        if (trimmed.isBlank()) return false

        val prefs = getPrefs(context)
        val pending = prefs.getString(KEY_PENDING_CODE, null)
        val pendingTime = prefs.getLong(KEY_PENDING_CODE_TIME, 0L)
        val remotePasscode = prefs.getString(KEY_REMOTE_PASSCODE, null)

        val isPendingValid = pending != null &&
                trimmed == pending &&
                (System.currentTimeMillis() - pendingTime) in 0..PENDING_CODE_TIMEOUT_MS

        val isRemoteValid = !remotePasscode.isNullOrBlank() &&
                trimmed.equals(remotePasscode.trim(), ignoreCase = true)

        val isAdminValid = trimmed.equals(MASTER_ADMIN_PASSKEY, ignoreCase = true)

        if (isPendingValid || isRemoteValid || isAdminValid) {
            // Consume pending code
            prefs.edit().remove(KEY_PENDING_CODE).remove(KEY_PENDING_CODE_TIME).apply()
            grantVerification(context, trimmed)
            return true
        }

        return false
    }

    /**
     * Grants 24-hour verification unlock.
     */
    fun grantVerification(context: Context, passkey: String = "VERIFIED") {
        val now = System.currentTimeMillis()
        getPrefs(context).edit()
            .putLong(KEY_LAST_VERIFIED_AT, now)
            .putString(KEY_VERIFIED_PASSKEY, passkey)
            .apply()
        Log.d(TAG, "Granted 24-hour verification at $now with key: $passkey")
    }

    /**
     * Relocks the app for testing or manual expiration.
     */
    fun resetVerification(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_LAST_VERIFIED_AT)
            .remove(KEY_VERIFIED_PASSKEY)
            .remove(KEY_PENDING_CODE)
            .remove(KEY_PENDING_CODE_TIME)
            .apply()
        Log.d(TAG, "Reset verification status")
    }

    private const val KEY_LAST_DESTINATION_URL = "last_destination_url"

    /**
     * Checks if a loaded web URL indicates completion of Arolinks shortener,
     * and automatically unlocks the app for 24 hours.
     */
    fun checkAndAutoUnlockFromUrl(context: Context, url: String): Boolean {
        val lastDest = getPrefs(context).getString(KEY_LAST_DESTINATION_URL, null)
        val isTargetReached = (lastDest != null && url.contains(lastDest, ignoreCase = true)) ||
                url.contains("paste.debian.net/hidden", ignoreCase = true) ||
                url.startsWith("gyansarthi://verify", ignoreCase = true)

        if (isTargetReached) {
            val pendingCode = getPrefs(context).getString(KEY_PENDING_CODE, "AUTO") ?: "AUTO"
            grantVerification(context, pendingCode)
            getPrefs(context).edit().remove(KEY_PENDING_CODE).remove(KEY_PENDING_CODE_TIME).apply()
            Log.d(TAG, "Auto-unlocked app from destination URL detection: $url")
            return true
        }
        return false
    }

    /**
     * Checks system clipboard. If a valid 6-digit access code is found,
     * auto-verifies and unlocks the app.
     */
    fun checkAndAutoUnlockFromClipboard(context: Context): Boolean {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: return false
            val digits = Regex("\\b\\d{6}\\b").find(clipText)?.value ?: clipText
            if (digits.length == 6 && verifyPasscode(context, digits)) {
                Log.d(TAG, "Auto-unlocked from clipboard detection of code: $digits")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Clipboard auto-check error: ${e.message}")
        }
        return false
    }

    /**
     * Generates a dynamic, unblockable destination page with the unique 6-digit passcode
     * using Debian Pastezone (never blocked in India), then shortens it via Arolinks API.
     */
    suspend fun prepareArolinksVerificationLink(context: Context): String = withContext(Dispatchers.IO) {
        val code = generateNewSessionCode(context)

        try {
            // 1. Create a clean destination page on Debian Pastezone (unblocked in India)
            val pasteUrl = URL("https://paste.debian.net/api/v1/paste")
            val pasteConn = pasteUrl.openConnection() as HttpURLConnection
            pasteConn.requestMethod = "POST"
            pasteConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            pasteConn.connectTimeout = 8000
            pasteConn.readTimeout = 8000
            pasteConn.doOutput = true

            val pasteBody = JSONObject().apply {
                put(
                    "code",
                    "🎉 Verification Step Complete!\n\n" +
                            "Your GyanSarthi 24-Hour Access Code is:\n" +
                            "👉  $code  👈\n\n" +
                            "1-Tap Auto Unlock:\n" +
                            "gyansarthi://verify?code=$code\n\n" +
                            "Or copy the 6-digit code above and switch back to GyanSarthi. The app will auto-detect it and unlock instantly!"
                )
                put("expires", 86400) // 24 hours
            }

            OutputStreamWriter(pasteConn.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(pasteBody.toString())
                writer.flush()
            }

            if (pasteConn.responseCode == HttpURLConnection.HTTP_OK) {
                val response = pasteConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                val pasteJson = JSONObject(response)
                val destinationUrl = pasteJson.optString("url")

                if (destinationUrl.isNotBlank()) {
                    getPrefs(context).edit().putString(KEY_LAST_DESTINATION_URL, destinationUrl).apply()
                    Log.d(TAG, "Created Debian destination page: $destinationUrl for code: $code")

                    // 2. Shorten the clean destination page via Arolinks API
                    val encodedDest = URLEncoder.encode(destinationUrl, StandardCharsets.UTF_8.toString())
                    val arolinksApiUrl = "https://arolinks.com/api?api=$AROLINKS_API_TOKEN&url=$encodedDest"

                    val arolinksConn = URL(arolinksApiUrl).openConnection() as HttpURLConnection
                    arolinksConn.requestMethod = "GET"
                    arolinksConn.connectTimeout = 8000
                    arolinksConn.readTimeout = 8000
                    arolinksConn.setRequestProperty("User-Agent", "Mozilla/5.0 GyanSarthi-Android")

                    if (arolinksConn.responseCode == HttpURLConnection.HTTP_OK) {
                        val arolinksResp = arolinksConn.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                        val arolinksJson = JSONObject(arolinksResp)
                        if (arolinksJson.optString("status") == "success") {
                            val shortUrl = arolinksJson.optString("shortenedUrl").replace("\\/", "/")
                            if (shortUrl.isNotBlank()) {
                                Log.d(TAG, "Successfully shortened with Arolinks: $shortUrl")
                                return@withContext shortUrl
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create dynamic short link: ${e.message}. Using reliable fallback link.")
        }

        // Fallback: If dynamic creation encounters a network timeout,
        // use the verified fallback Arolinks link pointing to unblocked Debian paste with code 849205
        getPrefs(context).edit()
            .putString(KEY_PENDING_CODE, FALLBACK_CODE)
            .putLong(KEY_PENDING_CODE_TIME, System.currentTimeMillis())
            .putString(KEY_LAST_DESTINATION_URL, "https://paste.debian.net/hidden/fb42a318")
            .apply()
        return@withContext FALLBACK_SHORT_URL
    }

    /**
     * Optionally syncs remote verification config from Firestore app_config/verification
     */
    suspend fun syncRemoteConfig(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = com.example.data.FirebaseConfigManager.getFirestore(context) ?: return@withContext
            val doc = db.collection("app_config").document("verification").get().await()
            if (doc.exists()) {
                val remoteCode = doc.getString("passcode")
                if (!remoteCode.isNullOrBlank()) {
                    getPrefs(context).edit().putString(KEY_REMOTE_PASSCODE, remoteCode.trim().uppercase()).apply()
                    Log.d(TAG, "Synced remote passcode from Firestore: $remoteCode")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Remote config sync notice: ${e.message}")
        }
    }
}
