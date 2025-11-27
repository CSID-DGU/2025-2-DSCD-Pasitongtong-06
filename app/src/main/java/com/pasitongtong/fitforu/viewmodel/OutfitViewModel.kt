package com.pasitongtong.fitforu.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.pasitongtong.fitforu.model.SavedOutfit

class OutfitViewModel : ViewModel() {

    private val _savedOutfits = MutableStateFlow<List<SavedOutfit>>(emptyList())
    val savedOutfits: StateFlow<List<SavedOutfit>> = _savedOutfits

    fun saveOutfit(outfit: SavedOutfit) {
        _savedOutfits.value = _savedOutfits.value + outfit
    }
}
