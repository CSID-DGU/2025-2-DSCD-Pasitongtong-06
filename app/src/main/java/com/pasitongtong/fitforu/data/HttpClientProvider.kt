package com.pasitongtong.fitforu.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientProvider {

    // JSON 설정 – 모르는 필드는 무시
    private val json = Json {
        ignoreUnknownKeys = true   // <- 이게 없어서도 자주 크래시 난다
    }

    val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
    }
}
