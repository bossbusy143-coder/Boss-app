package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseConfigManager
import com.example.data.model.BannerItem
import com.example.data.model.PlatformItem
import com.example.data.repository.BannerRepository
import com.example.data.repository.FirebaseBannerRepository
import com.example.data.repository.FirebasePlatformRepository
import com.example.data.repository.PlatformRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "AdminViewModel"

data class AdminStats(
    val total: Int = 0,
    val active: Int = 0,
    val upcoming: Int = 0,
    val withWebsite: Int = 0,
    val withApk: Int = 0
)

data class AdminBannerStats(
    val total: Int = 0,
    val enabled: Int = 0,
    val telegram: Int = 0,
    val image: Int = 0
)

class AdminViewModel(
    application: Application,
    private val repository: PlatformRepository,
    private val bannerRepository: BannerRepository = FirebaseBannerRepository(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        FirebasePlatformRepository(application),
        FirebaseBannerRepository(application)
    )

    private val firebaseAuth: FirebaseAuth?
        get() = FirebaseConfigManager.getAuth(getApplication())

    private val firestore: FirebaseFirestore?
        get() = FirebaseConfigManager.getFirestore(getApplication())

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _adminEmail = MutableStateFlow<String?>(null)
    val adminEmail: StateFlow<String?> = _adminEmail.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _messageEvents = MutableSharedFlow<String>()
    val messageEvents: SharedFlow<String> = _messageEvents.asSharedFlow()

    val platforms: StateFlow<List<PlatformItem>> = repository.getPlatformsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminStats: StateFlow<AdminStats> = platforms.map { list ->
        AdminStats(
            total = list.size,
            active = list.count { it.isActive },
            upcoming = list.count { it.isUpcoming },
            withWebsite = list.count { it.websiteUrl.isNotBlank() },
            withApk = list.count { it.apkUrl.isNotBlank() }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminStats())

    val banners: StateFlow<List<BannerItem>> = bannerRepository.getAllBannersStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bannerStats: StateFlow<AdminBannerStats> = banners.map { list ->
        AdminBannerStats(
            total = list.size,
            enabled = list.count { it.enabled },
            telegram = list.count { it.type.equals("telegram", ignoreCase = true) },
            image = list.count { it.type.equals("image", ignoreCase = true) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminBannerStats())

    init {
        // Verify existing authentication session against Firestore admins collection
        viewModelScope.launch {
            try {
                val auth = firebaseAuth
                val db = firestore
                val currentUser = auth?.currentUser
                if (currentUser != null && db != null) {
                    val adminDoc = db.collection("admins").document(currentUser.uid).get().await()
                    val role = adminDoc.getString("role")
                    val enabled = adminDoc.getBoolean("enabled") ?: false
                    val isRoleValid = role == null || role.equals("admin", ignoreCase = true)
                    if (adminDoc.exists() && isRoleValid && enabled) {
                        _isAdminLoggedIn.value = true
                        _adminEmail.value = currentUser.email
                        Log.d(TAG, "Restored active admin session for ${currentUser.email}")
                    } else {
                        Log.w(TAG, "User ${currentUser.email} is not authorized in admins collection. Signing out.")
                        auth.signOut()
                        _isAdminLoggedIn.value = false
                        _adminEmail.value = null
                    }
                } else {
                    _isAdminLoggedIn.value = false
                    _adminEmail.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to verify admin status on init: ${e.message}")
                _isAdminLoggedIn.value = false
                _adminEmail.value = null
            }
        }
    }

    fun isFirebaseConfigured(): Boolean = FirebaseConfigManager.ensureFirebaseInitialized(getApplication())

    fun getFirebaseApiKey(): String = FirebaseConfigManager.getApiKey(getApplication())

    fun saveFirebaseApiKey(apiKey: String): Boolean {
        val success = FirebaseConfigManager.saveApiKey(getApplication(), apiKey)
        if (success) {
            viewModelScope.launch {
                _messageEvents.emit("Firebase connected for ${FirebaseConfigManager.TARGET_PROJECT_ID}")
            }
        } else {
            viewModelScope.launch {
                _messageEvents.emit("Failed to initialize Firebase with provided API Key.")
            }
        }
        return success
    }

    /**
     * Authenticates administrator using Firebase Authentication signInWithEmailAndPassword
     * and strictly validates authorization against the admins/{uid} record in Cloud Firestore.
     */
    fun loginAdmin(
        email: String,
        pass: String,
        apiKeyInput: String? = null,
        onSuccess: () -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()

        if (cleanEmail.isBlank() || cleanPass.isBlank()) {
            viewModelScope.launch { _messageEvents.emit("Email and password cannot be empty.") }
            return
        }

        // If a new API key was entered that differs from current, save it
        if (!apiKeyInput.isNullOrBlank()) {
            val currentKey = FirebaseConfigManager.getApiKey(getApplication())
            if (apiKeyInput.trim() != currentKey) {
                FirebaseConfigManager.saveApiKey(getApplication(), apiKeyInput.trim())
            }
        }

        val auth = firebaseAuth
        val db = firestore
        if (auth == null || db == null) {
            val key = FirebaseConfigManager.getApiKey(getApplication())
            val msg = if (key.isBlank()) {
                "Firebase Web API Key is required for project ${FirebaseConfigManager.TARGET_PROJECT_ID}. Please configure it below."
            } else {
                "Admin authentication is unavailable. Please verify your Firebase project configuration."
            }
            viewModelScope.launch { _messageEvents.emit(msg) }
            return
        }

        _isProcessing.value = true
        viewModelScope.launch {
            try {
                // 1. Authenticate with Firebase Authentication
                val authResult = auth.signInWithEmailAndPassword(cleanEmail, cleanPass).await()
                val user = authResult.user
                val uid = user?.uid

                if (uid == null) {
                    auth.signOut()
                    _messageEvents.emit("Authentication failed: User ID not found.")
                    return@launch
                }

                // 2. Authorize against the Firestore admins/{uid} collection
                val adminDoc = db.collection("admins").document(uid).get().await()
                val role = adminDoc.getString("role")
                val enabled = adminDoc.getBoolean("enabled") ?: false
                val isRoleValid = role == null || role.equals("admin", ignoreCase = true)

                if (!adminDoc.exists() || !isRoleValid || !enabled) {
                    Log.w(TAG, "Access Denied: UID $uid does not have an active admin document in admins collection.")
                    auth.signOut()
                    _isAdminLoggedIn.value = false
                    _adminEmail.value = null
                    _messageEvents.emit("Access Denied: UID $uid is not authorized as an active administrator in admins/{uid}.")
                    return@launch
                }

                // 3. Grant admin session and navigate
                _isAdminLoggedIn.value = true
                _adminEmail.value = user.email ?: cleanEmail
                _messageEvents.emit("Welcome, ${user.email ?: "Admin"}")
                onSuccess()
            } catch (e: FirebaseAuthInvalidUserException) {
                _messageEvents.emit("Invalid email: No user account found for $cleanEmail.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _messageEvents.emit("Incorrect email or password. Please verify your credentials.")
            } catch (e: FirebaseNetworkException) {
                _messageEvents.emit("Network error: Unable to connect to Firebase. Check your internet connection.")
            } catch (e: Exception) {
                Log.e(TAG, "Admin login error: ${e.message}", e)
                val msg = e.localizedMessage ?: "Authentication failed."
                if (msg.contains("credential is incorrect", ignoreCase = true) ||
                    msg.contains("malformed or has expired", ignoreCase = true) ||
                    msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                    msg.contains("password", ignoreCase = true) ||
                    msg.contains("credential", ignoreCase = true)
                ) {
                    _messageEvents.emit("Incorrect email or password. Please verify your credentials.")
                } else if (msg.contains("API key not valid", ignoreCase = true)) {
                    _messageEvents.emit("Firebase API Key is invalid. Please verify your Web API Key for ${FirebaseConfigManager.TARGET_PROJECT_ID}.")
                } else if (msg.contains("TOO_MANY_ATTEMPTS", ignoreCase = true)) {
                    _messageEvents.emit("Too many failed login attempts. Please try again later.")
                } else {
                    _messageEvents.emit(msg)
                }
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error during logout: ${e.message}")
        }
        _isAdminLoggedIn.value = false
        _adminEmail.value = null
        viewModelScope.launch { _messageEvents.emit("Logged out of Admin") }
    }

    fun savePlatform(platform: PlatformItem, onComplete: () -> Unit) {
        if (platform.name.isBlank()) {
            viewModelScope.launch { _messageEvents.emit("Platform name is required") }
            return
        }
        if (platform.websiteUrl.isBlank() && platform.apkUrl.isBlank()) {
            viewModelScope.launch { _messageEvents.emit("Provide either a Website URL or APK URL") }
            return
        }

        _isProcessing.value = true
        viewModelScope.launch {
            try {
                if (platform.id.isBlank()) {
                    val result = repository.addPlatform(platform)
                    if (result.isSuccess) {
                        _messageEvents.emit("Platform '${platform.name}' added successfully to Firestore!")
                        onComplete()
                    } else {
                        _messageEvents.emit("Failed to add to Firestore: ${formatFirestoreError(result.exceptionOrNull())}")
                    }
                } else {
                    val result = repository.updatePlatform(platform)
                    if (result.isSuccess) {
                        _messageEvents.emit("Platform '${platform.name}' updated in Firestore!")
                        onComplete()
                    } else {
                        _messageEvents.emit("Failed to update in Firestore: ${formatFirestoreError(result.exceptionOrNull())}")
                    }
                }
            } catch (e: Exception) {
                _messageEvents.emit("Error saving platform: ${formatFirestoreError(e)}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deletePlatform(platform: PlatformItem) {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val result = repository.deletePlatform(platform.id)
                if (result.isSuccess) {
                    _messageEvents.emit("Platform '${platform.name}' deleted from Firestore.")
                } else {
                    _messageEvents.emit("Failed to delete from Firestore: ${formatFirestoreError(result.exceptionOrNull())}")
                }
            } catch (e: Exception) {
                _messageEvents.emit("Delete error: ${formatFirestoreError(e)}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    private fun formatFirestoreError(e: Throwable?): String {
        val msg = e?.localizedMessage ?: "Unknown Firestore error."
        return if (msg.contains("PERMISSION_DENIED", ignoreCase = true) ||
            msg.contains("insufficient permissions", ignoreCase = true)
        ) {
            "Permission Denied: Please publish firestore.rules in Firebase Console for gyansarthi-b30f0."
        } else {
            msg
        }
    }

    fun togglePlatformActive(platform: PlatformItem) {
        val updated = platform.copy(isActive = !platform.isActive)
        viewModelScope.launch {
            val result = repository.updatePlatform(updated)
            if (result.isSuccess) {
                _messageEvents.emit("${platform.name} is now ${if (updated.isActive) "Active" else "Inactive"}")
            } else {
                _messageEvents.emit("Failed to update status in Firestore: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun togglePlatformUpcoming(platform: PlatformItem) {
        val updated = platform.copy(isUpcoming = !platform.isUpcoming)
        viewModelScope.launch {
            val result = repository.updatePlatform(updated)
            if (result.isSuccess) {
                _messageEvents.emit("${platform.name} marked as ${if (updated.isUpcoming) "Upcoming" else "Standard"}")
            } else {
                _messageEvents.emit("Failed to update upcoming status: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun seedInitialPlatforms() {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val result = repository.seedInitialPlatformsIfEmpty()
                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    if (count > 0) {
                        _messageEvents.emit("Seeded $count initial platforms into Firestore!")
                    } else {
                        _messageEvents.emit("Platforms collection already contains documents.")
                    }
                } else {
                    _messageEvents.emit("Failed to seed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _messageEvents.emit("Seed error: ${e.localizedMessage}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun isConnectedToFirestore(): Boolean = repository.isConnectedToFirestore()

    fun saveBanner(banner: BannerItem, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                if (banner.id.isBlank()) {
                    val result = bannerRepository.addBanner(banner)
                    if (result.isSuccess) {
                        _messageEvents.emit("Banner '${banner.title}' added successfully!")
                        onComplete()
                    } else {
                        _messageEvents.emit("Failed to add banner: ${formatFirestoreError(result.exceptionOrNull())}")
                    }
                } else {
                    val result = bannerRepository.updateBanner(banner)
                    if (result.isSuccess) {
                        _messageEvents.emit("Banner '${banner.title}' updated successfully!")
                        onComplete()
                    } else {
                        _messageEvents.emit("Failed to update banner: ${formatFirestoreError(result.exceptionOrNull())}")
                    }
                }
            } catch (e: Exception) {
                _messageEvents.emit("Error saving banner: ${formatFirestoreError(e)}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun toggleBannerEnabled(banner: BannerItem) {
        viewModelScope.launch {
            val newState = !banner.enabled
            val result = bannerRepository.toggleBannerEnabled(banner.id, newState)
            if (result.isSuccess) {
                _messageEvents.emit("Banner '${banner.title}' is now ${if (newState) "enabled" else "disabled"}.")
            } else {
                _messageEvents.emit("Failed to toggle banner: ${formatFirestoreError(result.exceptionOrNull())}")
            }
        }
    }

    fun deleteBanner(banner: BannerItem) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val result = bannerRepository.deleteBanner(banner.id)
                if (result.isSuccess) {
                    _messageEvents.emit("Banner '${banner.title}' deleted from Firestore.")
                } else {
                    _messageEvents.emit("Failed to delete banner: ${formatFirestoreError(result.exceptionOrNull())}")
                }
            } catch (e: Exception) {
                _messageEvents.emit("Delete error: ${formatFirestoreError(e)}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun updateBannerOrder(banner: BannerItem, newOrder: Int) {
        if (newOrder < 0) return
        viewModelScope.launch {
            val result = bannerRepository.updateBannerOrder(banner.id, newOrder)
            if (result.isSuccess) {
                _messageEvents.emit("Order updated for '${banner.title}' to $newOrder.")
            } else {
                _messageEvents.emit("Failed to update order: ${formatFirestoreError(result.exceptionOrNull())}")
            }
        }
    }

    fun seedInitialBanners() {
        _isProcessing.value = true
        viewModelScope.launch {
            try {
                val result = bannerRepository.seedInitialBannersIfEmpty()
                if (result.isSuccess) {
                    val count = result.getOrDefault(0)
                    if (count > 0) {
                        _messageEvents.emit("Successfully seeded $count sample banners into Firestore!")
                    } else {
                        _messageEvents.emit("Banners already present in Firestore.")
                    }
                } else {
                    _messageEvents.emit("Failed to seed banners: ${formatFirestoreError(result.exceptionOrNull())}")
                }
            } catch (e: Exception) {
                _messageEvents.emit("Seed banners error: ${formatFirestoreError(e)}")
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
