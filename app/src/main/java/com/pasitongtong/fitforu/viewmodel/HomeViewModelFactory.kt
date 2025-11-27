package com.pasitongtong.fitforu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pasitongtong.fitforu.data.HttpClientProvider
import com.pasitongtong.fitforu.data.WeatherRepository

class HomeViewModelFactory(
    private val apiKey: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val client = HttpClientProvider.client
            val repo = WeatherRepository(client, apiKey)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}