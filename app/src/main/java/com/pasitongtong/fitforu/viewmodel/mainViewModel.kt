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
            try {
                val authUser = client.auth.currentUserOrNull()
                    ?: error("로그인 정보 없음")

                // ⚡ profiles 테이블에 내 프로필 upsert (없으면 insert, 있으면 update)
                client.postgrest["profiles"].upsert(
                    Profile(
                        user_id = userId,
                        gender = gender,
                        // height_cm, weight_kg, banned_items 는 지금은 null 로 둔다
                        height_cm = null,
                        weight_kg = null,
                        banned_items = null
                    )
                )

                // 로컬 상태 갱신
                _profile.value = Profile(
                    user_id = userId,
                    gender = gender
                )
                _authState.value = AuthUiState.Authed(email = authUser.email)
            } catch (e: Exception) {
                Log.e("MainViewModel", "createProfile 실패", e)

                // ❗ 임시 방편: DB 에러여도 앱은 진행되게 하기
                val email = client.auth.currentUserOrNull()?.email
                _profile.value = Profile(
                    user_id = userId,
                    gender = gender
                )
                _authState.value = AuthUiState.Authed(email = email)

                // 만약 꼭 에러를 보여주고 싶으면 위 Authed 대신 Error 로 바꾸면 됨
                // _authState.value = AuthUiState.Error("PROFILE_CREATE_FAILED")
            }
        }
    }

}

