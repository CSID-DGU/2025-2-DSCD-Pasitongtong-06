package com.pasitongtong.fitforu.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class WeatherRepository(
    private val client: HttpClient,
    private val apiKey: String
) {

    // 서울 좌표 (임시 값). 나중에 GPS 연동 가능
    private val defaultLat = 37.5665
    private val defaultLon = 126.9780

    suspend fun getTodayWeather(): HomeUiState {
        // OpenWeather 현재 날씨 API
        val response: WeatherResponse = client.get(
            "https://api.openweathermap.org/data/2.5/weather"
        ) {
            parameter("lat", defaultLat)
            parameter("lon", defaultLon)
            parameter("appid", apiKey)
            parameter("units", "metric") // 섭씨
            parameter("lang", "kr")      // 한글 설명
        }.body()

        val tempC = response.main.temp.toInt()
        val description = response.weather.firstOrNull()?.description ?: "날씨 정보 없음"

        return HomeUiState(
            temperature = "${tempC}°C",
            weatherText = description,
            loading = false
        )
    }
}
