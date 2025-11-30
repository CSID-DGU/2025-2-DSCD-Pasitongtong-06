package com.pasitongtong.fitforu

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * 앱 전체에서 사용될 단 하나의 Supabase 클라이언트 인스턴스를 제공하는 싱글톤 객체입니다.
 */

object SupabaseProvider {

    val client: SupabaseClient = createSupabaseClient(
        // ❗️아래 URL과 KEY는 실제 본인의 프로젝트 값으로 반드시 교체해야 합니다.
        supabaseUrl = "https://yfgeqnpnxhtzxsizwhle.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlmZ2VxbnBueGh0enhzaXp3aGxlIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI3MzMwOTgsImV4cCI6MjA3ODMwOTA5OH0.-RSFiXi99wqL92cVSTwf6KJrr7qKR5aE9U-a4iT8RRE"
    ) {
        install(Auth)       // 인증 기능
        install(Postgrest)  // 데이터베이스 기능
        install(Storage)    // 파일 저장소 기능
    }

}
