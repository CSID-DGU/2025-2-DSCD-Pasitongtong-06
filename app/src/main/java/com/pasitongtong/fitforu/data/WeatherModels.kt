package com.pasitongtong.fitforu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- OpenWeather 응답용 모델 ----------

@Serializable
data class WeatherMain(
    @SerialName("temp") val temp: Double    // 현재 기온 (섭씨로 요청할 것)
)

@Serializable
data class WeatherDescription(
    @SerialName("description") val description: String
)

@Serializable
data class WeatherResponse(
    val weather: List<WeatherDescription>,
    val main: WeatherMain,
    val name: String? = null               // 도시 이름 (사용 안 하면 써도 되고 안 써도 됨)
)

// ---------- UI 에서 쓸 상태 모델 ----------

data class HomeUiState(
    val temperature: String = "",          // "23°C" 이런 형태의 문자열
    val weatherText: String = "",          // "맑음" 등 설명
    val loading: Boolean = true            // 로딩 중 여부
)
