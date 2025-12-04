package com.pasitongtong.fitforu.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout  // 👈 이 import 추가 필수
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientProvider {

    // JSON 설정 – 모르는 필드는 무시
    private val json = Json {
        ignoreUnknownKeys = true
    }

    val client: HttpClient = HttpClient(OkHttp) {
        // 1. 타임아웃 설정 추가 (기본값은 보통 10초라 AI 작업엔 부족함)
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000  // 요청 전체 타임아웃 (2분)
            connectTimeoutMillis = 60_000   // 연결 타임아웃 (1분)
            socketTimeoutMillis = 120_000   // 데이터 수신 대기 타임아웃 (2분)
        }

        // 2. 기존 설정 유지
        install(ContentNegotiation) {
            json(json)
        }
    }
}