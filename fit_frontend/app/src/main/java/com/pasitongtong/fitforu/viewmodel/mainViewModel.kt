package com.pasitongtong.fitforu.viewmodel

import android.content.Context
import android.net.Uri
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

import com.pasitongtong.fitforu.model.RecommendData
import com.pasitongtong.fitforu.model.RecommendResult


import kotlinx.coroutines.flow.update
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.http.contentType
import com.pasitongtong.fitforu.model.RecommendResponse




// ---------- Ktor(백엔드 호출용) ----------
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.call.body
import io.ktor.client.request.forms.*
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


import com.pasitongtong.fitforu.model.WardrobeItem
import com.pasitongtong.fitforu.viewmodel.AuthUiState.ClosetUiState

import io.ktor.client.plugins.HttpTimeout

import io.ktor.client.request.forms.*
import io.ktor.http.*        // HttpHeaders
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.decodeFromString
import com.pasitongtong.fitforu.model.UploadClothesResponse
import kotlinx.coroutines.flow.update


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

    data class ClosetUiState(
        val items: List<WardrobeItem> = emptyList()
    )

}

data class OutfitRecommendUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val data: RecommendData? = null,
    val currentIndex: Int = 0,
) {
    val current: RecommendResult?
        get() = data?.recommendations?.getOrNull(currentIndex)
}

class MainViewModel : ViewModel() {

    // ===== 코디 추천 상태 =====
    private val _outfitUiState = MutableStateFlow(OutfitRecommendUiState())
    val outfitUiState: StateFlow<OutfitRecommendUiState> = _outfitUiState

    // Supabase 클라이언트
    private val client = SupabaseProvider.client

    // 🔹 FastAPI 백엔드 주소 (에뮬레이터)
    private val backendBaseUrl = "http://10.0.2.2:8000"

