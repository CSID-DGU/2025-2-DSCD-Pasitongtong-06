package com.pasitongtong.fitforu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pasitongtong.fitforu.ui.screen.LoginScreen
import com.pasitongtong.fitforu.ui.screen.MainScreen
import com.pasitongtong.fitforu.ui.screen.ProfileSetupScreen
import com.pasitongtong.fitforu.ui.theme.FitForUTheme
import com.pasitongtong.fitforu.viewmodel.MainViewModel
import com.pasitongtong.fitforu.viewmodel.AuthUiState


class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FitForUTheme {
                val navController = rememberNavController()
                val authState by viewModel.authState.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    LaunchedEffect(authState) {
                        when (authState) {
                            is AuthUiState.Authed -> {
                                intent = Intent()   // 딥링크 재사용 방지

                                navController.navigate("main") {
                                    popUpTo(0)
                                    launchSingleTop = true
                                }
                            }

                            is AuthUiState.Error -> {
                                if ((authState as AuthUiState.Error).message == "PROFILE_REQUIRED") {
                                    navController.navigate("profileSetup") {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate("login") {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            }

                            else -> {
                                navController.navigate("login") {
                                    popUpTo(0)
                                    launchSingleTop = true
                                }
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "login"
                    ) {
                        composable("login") {
                            LoginScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("profileSetup") {
                            ProfileSetupScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("main") {
                            MainScreen(mainViewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
