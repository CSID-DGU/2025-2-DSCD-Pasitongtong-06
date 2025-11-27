package com.pasitongtong.fitforu.viewmodel

import com.pasitongtong.fitforu.data.HomeUiState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitongtong.fitforu.data.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val temperature: String = "",
    val weatherText: String = "",
    val loading: Boolean = true
)

class HomeViewModel(private val repo: WeatherRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            try {
                val ui = repo.getTodayWeather()
                _state.value = ui
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    weatherText = "날씨 불러오기 실패"
                )
            }
        }
    }
}
