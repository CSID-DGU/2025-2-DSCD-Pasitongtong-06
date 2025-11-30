package com.pasitongtong.fitforu

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import android.util.Log
import com.kakao.sdk.common.util.Utility

class GlobalApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1) Kakao SDK 초기화 (네이티브 앱 키)
        KakaoSdk.init(this, "07d770992a4f1828d5d4b0d8a95f0369")

        // 2) 실제 키 해시 로그로 출력
        val keyHash = Utility.getKeyHash(this)
        Log.d("KAKAO", "keyHash = $keyHash")
    }
}
