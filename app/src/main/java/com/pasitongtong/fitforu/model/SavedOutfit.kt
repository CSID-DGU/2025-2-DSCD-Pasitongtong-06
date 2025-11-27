package com.pasitongtong.fitforu.model

data class SavedOutfit(
    val id: String? = null,        // Supabase row id
    val imageRes: Int,
    val title: String,
    val bodyText: String,
    val tip: String,
    val createdAt: Long = System.currentTimeMillis()
)
