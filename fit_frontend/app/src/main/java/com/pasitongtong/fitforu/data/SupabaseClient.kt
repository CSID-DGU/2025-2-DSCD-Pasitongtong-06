package com.pasitongtong.fitforu.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ---------- Supabase 기본 설정 ----------

// Supabase 프로젝트 URL / anon key
private const val SUPABASE_URL = "https://yfgeqnpnxhtzxsizwhle.supabase.co"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlmZ2VxbnBueGh0enhzaXp3aGxlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI3MzMwOTgsImV4cCI6MjA3ODMwOTA5OH0.-RSFiXi99wqL92cVSTwf6KJrr7qKR5aE9U-a4iT8RRE"

// 앱 전역에서 사용할 Supabase 클라이언트
val supabase = createSupabaseClient(
    supabaseUrl = "https://yfgeqnpnxhtzxsizwhle.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlmZ2VxbnBueGh0enhzaXp3aGxlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI3MzMwOTgsImV4cCI6MjA3ODMwOTA5OH0.-RSFiXi99wqL92cVSTwf6KJrr7qKR5aE9U-a4iT8RRE"
) {
    install(Auth)      // 인증
    install(Postgrest) // DB
    install(Storage)   // 스토리지
}

// ---------- 백엔드(auth 서버) 설정 ----------

/**
 * FastAPI(or 기타) 백엔드의 base URL.
 * 예) "https://fitforu-api.example.com"
 *
 * 실제 배포 주소로 바꿔 넣어야 한다!
 */
private const val BACKEND_BASE_URL = "https://YOUR_BACKEND_BASE_URL"   // TODO: 실제 주소로 교체

// Ktor HttpClient (이미 data 패키지에 HttpClientProvider.kt 가 있다고 가정)
private val httpClient get() = HttpClientProvider.client

/**
 * 로그인 후 프론트에서 성별을 선택하고,
 * accessToken + userId + gender 를 이용해 백엔드에 프로필을 저장하는 함수.
 *
 *   POST {BACKEND_BASE_URL}/auth/profile
 *   Authorization: Bearer <access_token>
 *   Body: { "user_id": "...", "gender": "M" | "F" }
 */
suspend fun saveUserProfile(
    accessToken: String,
    userId: String,
    gender: String
): Boolean {

    // JSON body 구성
    val payload = buildJsonObject {
        put("user_id", userId)
        put("gender", gender)
    }

    val response = httpClient.post("$BACKEND_BASE_URL/auth/profile") {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        contentType(ContentType.Application.Json)
        setBody(payload)
    }

    // 2xx 이면 true, 아니면 false
    return response.status.isSuccess()
}
