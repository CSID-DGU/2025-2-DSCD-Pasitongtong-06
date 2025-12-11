package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pasitongtong.fitforu.ui.Screen
import com.pasitongtong.fitforu.viewmodel.HomeViewModel
import com.pasitongtong.fitforu.viewmodel.HomeViewModelFactory
import com.pasitongtong.fitforu.viewmodel.MainViewModel
import com.pasitongtong.fitforu.viewmodel.SavedOutfitViewModel
import java.time.LocalDate

/**
 * 로그인 후 진입하는 메인 프레임 화면입니다.
 * 하단 네비게이션 바와 각 탭에 해당하는 화면들을 포함합니다.
 */
@Composable
fun MainScreen(mainViewModel: MainViewModel) {

    val navController = rememberNavController()

    // ✅ Home 화면용 ViewModel (한 번만 생성해서 공유)
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory("0449c5e0de9ca76dea138c059277b8a5")
    )

    // ✅ 코디 저장용 ViewModel (캘린더/코디북/상세 화면에서 같이 사용)
    val savedOutfitViewModel: SavedOutfitViewModel = viewModel()

    // 하단 네비게이션에 포함될 화면 리스트
    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Closet,
        Screen.Stylebook,
        Screen.Calendar
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                        label = { Text(screen.title!!) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            // ───────── Home ─────────
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    homeViewModel = homeViewModel,
                    mainViewModel = mainViewModel
                )
            }

            // ───────── 옷장 ─────────
            composable(Screen.Closet.route) {
                ClosetScreen(navController, mainViewModel)
            }

            composable(Screen.AddClothes.route) {
                AddClothesScreen(
                    navController = navController,
                    mainViewModel = mainViewModel
                )
            }

            // ───────── 코디 북 (저장된 코디 전체) ─────────
            composable(Screen.Stylebook.route) {
                StylebookScreen(
                    navController = navController,
                    savedOutfitViewModel = savedOutfitViewModel
                )
            }

            // ───────── 캘린더 (날짜별 코디) ─────────
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    navController = navController,
                    savedOutfitViewModel = savedOutfitViewModel
                )
            }

            // ───────── 체형 분석 ─────────
            composable(Screen.SkeletalAnalysis.route) {
                // 메인에서 받은 mainViewModel 그대로 사용
                SkeletalAnalysisScreen(
                    navController = navController,
                    viewModel = mainViewModel
                )
            }

            // ───────── 분석 결과 ─────────
            composable(Screen.AnalysisResult.route) {
                AnalysisResultScreen(
                    navController = navController,
                    mainViewModel = mainViewModel
                )
            }

            // ───────── 코디 상세 + 저장 ─────────
            composable(
                route = Screen.OutfitDetail.route,   // 예: "outfitDetail?date={date}"
                arguments = listOf(
                    navArgument("date") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->

                // "2025-12-24" 형태의 문자열 > LocalDate? 로 변환
                val dateArg = backStackEntry.arguments?.getString("date")
                val selectedDate: LocalDate? =
                    dateArg
                        ?.takeIf { it.isNotBlank() }
                        ?.let { LocalDate.parse(it) }

                OutfitDetailScreen(
                    navController = navController,
                    viewModel = mainViewModel,
                    selectedDate = selectedDate,
                    onSaveOutfit = { outfit ->
                        // ✅ 상세 화면에서 체크 누르면 공용 ViewModel 에 저장
                        savedOutfitViewModel.saveOutfit(outfit)
                    }
                )
            }
        }
    }
}
