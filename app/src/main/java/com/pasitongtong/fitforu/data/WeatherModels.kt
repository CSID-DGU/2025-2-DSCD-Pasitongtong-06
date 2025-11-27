package com.pasitongtong.fitforu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class WeatherMain(
    @SerialName("temp_max") val tempMax: Double,
    @SerialName("temp_min") val tempMin: Double,
    @SerialName("temp") val temp: Double
)

@Serializable
data class WeatherDescription(
    @SerialName("description") val description: String
)

@Serializable
data class WeatherResponse(
    val weather: List<WeatherDescription>,
    val main: WeatherMain
)


// ---------- UI 에서 쓸 상태 모델 ----------

// 홈 화면에서 사용할 상태
// WeatherModels.kt (또는 HomeUiState 선언된 파일)

data class HomeUiState(
    val loading: Boolean = false,

    // 예전 값들
    val weatherText: String = "",
    val temperature: String = "",

    // 새 디자인용 값들
    val dateText: String = "",   // 예: "11월 25일 (화)"
    val maxTemp: String = "",    // 예: "15℃"
    val minTemp: String = "",    // 예: "7℃"
    val diffText: String = "",   // 예: "어제보다 5℃ 높아요"

    // 🔥 여기! 날씨 아이콘을 이모지(String)로 저장
    val weatherIcon: String = "🌥️"
)


