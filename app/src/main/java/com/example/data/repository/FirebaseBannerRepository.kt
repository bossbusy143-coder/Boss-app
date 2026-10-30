package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.FirebaseConfigManager
import com.example.data.model.BannerItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseBannerRepository(private val context: Context) : BannerRepository {

    private val tag = "FirebaseBannerRepo"

    private fun getFirestore(): FirebaseFirestore? {
        return FirebaseConfigManager.getFirestore(context)
    }

    override fun getEnabledBannersStream(): Flow<List<BannerItem>> = callbackFlow {
        val db = getFirestore()
        if (db == null) {
            Log.w(tag, "Firestore not initialized, returning default seed banners.")
            trySend(getInitialSeedBanners())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = db.collection("banners")
            .whereEqualTo("enabled", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(tag, "Firestore permission notice for banners: check firestore.rules.")
                    } else {
                        Log.w(tag, "Firestore banners listener error: ${error.message}")
                    }
                    // Return seed banners so user screen remains functional
                    trySend(getInitialSeedBanners())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            BannerItem.fromMap(doc.id, data)
                        }
                    }.sortedWith(compareBy({ it.order }, { it.title }))

                    if (items.isEmpty()) {
                        // Fallback to initial sample banner if none configured yet
                        trySend(getInitialSeedBanners())
                    } else {
                        trySend(items)
                    }
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override fun getAllBannersStream(): Flow<List<BannerItem>> = callbackFlow {
        val db = getFirestore()
        if (db == null) {
            trySend(getInitialSeedBanners())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = db.collection("banners")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(tag, "Admin banners listener error: ${error.message}")
                    trySend(getInitialSeedBanners())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { data ->
                            BannerItem.fromMap(doc.id, data)
                        }
                    }.sortedWith(compareBy({ it.order }, { it.title }))

                    trySend(items)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun addBanner(banner: BannerItem): Result<String> = withContext(Dispatchers.IO) {
        val db = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not initialized. Verify Firebase credentials.")
        )

        try {
            val docRef = db.collection("banners").document()
            val bannerToSave = banner.copy(
                id = docRef.id,
                createdAt = Timestamp.now()
            )
            docRef.set(bannerToSave.toMap()).await()
            Log.d(tag, "Banner added successfully with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add banner: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateBanner(banner: BannerItem): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not initialized. Verify Firebase credentials.")
        )

        try {
            val docRef = if (banner.id.isNotBlank()) {
                db.collection("banners").document(banner.id)
            } else {
                db.collection("banners").document()
            }
            docRef.set(banner.toMap(), SetOptions.merge()).await()
            Log.d(tag, "Banner updated successfully: ${docRef.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update banner: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteBanner(bannerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val db = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not initialized.")
        )

        try {
            db.collection("banners").document(bannerId).delete().await()
            Log.d(tag, "Banner deleted successfully: $bannerId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete banner: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleBannerEnabled(bannerId: String, enabled: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val db = getFirestore() ?: return@withContext Result.failure(
                IllegalStateException("Firestore is not initialized.")
            )

            try {
                db.collection("banners").document(bannerId)
                    .update("enabled", enabled)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(tag, "Failed to toggle banner enabled state: ${e.message}", e)
                Result.failure(e)
            }
        }

    override suspend fun updateBannerOrder(bannerId: String, newOrder: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            val db = getFirestore() ?: return@withContext Result.failure(
                IllegalStateException("Firestore is not initialized.")
            )

            try {
                db.collection("banners").document(bannerId)
                    .update("order", newOrder)
                    .await()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(tag, "Failed to update banner order: ${e.message}", e)
                Result.failure(e)
            }
        }

    override suspend fun seedInitialBannersIfEmpty(): Result<Int> = withContext(Dispatchers.IO) {
        val db = getFirestore() ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not initialized.")
        )

        try {
            val snapshot = db.collection("banners").limit(1).get().await()
            if (!snapshot.isEmpty) {
                Log.d(tag, "Banners collection already contains documents, skipping seed.")
                return@withContext Result.success(0)
            }

            val seedList = getInitialSeedBanners()
            var count = 0
            for (banner in seedList) {
                val docRef = db.collection("banners").document()
                val prepared = banner.copy(
                    id = docRef.id,
                    createdAt = Timestamp.now()
                )
                docRef.set(prepared.toMap()).await()
                count++
            }
            Log.d(tag, "Successfully seeded $count initial banners into Firestore.")
            Result.success(count)
        } catch (e: Exception) {
            Log.e(tag, "Failed to seed banners: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        fun getInitialSeedBanners(): List<BannerItem> {
            return listOf(
                BannerItem(
                    id = "seed_telegram_banner",
                    title = "Join GyanSarthi on Telegram",
                    subtitle = "Get free study notes, test alerts, batch updates & daily quizzes directly on Telegram",
                    buttonText = "Join Telegram",
                    buttonUrl = "https://t.me/gyansarthi_community",
                    imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80",
                    type = "telegram",
                    enabled = true,
                    order = 1
                ),
                BannerItem(
                    id = "seed_exam_banner",
                    title = "All India Mock Test Series",
                    subtitle = "Complete exam simulation with live national rank, speed analytics & video explanations",
                    buttonText = "Explore Tests",
                    buttonUrl = "https://gyansarthi.org",
                    imageUrl = "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=800&auto=format&fit=crop&q=80",
                    type = "image",
                    enabled = true,
                    order = 2
                )
            )
        }
    }
}
