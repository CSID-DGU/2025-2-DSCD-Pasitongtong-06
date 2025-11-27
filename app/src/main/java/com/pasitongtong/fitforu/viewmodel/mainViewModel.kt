package com.pasitongtong.fitforu.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasitongtong.fitforu.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// Supabase profiles 테이블과 매핑
@Serializable
data class Profile(
    val user_id: String,
    val gender: String? = null,
    val height_cm: Int? = null,
    val weight_kg: Int? = null,
    val banned_items: List<String>? = null
)

// 인증 상태
sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Authed(val email: String?) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class MainViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    // 현재 로그인한 사용자 id
    var userId: String? = null
        private set

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    init {
        // 앱 켰을 때 기존 세션 있으면 복구
        viewModelScope.launch {
            try {
                val user = client.auth.retrieveUserForCurrentSession(updateSession = true)
                if (user != null) {
                    userId = user.id
                    _authState.value = AuthUiState.Authed(email = user.email)
                    loadMyProfile(user.id)
                } else {
                    _authState.value = AuthUiState.Idle
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "retrieveUserForCurrentSession 실패", e)
                _authState.value = AuthUiState.Idle
            }
        }
    }

    /** 카카오 로그인 */
    fun signInWithKakao(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading

            runCatching {
                client.auth.signInWith(IDToken) {
                    provider = Kakao
                    this.idToken = idToken
                }
            }.onSuccess {
                val user = client.auth.currentUserOrNull()
                if (user != null) {
                    userId = user.id
                    _authState.value = AuthUiState.Authed(email = user.email)
                    loadMyProfile(user.id)
                } else {
                    _authState.value = AuthUiState.Error("카카오 로그인 후 사용자 정보를 찾지 못했습니다.")
                }
            }.onFailure { e ->
                Log.e("SupabaseAuth", "signInWithKakao 실패", e)
                _authState.value = AuthUiState.Error(e.message ?: "카카오 로그인 실패")
            }
        }
    }

    /** 로그아웃 */
    fun signOut() {
        viewModelScope.launch {
            try {
                client.auth.signOut()
            } catch (_: Exception) {
            } finally {
                userId = null
                _authState.value = AuthUiState.Idle
                _profile.value = null
            }
        }
    }

    /** profiles 테이블에서 내 프로필 가져오기 */
    fun loadMyProfile(userId: String) = viewModelScope.launch {
        try {
            val profile = client.postgrest["profiles"]
                .select {
                    filter { eq("user_id", userId) }
                    limit(1)
                }
                .decodeSingle<Profile>()

            _profile.value = profile
            val currentEmail = client.auth.currentUserOrNull()?.email
            _authState.value = AuthUiState.Authed(email = currentEmail)
        } catch (e: Exception) {
            // 프로필 없음 → ProfileSetupScreen 필요
            Log.i("MainViewModel", "profile 없음, PROFILE_REQUIRED. ${e.message}")
            _profile.value = null
            _authState.value = AuthUiState.Error("PROFILE_REQUIRED")
        }
    }

    /** 프로필 최초 생성 (성별 선택 후) */
    fun createProfile(userId: String, gender: String) {
        viewModelScope.launch {
            runCatching {
                val authUser = client.auth.currentUserOrNull()
                    ?: error("로그인 정보 없음")

                val email = authUser.email

                // 1) public.users upsert
                client.postgrest["users"].upsert(
                    mapOf(
                        "id" to userId,
                        "email" to email
                    )
                )

                // 2) profiles insert (없으면 생성, 있으면 업데이트)
                client.postgrest["profiles"].upsert(
                    mapOf(
                        "user_id" to userId,
                        "gender" to gender,
                    )
                )
            }.onSuccess {
                loadMyProfile(userId)
            }.onFailure { e ->
                Log.e("MainViewModel", "createProfile 실패", e)
                _authState.value = AuthUiState.Error("PROFILE_CREATE_FAILED")
            }
        }
    }
}