    // 🔹 Ktor HTTP 클라이언트
    private val httpClient = HttpClient(OkHttp) {


        // 1) 타임아웃 설정
        install(HttpTimeout) {
            requestTimeoutMillis = 5 * 60_000L   // 전체 요청 최대 5분
            connectTimeoutMillis = 2 * 60_000L   // 서버 연결 최대 2분
            socketTimeoutMillis  = 5 * 60_000L   // 응답 대기 최대 5분
        }


        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                }
            )
        }
    }

    private val json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
    }


    // 현재 로그인한 사용자 id
    var userId: String? = null
        private set

    private val _closetUiState = MutableStateFlow(ClosetUiState())
    val closetUiState: StateFlow<ClosetUiState> = _closetUiState

    // 🔹 홈 화면용 “저장된 체형 라벨” (저장하기 버튼에서 세팅)
    private val _savedBodyShapeLabel = MutableStateFlow<String?>(null)
    val savedBodyShapeLabel: StateFlow<String?> = _savedBodyShapeLabel.asStateFlow()


    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    // ───────────────────────────────────────
    //  체형 분석 결과(서버의 shape) 상태
    // ───────────────────────────────────────
    private val _lastAnalyzedImageUri = MutableStateFlow<Uri?>(null)
    val lastAnalyzedImageUri: StateFlow<Uri?> = _lastAnalyzedImageUri.asStateFlow()

    // ex) "Rectangle", "Hourglass" (백엔드 shape 값)
    private val _bodyShapeType = MutableStateFlow<String?>(null)
    val bodyShapeType: StateFlow<String?> = _bodyShapeType.asStateFlow()

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

                    // 백엔드 /users/me 한 번 체크 (shape, gender 등)
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

    // 외부에서 직접 이미지 Uri 를 넣어주고 싶을 때 사용 (필요 시)
    fun setLastAnalyzedImage(uri: Uri?) {
        _lastAnalyzedImageUri.value = uri
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
                _lastAnalyzedImageUri.value = null
                _bodyShapeType.value = null
                _savedBodyShapeLabel.value = null      // ✅ 홈 카드도 초기화
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
                val session = client.auth.currentSessionOrNull()
                    ?: error("Supabase 세션이 없습니다. (로그인 먼저 필요)")

                val accessToken = session.accessToken
                Log.d("Backend", "accessToken: $accessToken")

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
    //  🔥 GET /users/me : 백엔드 프로필 조회 (gender, shape 등)
    // ─────────────────────────────────────────────
    fun fetchBackendMe() {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading

            try {
                val session = client.auth.currentSessionOrNull()
                if (session == null) {
                    Log.e("Backend", "세션 없음, /users/me 호출 불가")
                    _authState.value = AuthUiState.Error("NO_SESSION")
                    return@launch
                }

                val accessToken = session.accessToken
                Log.d("Backend", "accessToken: $accessToken")

                val response = httpClient.get("$backendBaseUrl/users/me") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

                val bodyText: String = response.body()
                Log.d(
                    "Backend",
                    "GET /users/me status=${response.status.value}, body=$bodyText"
                )

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val email = client.auth.currentUserOrNull()?.email
                        _authState.value = AuthUiState.Authed(email = email)

                        // ✅ 서버에 이미 저장된 shape 를 _bodyShapeType 에 반영
                        runCatching {
                            val json = Json.parseToJsonElement(bodyText).jsonObject
                            val shape = json["shape"]?.jsonPrimitive?.contentOrNull
                            Log.d("Backend", "shape from /users/me = $shape")
                            _bodyShapeType.value = shape
                        }
                    }

                    HttpStatusCode.NotFound -> {
                        Log.d("Backend", "백엔드 프로필 없음 → PROFILE_REQUIRED")
                        _authState.value = AuthUiState.Error("PROFILE_REQUIRED")
                    }

                    else -> {
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

    // ─────────────────────────────────────────────
    //  🔥 POST /user/analyze-shape : 체형 분석 요청
    //      - gender : /users/me 에서 자동으로 조회
    //      - file   : 이미지 파일 (Uri)
    //  성공 시:
    //      - _lastAnalyzedImageUri 에 이미지 저장
    //      - /users/me 재호출해서 shape 가져와 _bodyShapeType 에 저장
    // ─────────────────────────────────────────────
    fun analyzeBodyShape(
        context: Context,
        imageUri: Uri,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val session = client.auth.currentSessionOrNull()
                    ?: error("Supabase 세션이 없습니다. (로그인 먼저 필요)")

                val accessToken = session.accessToken
                Log.d("BodyShape", "accessToken: $accessToken")

                // 화면에서 다시 쓸 수 있도록 Uri 저장
                _lastAnalyzedImageUri.value = imageUri

                // 1) 먼저 /users/me 에서 gender 가져오기
                val meResponse = httpClient.get("$backendBaseUrl/users/me") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                val meBodyText: String = meResponse.body()
                Log.d(
                    "BodyShape",
                    "GET /users/me status=${meResponse.status.value}, body=$meBodyText"
                )

                val meJson = Json.parseToJsonElement(meBodyText).jsonObject
                val genderText = meJson["gender"]?.jsonPrimitive?.contentOrNull

                Log.d("BodyShape", "resolved gender from /users/me = $genderText")

                if (genderText.isNullOrBlank()) {
                    onResult(false, "NO_GENDER")
                    return@launch
                }

                // 2) Uri -> ByteArray
                val imageBytes = context.contentResolver
                    .openInputStream(imageUri)
                    ?.use { it.readBytes() }
                    ?: error("이미지 파일을 읽을 수 없습니다.")

                // 3) multipart/form-data 로 POST /user/analyze-shape 호출
                val response = httpClient.submitFormWithBinaryData(
                    url = "$backendBaseUrl/user/analyze-shape",
                    formData = formData {
                        append("gender", genderText)
                        append(
                            "file",
                            imageBytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"body_image.jpg\""
                                )
                            }
                        )
                    }
                ) {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

                val bodyText: String = response.body()
                Log.d(
                    "BodyShape",
                    "POST /user/analyze-shape status=${response.status.value}, body=$bodyText"
                )

                if (!response.status.isSuccess()) {
                    onResult(false, bodyText)
                    return@launch
                }

                // 4) 분석 후 /users/me 다시 호출해서 최신 shape 가져오기
                val afterResponse = httpClient.get("$backendBaseUrl/users/me") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }
                val afterBody: String = afterResponse.body()
                Log.d(
                    "BodyShape",
                    "GET /users/me(after analyze) status=${afterResponse.status.value}, body=$afterBody"
                )

                runCatching {
                    val json = Json.parseToJsonElement(afterBody).jsonObject
                    val shape = json["shape"]?.jsonPrimitive?.contentOrNull
                    Log.d("BodyShape", "parsed shape(after) = $shape")
                    _bodyShapeType.value = shape    // ✅ 분석 결과를 상태에 반영
                }

                onResult(true, null)
            } catch (e: Exception) {
                Log.e("BodyShape", "analyzeBodyShape 실패: ${e.message}", e)
                onResult(false, e.message)
            }
        }
    }

    // 🔹 “저장하기” 버튼에서 호출 → 홈 화면용 카드에 쓸 라벨 저장
    fun saveBodyShapeLabel(label: String) {
        _savedBodyShapeLabel.value = label    // ✅ HomeScreen 이 이 값을 봄
    }

    // ─────────────────────────────────────────────
