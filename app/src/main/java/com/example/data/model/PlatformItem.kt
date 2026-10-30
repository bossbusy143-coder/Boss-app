package com.example.data.model

data class PlatformItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val bannerUrl: String = "",
    val websiteUrl: String = "",
    val apkUrl: String = "",
    val category: String = "Education",
    val isActive: Boolean = true,
    val isUpcoming: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "description" to description,
            "logoUrl" to logoUrl,
            "bannerUrl" to bannerUrl,
            "websiteUrl" to websiteUrl,
            "apkUrl" to apkUrl,
            "category" to category,
            "isActive" to isActive,
            "isUpcoming" to isUpcoming,
            "sortOrder" to sortOrder,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): PlatformItem {
            return PlatformItem(
                id = id,
                name = map["name"] as? String ?: "",
                description = map["description"] as? String ?: "",
                logoUrl = map["logoUrl"] as? String ?: "",
                bannerUrl = map["bannerUrl"] as? String ?: "",
                websiteUrl = map["websiteUrl"] as? String ?: "",
                apkUrl = map["apkUrl"] as? String ?: "",
                category = map["category"] as? String ?: "Education",
                isActive = (map["isActive"] as? Boolean) ?: true,
                isUpcoming = (map["isUpcoming"] as? Boolean) ?: false,
                sortOrder = (map["sortOrder"] as? Number)?.toInt() ?: 0,
                createdAt = map["createdAt"] as? String ?: "",
                updatedAt = map["updatedAt"] as? String ?: ""
            )
        }
    }
}
