package com.pasitongtong.fitforu.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate          // ✅ 이걸로 통일


// 코디 안의 개별 아이템 (top, bottom, outer, onepiece)
@Serializable
data class RecommendCloth(
    val major: String,
    val minor: String,
    val color: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val fit: String? = null,
)

// 한 벌 코디
@Serializable
data class RecommendResult(
    val score: Double,
    val comment: String,
    val outer: RecommendCloth? = null,
    val top: RecommendCloth? = null,
    val bottom: RecommendCloth? = null,
    val onepiece: RecommendCloth? = null,
)

// /recommend/outfit 의 data 블록
@Serializable
data class RecommendData(
    val date: String,
    val temperature: Double,
    @SerialName("season_band") val seasonBand: String,
    @SerialName("user_shape") val userShape: String,
    val recommendations: List<RecommendResult>,
)

// 최상위 응답
@Serializable
data class RecommendResponse(
    val ok: Boolean,
    val data: RecommendData? = null,
)
