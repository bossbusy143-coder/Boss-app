package com.example.data.repository

import com.example.data.model.BannerItem
import kotlinx.coroutines.flow.Flow

interface BannerRepository {
    fun getEnabledBannersStream(): Flow<List<BannerItem>>
    fun getAllBannersStream(): Flow<List<BannerItem>>
    suspend fun addBanner(banner: BannerItem): Result<String>
    suspend fun updateBanner(banner: BannerItem): Result<Unit>
    suspend fun deleteBanner(bannerId: String): Result<Unit>
    suspend fun toggleBannerEnabled(bannerId: String, enabled: Boolean): Result<Unit>
    suspend fun updateBannerOrder(bannerId: String, newOrder: Int): Result<Unit>
    suspend fun seedInitialBannersIfEmpty(): Result<Int>
}
