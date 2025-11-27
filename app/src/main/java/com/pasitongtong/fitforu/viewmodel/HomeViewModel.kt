package com.pasitongtong.fitforu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitongtong.fitforu.data.HomeUiState
import com.pasitongtong.fitforu.data.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeViewModel(
    private val repo: WeatherRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    // ViewModel 이 생성될 때 한 번 자동으로 날씨 조회
    init {
        refresh()
    }

    // 🔹 날씨 설명 -> 이모지 매핑
    private fun mapDescriptionToEmoji(description: String): String {
        val lower = description.lowercase()

        return when {
            "비" in lower || "rain" in lower -> "🌧️"
            "눈" in lower || "snow" in lower -> "❄️"
            "맑" in lower || "clear" in lower -> "☀️"
            "구름" in lower || "cloud" in lower -> "⛅"
            else -> "🌥️"  // 기본값 (구름 사이 햇살)
        }
    }

    private fun todayStr(): String =
        LocalDate.now().format(
            DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN)
        )

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val w = repo.getTodayWeather()   // TodayWeather

                val icon = mapDescriptionToEmoji(w.description)
                val today = todayStr()

                val diff = w.temp - w.yesterdayAvg
                val diffText = when {
                    diff > 0 -> "어제보다 ${diff.toInt()}℃ 높아요"
                    diff < 0 -> "어제보다 ${kotlin.math.abs(diff.toInt())}℃ 낮아요"
                    else -> "어제와 동일해요"
                }

                _state.value = _state.value.copy(
                    loading     = false,
                    weatherText = w.description,
                    temperature = "${w.temp.toInt()}℃ / ${w.minTemp.toInt()}℃",
                    dateText    = today,
                    maxTemp     = "${w.maxTemp.toInt()}℃",
                    minTemp     = "${w.minTemp.toInt()}℃",
                    diffText    = diffText,
                    weatherIcon = icon        // 👈 이모지 저장
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    weatherText = "날씨 불러오기 실패"
                )
            }
        }
    }
}

