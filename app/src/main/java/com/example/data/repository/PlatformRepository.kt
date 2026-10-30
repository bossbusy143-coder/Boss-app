package com.example.data.repository

import com.example.data.model.PlatformItem
import kotlinx.coroutines.flow.Flow

interface PlatformRepository {
    fun getPlatformsStream(): Flow<List<PlatformItem>>
    suspend fun addPlatform(platform: PlatformItem): Result<String>
    suspend fun updatePlatform(platform: PlatformItem): Result<Unit>
    suspend fun deletePlatform(platformId: String): Result<Unit>
    suspend fun seedInitialPlatformsIfEmpty(): Result<Int>
    fun isConnectedToFirestore(): Boolean
}