//  🔥 옷 사진 업로드: /clothes/upload
//  - 입력: 로컬 Uri (카메라/갤러리에서 받은 것)
//  - 처리: multipart/form-data 로 서버에 전송
//  - 결과: 누끼 딴 이미지의 URL(String) 을 콜백으로 넘김
// ─────────────────────────────────────────────
    fun uploadClothesImage(
        context: Context,
        imageUri: Uri,
        onResult: (Boolean, String?) -> Unit   // success, urlOrError
    ) {
        viewModelScope.launch {
            try {
                // 1) Supabase 세션에서 accessToken 꺼내기
                val session = client.auth.currentSessionOrNull()
                    ?: error("Supabase 세션이 없습니다. (로그인 먼저 필요)")

                val accessToken = session.accessToken
                Log.d("ClothesUpload", "accessToken: $accessToken")

                // 2) Uri -> ByteArray
                val imageBytes = context.contentResolver
                    .openInputStream(imageUri)
                    ?.use { it.readBytes() }
                    ?: error("이미지 파일을 읽을 수 없습니다.")

                // 3) multipart/form-data 로 POST /clothes/upload 호출
                val response = httpClient.submitFormWithBinaryData(
                    url = "$backendBaseUrl/clothes/upload",
                    formData = formData {
                        append(
                            "file",
                            imageBytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "filename=\"closet_image.jpg\""
                                )
                            }
                        )
                    }
                ) {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                }

                val bodyText: String = response.bodyAsText()
                Log.d(
                    "ClothesUpload",
                    "POST /clothes/upload status=${response.status.value}, body=$bodyText"
                )

                // HTTP 코드가 200대 아니면 실패 처리
                if (!response.status.isSuccess()) {
                    onResult(false, bodyText)
                    return@launch
                }

                // 4) 서버 응답(JSON)을 UploadClothesResponse로 파싱
                val uploadResult = Json {
                    ignoreUnknownKeys = true          // 백엔드에서 필드 더 줘도 에러 안 나게
                }.decodeFromString<UploadClothesResponse>(bodyText)

                if (!uploadResult.ok || uploadResult.items.isEmpty()) {
                    Log.e("ClothesUpload", "응답 ok=false 이거나 items 비어 있음")
                    onResult(false, "UPLOAD_FAILED")
                    return@launch
                }

                // 5) 새로 저장된 옷 목록을 ViewModel 상태에 추가
                val newItems = uploadResult.items

                _closetUiState.update { state ->
                    state.copy(items = state.items + newItems)
                }

                // 6) 콜백으로는 첫 번째 아이템의 image_url을 넘겨주기 (기존 onResult 사용 로직 유지)
                val firstUrl = newItems.first().image_url
                Log.d("ClothesUpload", "parsed image_url = $firstUrl")
                onResult(true, firstUrl)

            } catch (e: Exception) {
                Log.e("ClothesUpload", "uploadClothesImage 실패: ${e.message}", e)
                onResult(false, e.message)
            }
        }
    }

    fun loadOutfitRecommendations() {
        viewModelScope.launch {
            try {
                _outfitUiState.update { it.copy(isLoading = true) }

                val session = client.auth.currentSessionOrNull()
                    ?: error("세션 없음: 로그인 먼저 해주세요")

                val accessToken = session.accessToken

                // 🔥 인증 헤더 추가!
                val httpResponse = httpClient.post("$backendBaseUrl/recommend/outfit") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    contentType(ContentType.Application.Json)
                    setBody(emptyMap<String, String>())
                }

                val raw = httpResponse.bodyAsText()
                Log.d("Recommend", "raw body = $raw")

                val response: RecommendResponse = httpResponse.body()


                _outfitUiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        data = response.data,
                        currentIndex = 0
                    )
                }
            } catch (e: Exception) {
                _outfitUiState.update {
                    it.copy(isLoading = false, error = e.message ?: "알 수 없는 오류")
                }
            }
        }
    }

    fun showNextRecommendation() {
        val state = _outfitUiState.value
        val size = state.data?.recommendations?.size ?: return
        if (size == 0) return

        val next = (state.currentIndex + 1) % size
        _outfitUiState.update { it.copy(currentIndex = next) }
    }

    fun confirmCurrentRecommendation() {
        val current = _outfitUiState.value.current ?: return
        // TODO: 선택된 코디를 Supabase에 저장하거나 코디북에 넣는 로직
        Log.d("Outfit", "선택된 코디 score=${current.score}")
    }






    override fun onCleared() {
        super.onCleared()
        httpClient.close()
    }
}


