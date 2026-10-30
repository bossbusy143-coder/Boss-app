package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseConfigManager {
    private const val TAG = "FirebaseConfigManager"
    private const val PREFS_NAME = "gyansarthi_firebase_prefs"
    private const val KEY_FIREBASE_API_KEY = "firebase_api_key"

    // Project parameters from google-services.json
    const val TARGET_PROJECT_ID = "gyansarthi-b30f0"
    const val TARGET_APPLICATION_ID = "1:86820832067:android:dac78de64d61caaf90a5f6"
    const val DEFAULT_API_KEY = "AIzaSyA1LzxWGqS45d2T0459_PEW9V7FpSRTtvA"
    const val TARGET_STORAGE_BUCKET = "gyansarthi-b30f0.firebasestorage.app"

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_FIREBASE_API_KEY, "") ?: ""
        if (saved.isNotBlank()) return saved

        // Check if injected via BuildConfig
        val buildConfigKey = try {
            val field = Class.forName("com.example.BuildConfig").getField("FIREBASE_API_KEY")
            val value = field.get(null) as? String
            value?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
        if (buildConfigKey.isNotBlank()) return buildConfigKey

        // Fallback to configured key from google-services.json
        return DEFAULT_API_KEY
    }

    fun saveApiKey(context: Context, apiKey: String): Boolean {
        val cleanKey = apiKey.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FIREBASE_API_KEY, cleanKey).apply()
        if (FirebaseApp.getApps(context).isEmpty()) {
            return initializeWithOptions(context, cleanKey)
        }
        return true
    }

    fun ensureFirebaseInitialized(context: Context): Boolean {
        // 1. If already initialized, never delete or reinitialize the singleton
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            return true
        }

        // 2. Try default resource-based initialization (automatically parsed from /app/google-services.json)
        try {
            val app = FirebaseApp.initializeApp(context)
            if (app != null) {
                Log.d(TAG, "Firebase initialized automatically from google-services.json resources.")
                return true
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Resource-based Firebase init failed: ${e.message}")
        }

        // 3. Try programmatic initialization using configured options for gyansarthi-b30f0
        val apiKey = getApiKey(context)
        if (apiKey.isNotBlank()) {
            return initializeWithOptions(context, apiKey)
        }

        return false
    }

    fun initializeWithOptions(context: Context, apiKey: String): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) {
            return true
        }
        if (apiKey.isBlank()) return false
        return try {
            val options = FirebaseOptions.Builder()
                .setProjectId(TARGET_PROJECT_ID)
                .setApplicationId(TARGET_APPLICATION_ID)
                .setApiKey(apiKey.trim())
                .setStorageBucket(TARGET_STORAGE_BUCKET)
                .build()

            FirebaseApp.initializeApp(context, options)
            Log.d(TAG, "Successfully initialized Firebase for $TARGET_PROJECT_ID")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize Firebase with options: ${e.message}", e)
            false
        }
    }

    fun getAuth(context: Context): FirebaseAuth? {
        if (!ensureFirebaseInitialized(context)) return null
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "Error getting FirebaseAuth instance: ${e.message}", e)
            null
        }
    }

    fun getFirestore(context: Context): FirebaseFirestore? {
        if (!ensureFirebaseInitialized(context)) return null
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.e(TAG, "Error getting FirebaseFirestore instance: ${e.message}", e)
            null
        }
    }
}
