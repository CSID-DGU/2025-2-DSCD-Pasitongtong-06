package com.pasitongtong.fitforu.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import com.pasitongtong.fitforu.data.TodayWeather

class WeatherRepository(
    private val client: HttpClient,
    private val apiKey: String
) {
    private val defaultLat = 37.5665
    private val defaultLon = 126.9780

    suspend fun getTodayWeather(): TodayWeather {
        val response: WeatherResponse = client.get(
            "https://api.openweathermap.org/data/2.5/weather"
        ) {
            parameter("lat", defaultLat)
            parameter("lon", defaultLon)
            parameter("appid", apiKey)
            parameter("units", "metric")
            parameter("lang", "kr")
        }.body()

        val temp = response.main.temp
        val max = temp + 1            // 임시값
        val min = temp - 2            // 임시값
        val description = response.weather.firstOrNull()?.description ?: "날씨 정보 없음"

        // 어제 평균 기온이 없으므로 임시 계산
        val yesterdayAvg = (max + min) / 2 - 2

        return TodayWeather(
            temp = temp,
            maxTemp = max,
            minTemp = min,
            yesterdayAvg = yesterdayAvg,
            description = description
        )
    }

}
