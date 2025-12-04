package com.pasitongtong.fitforu.model

import kotlinx.serialization.Serializable

@Serializable
data class WardrobeItem(
    val id: String,
    val user_id: String,
    val image_url: String,
    val major_category: String,
    val minor_category: String,
    val color: String? = null,
    val attributes: String? = null,
    val created_at: String
)

@Serializable
data class UploadClothesResponse(
    val ok: Boolean,
    val saved_count: Int,
    val items: List<WardrobeItem>
)
