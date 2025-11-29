package com.pasitongtong.fitforu.ui.screen

import androidx.lifecycle.viewmodel.compose.viewModel
import com.pasitongtong.fitforu.viewmodel.HomeViewModel


import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pasitongtong.fitforu.ui.Screen
import com.pasitongtong.fitforu.viewmodel.MainViewModel

import com.pasitongtong.fitforu.viewmodel.HomeViewModelFactory
import com.pasitongtong.fitforu.viewmodel.SavedOutfitViewModel


// ✅ Home 화면용 ViewModel / Factory (ui 패키지에 만든 파일 기준)

/**
 * 로그인 후 진입하는 메인 프레임 화면입니다.
 * 하단 네비게이션 바와 각 탭에 해당하는 화면들을 포함합니다.
 */
@Composable
fun MainScreen(mainViewModel: MainViewModel) {

    val navController = rememberNavController()

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

            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory("0449c5e0de9ca76dea138c059277b8a5")
                )
                HomeScreen(navController, homeViewModel)
            }

            composable(Screen.Closet.route) {
                ClosetScreen(navController, mainViewModel)
            }

            composable(Screen.Stylebook.route) {
                StylebookScreen(navController)
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(navController)
            }

            composable(Screen.SkeletalAnalysis.route) {
                SkeletalAnalysisScreen(navController)
            }

            composable(Screen.AnalysisResult.route) {
                AnalysisResultScreen(navController = navController)
            }

            composable(Screen.OutfitDetail.route) {
                val savedOutfitViewModel: SavedOutfitViewModel = viewModel()

                OutfitDetailScreen(
                    navController = navController,
                    onSaveOutfit = { outfit ->
                        savedOutfitViewModel.saveOutfit(outfit)
                    }
                )
            }
        }

    }
}
