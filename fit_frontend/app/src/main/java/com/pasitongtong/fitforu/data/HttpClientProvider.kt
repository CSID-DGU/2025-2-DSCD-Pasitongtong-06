package com.pasitongtong.fitforu.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientProvider {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    val client: HttpClient = HttpClient(OkHttp) {

        // 🔥 타임아웃 여유 있게 (예: 요청 5분, 커넥션 2분, 소켓 5분)
        install(HttpTimeout) {
            requestTimeoutMillis = 5 * 60_000L   // 전체 요청 시간 5분
            connectTimeoutMillis = 2 * 60_000L   // 서버 연결 2분
            socketTimeoutMillis  = 5 * 60_000L   // 데이터 송수신 5분
        }

        install(ContentNegotiation) {
            json(json)
        }
    }
}
