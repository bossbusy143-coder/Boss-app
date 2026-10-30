package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.downloader.ApkDownloadHelper
import com.example.data.model.BannerItem
import com.example.data.model.PlatformItem
import com.example.data.repository.BannerRepository
import com.example.data.repository.FirebaseBannerRepository
import com.example.data.repository.FirebasePlatformRepository
import com.example.data.repository.PlatformRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val repository: PlatformRepository,
    private val bannerRepository: BannerRepository = FirebaseBannerRepository(application)
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        FirebasePlatformRepository(application),
        FirebaseBannerRepository(application)
    )

    private val apkDownloadHelper = ApkDownloadHelper(application)

    val banners: StateFlow<List<BannerItem>> = bannerRepository.getEnabledBannersStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlatforms: StateFlow<List<PlatformItem>> = repository.getPlatformsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<String>> = allPlatforms.combine(_selectedCategory) { platforms, _ ->
        val cats = platforms
            .map { it.category.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("All") + if (cats.isEmpty()) listOf("Education", "Study", "Courses", "Tools") else cats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "Education", "Study", "Courses"))

    val activePlatforms: StateFlow<List<PlatformItem>> = combine(
        allPlatforms,
        _searchQuery,
        _selectedCategory
    ) { platforms, query, category ->
        platforms.filter { item ->
            item.isActive && !item.isUpcoming &&
                    (category == "All" || item.category.equals(category, ignoreCase = true)) &&
                    (query.isBlank() ||
                            item.name.contains(query, ignoreCase = true) ||
                            item.category.contains(query, ignoreCase = true) ||
                            item.description.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val upcomingPlatforms: StateFlow<List<PlatformItem>> = combine(
        allPlatforms,
        _searchQuery
    ) { platforms, query ->
        platforms.filter { item ->
            item.isUpcoming &&
                    (query.isBlank() ||
                            item.name.contains(query, ignoreCase = true) ||
                            item.category.contains(query, ignoreCase = true) ||
                            item.description.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun downloadPlatformApk(platform: PlatformItem, onError: (String) -> Unit) {
        apkDownloadHelper.downloadAndInstallApk(
            apkUrl = platform.apkUrl,
            platformName = platform.name,
            onDownloadStarted = {},
            onError = onError
        )
    }

    fun isConnectedToFirestore(): Boolean = repository.isConnectedToFirestore()
}
