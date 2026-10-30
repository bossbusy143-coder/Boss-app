package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.FirebaseConfigManager
import com.example.data.model.PlatformItem
import com.google.firebase.FirebaseApp
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebasePlatformRepository(
    private val context: Context
) : PlatformRepository {

    private val tag = "PlatformRepository"
    private val firestore: FirebaseFirestore?
        get() = FirebaseConfigManager.getFirestore(context)

    override fun isConnectedToFirestore(): Boolean = firestore != null

    override fun getPlatformsStream(): Flow<List<PlatformItem>> = callbackFlow {
        val db = firestore
        if (db == null) {
            Log.w(tag, "Firestore not available; emitting initial seed platform catalog.")
            trySend(getInitialSeedData().sortedBy { it.sortOrder })
            awaitClose { }
            return@callbackFlow
        }

        // Real-time listener for the "platforms" collection
        val listener: ListenerRegistration = db.collection("platforms")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.w(
                            tag,
                            "Firestore permission notice: 'platforms' collection read is restricted by security rules. Showing default catalog. Publish 'allow read: if true;' in Firebase Console to enable live cloud sync."
                        )
                    } else {
                        Log.w(tag, "Firestore real-time listener notice: ${error.message}")
                    }
                    // Emit seed data so UI stays responsive and functional
                    trySend(getInitialSeedData().sortedBy { it.sortOrder })
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            PlatformItem.fromMap(doc.id, data)
                        } catch (e: Exception) {
                            Log.w(tag, "Failed to parse platform document ${doc.id}", e)
                            null
                        }
                    }.sortedWith(compareBy({ it.sortOrder }, { it.name }))

                    // Emit live documents or seed fallback if collection is empty
                    if (items.isEmpty()) {
                        trySend(getInitialSeedData().sortedBy { it.sortOrder })
                    } else {
                        trySend(items)
                    }
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun addPlatform(platform: PlatformItem): Result<String> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firebase Firestore is not initialized.")
        )
        val now = getCurrentTimestamp()
        val itemToSave = platform.copy(
            createdAt = if (platform.createdAt.isBlank()) now else platform.createdAt,
            updatedAt = now
        )

        try {
            val docRef = if (platform.id.isNotBlank()) {
                db.collection("platforms").document(platform.id)
            } else {
                db.collection("platforms").document()
            }
            docRef.set(itemToSave.toMap()).await()
            Log.d(tag, "Successfully added platform '${itemToSave.name}' with ID: ${docRef.id}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(tag, "Failed to add platform to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePlatform(platform: PlatformItem): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firebase Firestore is not initialized.")
        )
        if (platform.id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot update platform without document ID."))
        }

        val now = getCurrentTimestamp()
        val itemToSave = platform.copy(updatedAt = now)

        try {
            db.collection("platforms").document(platform.id)
                .set(itemToSave.toMap(), SetOptions.merge())
                .await()
            Log.d(tag, "Successfully updated platform '${platform.name}' (${platform.id}) in Firestore.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to update platform '${platform.name}' in Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePlatform(platformId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firebase Firestore is not initialized.")
        )
        if (platformId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Platform ID cannot be blank."))
        }

        try {
            db.collection("platforms").document(platformId).delete().await()
            Log.d(tag, "Successfully deleted platform $platformId from Firestore.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to delete platform $platformId from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun seedInitialPlatformsIfEmpty(): Result<Int> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firebase Firestore is not initialized.")
        )
        val initialSeeds = getInitialSeedData()

        try {
            val snapshot = db.collection("platforms").get().await()
            if (snapshot.isEmpty) {
                val batch = db.batch()
                for (item in initialSeeds) {
                    val docRef = db.collection("platforms").document(item.id)
                    batch.set(docRef, item.toMap())
                }
                batch.commit().await()
                Log.d(tag, "Explicitly seeded ${initialSeeds.size} initial platforms into Firestore.")
                Result.success(initialSeeds.size)
            } else {
                Log.d(tag, "Platforms collection already has ${snapshot.size()} documents. No seeding needed.")
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error seeding initial platforms to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    companion object {
        fun getInitialSeedData(): List<PlatformItem> {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            return listOf(
                PlatformItem(
                    id = "pw_study",
                    name = "PW Study",
                    description = "Interactive online study hub featuring comprehensive academic lessons, test modules, and course batches.",
                    logoUrl = "",
                    bannerUrl = "",
                    websiteUrl = "https://pwthor.live/study",
                    apkUrl = "",
                    category = "Study",
                    isActive = true,
                    isUpcoming = false,
                    sortOrder = 1,
                    createdAt = now,
                    updatedAt = now
                ),
                PlatformItem(
                    id = "pw_marco",
                    name = "PW Marco",
                    description = "Structured learning classroom with chapter-wise video sessions, practice materials, and batch updates.",
                    logoUrl = "",
                    bannerUrl = "",
                    websiteUrl = "https://pwmarco-og-06656c44b005.herokuapp.com",
                    category = "Education",
                    isActive = true,
                    isUpcoming = false,
                    sortOrder = 2,
                    createdAt = now,
                    updatedAt = now
                ),
                PlatformItem(
                    id = "as_multiverse",
                    name = "AS Multiverse",
                    description = "Multiverse educational ecosystem offering syllabus tracking, video lectures, and rich learning tracks.",
                    logoUrl = "",
                    bannerUrl = "",
                    websiteUrl = "https://asmultiverse.com/?tab=home&app=694690cbe1df5128eacc5caa",
                    category = "Courses",
                    isActive = true,
                    isUpcoming = false,
                    sortOrder = 3,
                    createdAt = now,
                    updatedAt = now
                ),
                PlatformItem(
                    id = "pw_study_parcham",
                    name = "PW Study Parcham",
                    description = "Specialized exam preparation portal with chapter notes, doubt resolution, and recorded course batches.",
                    logoUrl = "",
                    bannerUrl = "",
                    websiteUrl = "https://pw.studyparcham.in/#home-view",
                    category = "Study",
                    isActive = true,
                    isUpcoming = false,
                    sortOrder = 4,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }
}
