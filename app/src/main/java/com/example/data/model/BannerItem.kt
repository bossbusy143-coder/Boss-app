package com.example.data.model

import com.google.firebase.Timestamp

data class BannerItem(
    val id: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val subtitle: String = "",
    val buttonText: String = "",
    val buttonUrl: String = "",
    val type: String = "image", // "image" or "telegram"
    val enabled: Boolean = true,
    val order: Int = 0,
    val createdAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "imageUrl" to imageUrl,
            "title" to title,
            "subtitle" to subtitle,
            "buttonText" to buttonText,
            "buttonUrl" to buttonUrl,
            "type" to type.lowercase(),
            "enabled" to enabled,
            "order" to order,
            "createdAt" to (createdAt ?: Timestamp.now())
        )
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): BannerItem {
            return BannerItem(
                id = id,
                imageUrl = map["imageUrl"] as? String ?: "",
                title = map["title"] as? String ?: "",
                subtitle = map["subtitle"] as? String ?: "",
                buttonText = map["buttonText"] as? String ?: "",
                buttonUrl = map["buttonUrl"] as? String ?: "",
                type = (map["type"] as? String)?.lowercase() ?: "image",
                enabled = (map["enabled"] as? Boolean) ?: true,
                order = (map["order"] as? Number)?.toInt() ?: 0,
                createdAt = map["createdAt"] as? Timestamp
            )
        }
    }
}
