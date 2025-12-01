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

// ---------- Ktor(백엔드 호출용) ----------
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.call.body
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.http.isSuccess

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

    // Supabase 클라이언트
    private val client = SupabaseProvider.client

    // 🔹 FastAPI 백엔드 주소
    // 에뮬레이터: http://10.0.2.2:8000
    private val backendBaseUrl = "http://10.0.2.2:8000"

    // 🔹 Ktor HTTP 클라이언트
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                }
            )
        }
    }

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

                    // Supabase profiles 테이블에서 읽기 (있으면)
                    loadMyProfile(user.id)

                    // 백엔드 /users/me 도 한 번 체크
                    fetchBackendMe()
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

                    // Supabase profiles
                    loadMyProfile(user.id)

                    // ✅ 로그인 직후 백엔드 /users/me 호출
                    fetchBackendMe()
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

    /** Supabase profiles 테이블에서 내 프로필 가져오기 */
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
            Log.i("MainViewModel", "Supabase profile 없음, PROFILE_REQUIRED. ${e.message}")
            _profile.value = null
            _authState.value = AuthUiState.Error("PROFILE_REQUIRED")
        }
    }

    // ─────────────────────────────────────────────
    //  🔥 POST /users/profile : 성별을 백엔드에 저장
    // ─────────────────────────────────────────────
    fun postBackendProfile(
        gender: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // 1) Supabase 세션에서 accessToken 꺼내기
                val session = client.auth.currentSessionOrNull()
                    ?: error("Supabase 세션이 없습니다. (로그인 먼저 필요)")

                val accessToken = session.accessToken
                Log.d("Backend", "accessToken: $accessToken")

                // 2) POST /users/profile 호출 (JSON body: { "gender": "male" } 이런 식)
                val response = httpClient.post("$backendBaseUrl/users/profile") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody(
                        mapOf(
                            "gender" to gender
                        )
                    )
                }

                val bodyText: String = response.body()
                Log.d(
                    "Backend",
                    "POST /users/profile status=${response.status.value}, body=$bodyText"
                )

                if (response.status.isSuccess()) {
                    // 성공이면 앞으로는 GET /users/me 가 200이 떨어져야 함
                    _authState.value =
                        AuthUiState.Authed(email = client.auth.currentUserOrNull()?.email)
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("Backend", "POST /users/profile 실패: ${e.message}", e)
                onResult(false)
            }
        }
    }

    // ─────────────────────────────────────────────
    //  🔥 GET /users/me : 백엔드 프로필 존재 여부 확인
    // ─────────────────────────────────────────────
    fun fetchBackendMe() {
        viewModelScope.launch {
            // 필요하다면 로딩 상태 표시
            _authState.value = AuthUiState.Loading

            try {
                // 1) Supabase 세션에서 access_token 꺼내기
                val session = client.auth.currentSessionOrNull()
                if (session == null) {
                    Log.e("Backend", "세션 없음, /users/me 호출 불가")
                    _authState.value = AuthUiState.Error("NO_SESSION")
                    return@launch
                }

                val accessToken = session.accessToken
                Log.d("Backend", "accessToken: $accessToken")

                // 2) GET /users/me 호출
                val response = httpClient.get("$backendBaseUrl/users/me") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

                val bodyText: String = response.body()
                Log.d(
                    "Backend",
                    "GET /users/me status=${response.status.value}, body=$bodyText"
                )

                // 3) 상태코드에 따라 분기
                when (response.status) {
                    HttpStatusCode.OK -> {
                        // 백엔드에도 프로필이 있는 정상 상태
                        val email = client.auth.currentUserOrNull()?.email
                        _authState.value = AuthUiState.Authed(email = email)
                    }

                    HttpStatusCode.NotFound -> {
                        // 백엔드가 "프로필이 없다" 라고 알려준 상태 → 프로필 설정 화면으로
                        Log.d("Backend", "백엔드 프로필 없음 → PROFILE_REQUIRED")
                        _authState.value = AuthUiState.Error("PROFILE_REQUIRED")
                    }

                    else -> {
                        // 그 외 status 는 전부 백엔드 에러로 처리
                        Log.e("Backend", "예상 밖 상태코드: ${response.status}")
                        _authState.value =
                            AuthUiState.Error("BACKEND_ERROR_${response.status.value}")
                    }
                }
            } catch (e: Exception) {
                Log.e("Backend", "/users/me 호출 실패: ${e.message}", e)
                _authState.value = AuthUiState.Error("BACKEND_EXCEPTION")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }
}
