package com.pasitongtong.fitforu.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Style
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 앱 내의 전체 화면 경로와 하단 네비게이션 아이템을 정의합니다.
 */
sealed class Screen(
    val route: String,
    val title: String? = null,
    val icon: ImageVector? = null
) {
    // 하단 탭에 표시될 화면들
    object Home : Screen(route = "home", title = "홈", icon = Icons.Default.Home)
    object Closet : Screen(route = "closet", title = "옷장", icon = Icons.Default.Checkroom)
    object Stylebook : Screen(route = "stylebook", title = "코디 북", icon = Icons.Default.Style)
    object Calendar : Screen(route = "calendar", title = "캘린더", icon = Icons.Default.CalendarMonth)

    // 하단 탭에 표시되지 않는 화면들
    object SkeletalAnalysis : Screen(route = "skeletal_analysis")
    object AnalysisResult : Screen(route = "analysis_result")
    object Login : Screen(route = "login")

    // ✅ date 를 쿼리 파라미터로 받는 라우트
    object OutfitDetail : Screen("outfitDetail?date={date}") {

        // 네비게이션할 때 쓸 헬퍼 함수
        fun routeWithDate(date: String?): String {
            // date가 null이면 빈 문자열로 보냄
            return "outfitDetail?date=${date ?: ""}"
        }
    }

    // ✅ 저장된 코디 리스트 화면
    object SavedOutfitList : Screen(
        route = "saved_outfit_list",
        title = "저장한 코디 보기"
    )

    // Screen.kt 안에
    object AddClothes : Screen(
        route = "add_clothes",
        title = "옷 추가"
    )

}
