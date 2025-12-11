package com.pasitongtong.fitforu.viewmodel

import androidx.lifecycle.ViewModel
import com.pasitongtong.fitforu.model.SavedOutfit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 코디 저장/조회용 ViewModel
 * - 지금은 앱 메모리에만 저장
 * - 나중에 Supabase 연동 시 내부 구현만 바꾸면 됨
 */
class SavedOutfitViewModel : ViewModel() {

    // 내부에서만 수정 가능한 리스트
    private val _savedOutfits = MutableStateFlow<List<SavedOutfit>>(emptyList())

    // 화면(Composable)에서 구독하는 공개 StateFlow
    val savedOutfits: StateFlow<List<SavedOutfit>> = _savedOutfits.asStateFlow()

    /**
     * 코디 저장
     * 이미 같은 코디가 있으면(data class equals 기준) 중복 저장하지 않음
     */
    fun saveOutfit(outfit: SavedOutfit) {
        _savedOutfits.update { current ->
            // ✅ SavedOutfit 이 data class 라서 equals/hashCode 자동 구현됨
            if (current.contains(outfit)) current else current + outfit
        }
    }

    /**
     * 특정 코디 삭제
     */
    fun removeOutfit(outfit: SavedOutfit) {
        _savedOutfits.update { current ->
            current - outfit   // equals 기준으로 같은 객체 제거
        }
    }

    /**
     * 전체 삭제
     */
    fun clearAll() {
        _savedOutfits.value = emptyList()
    }

    // 🔜 나중에 Supabase 연동 시 여기서 원격 저장/동기화 추가하면 됨
}

