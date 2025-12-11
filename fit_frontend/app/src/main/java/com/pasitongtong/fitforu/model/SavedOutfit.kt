// SavedOutfit.kt
package com.pasitongtong.fitforu.model

import java.time.LocalDate

data class SavedOutfit(
    val id: String? = null,            // Supabase row id (있으면)
    val outerImageUrl: String? = null, // 아우터
    val topImageUrl: String? = null,   // 상의
    val bottomImageUrl: String? = null,// 하의
    val onepieceImageUrl: String? = null, // 원피스

    val title: String,
    val bodyText: String,
    val tip: String,

    val createdAt: Long = System.currentTimeMillis(),

    // 캘린더용 날짜
    val date: LocalDate? = null
)
