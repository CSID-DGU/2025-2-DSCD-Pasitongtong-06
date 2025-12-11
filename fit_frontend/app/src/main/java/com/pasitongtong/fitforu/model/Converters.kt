package com.pasitongtong.fitforu.model

import java.time.LocalDate

// RecommendResult 를 SavedOutfit 으로 변환
fun RecommendResult.toSavedOutfit(date: LocalDate): SavedOutfit {

    // 1) LLM 코멘트에서 body / tip 분리
    val rawComment = comment               // 전체 문장 (예: "~~설명입니다. Tip: 긴 귀걸이...")
    val (bodyText, tipFromComment) = rawComment
        .split("Tip:", limit = 2)
        .let { parts ->
            val body = parts[0].trim()
            val tip = parts.getOrNull(1)?.trim().orEmpty()   // "긴 귀걸이..." 이런 부분
            body to tip
        }

    // 2) 아이템 기반 fallback Tip (LLM에 Tip이 없을 때만 사용)
    val tipTextFallback = buildString {
        if (top != null)     append("상의: ${top.color} ${top.minor} ")
        if (bottom != null)  append("하의: ${bottom.color} ${bottom.minor} ")
        if (outer != null)   append("아우터: ${outer.color} ${outer.minor} ")
        if (onepiece != null)append("원피스: ${onepiece.color} ${onepiece.minor} ")
    }.ifBlank { "추천된 아이템 조합을 참고해보세요!" }

    return SavedOutfit(
        outerImageUrl = outer?.imageUrl,
        topImageUrl = top?.imageUrl,
        bottomImageUrl = bottom?.imageUrl,
        onepieceImageUrl = onepiece?.imageUrl,
        title = "",
        bodyText = bodyText,                         // 🔹 설명 부분만
        tip = if (tipFromComment.isNotBlank())       // 🔹 Tip 있으면 우선 사용
            tipFromComment
        else
            tipTextFallback,                    // 없으면 예전처럼 아이템 나열 Tip
        date = date
    )
}
