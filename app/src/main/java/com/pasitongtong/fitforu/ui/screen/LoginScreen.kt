package com.pasitongtong.fitforu.ui.screen

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.pasitongtong.fitforu.viewmodel.AuthUiState
import com.pasitongtong.fitforu.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import com.pasitongtong.fitforu.R


@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthUiState.Loading

    // 🔹 로그인 성공하면 자동으로 Home으로 네비게이션
    LaunchedEffect(key1 = authState) {
        if (authState is AuthUiState.Authed) {

            // 💥 딥링크 intent 한 번 소비해서 중복 콜백 방지
            (context as? Activity)?.intent = Intent()

            if (navController.currentDestination?.route != "main") {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    // ===== UI =====
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("FitForU", style = MaterialTheme.typography.displaySmall)

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = authState) {
                AuthUiState.Idle -> {
                    Text("카카오 로그인을 해주세요.")
                }
                is AuthUiState.Authed -> {
                    val email = state.email ?: "사용자"
                    Text("안녕하세요, $email 님!")
                }
                is AuthUiState.Error -> {
                    Text(
                        text = "오류: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                AuthUiState.Loading -> {
                    Text("로그인 중입니다...")
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            // 카카오 로그인 버튼
            Image(
                painter = painterResource(id = R.drawable.kakao_login_large_wide),
                contentDescription = "카카오 로그인",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) {
                        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                                if (error != null) {
                                    Log.e("LoginScreen", "카카오톡 로그인 실패", error)
                                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) return@loginWithKakaoTalk

                                    UserApiClient.instance.loginWithKakaoAccount(
                                        context,
                                        callback = kakaoCallback(viewModel, scope)
                                    )
                                } else if (token != null) {
                                    kakaoCallback(viewModel, scope)(token, null)
                                }
                            }
                        } else {
                            UserApiClient.instance.loginWithKakaoAccount(
                                context,
                                callback = kakaoCallback(viewModel, scope)
                            )
                        }
                    }
            )
        }

        if (isLoading) {
            CircularProgressIndicator()
        }
    }
}

// 콜백 분리
private fun kakaoCallback(
    viewModel: MainViewModel,
    scope: kotlinx.coroutines.CoroutineScope
): (OAuthToken?, Throwable?) -> Unit = { token, error ->
    if (error != null) {
        Log.e("LoginScreen", "카카오 로그인 실패", error)
    } else if (token != null) {
        val idToken = token.idToken
        if (idToken == null) {
            Log.e("LoginScreen", "카카오 로그인 성공했지만 idToken 이 null 입니다.")
        } else {
            Log.i("LoginScreen", "카카오 로그인 성공. idToken: $idToken")
            scope.launch {
                viewModel.signInWithKakao(idToken)
            }
        }
    }
}
