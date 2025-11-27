package com.pasitongtong.fitforu.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.pasitongtong.fitforu.viewmodel.MainViewModel

@Composable
fun ProfileSetupScreen(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current

    // 성별 선택 상태 (M / F)
    var selectedGender by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "기본 프로필을 입력해주세요!",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "성별을 선택해주세요 (필수)")
        Spacer(modifier = Modifier.height(12.dp))

        // 🔹 성별 선택 버튼 2개 (남 / 여)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GenderChoiceButton(
                label = "남(M)",
                selected = selectedGender == "M",
                onClick = { selectedGender = "M" }
            )
            GenderChoiceButton(
                label = "여(F)",
                selected = selectedGender == "F",
                onClick = { selectedGender = "F" }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val userId = viewModel.userId

                // 1) 로그인 정보 확인
                if (userId == null) {
                    Toast.makeText(
                        context,
                        "로그인 정보가 없습니다. 다시 로그인 해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                // 2) 성별 선택 확인
                if (selectedGender == null) {
                    Toast.makeText(
                        context,
                        "성별을 선택해주세요!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                // 3) 내부에서 사용할 gender 값 (백엔드/DB 용)
                val genderValue = if (selectedGender == "M") "male" else "female"

                // ✅ 여기서 MainViewModel 쪽으로 프로필 저장 요청 전달
                //    -> MainViewModel.createProfile() 안에서
                //       POST /auth/profile 호출하도록 구현하면 됨
                viewModel.createProfile(userId, genderValue)

                // 4) 메인 화면으로 이동 (프로필 설정 화면 스택 제거)
                navController.navigate("main") {
                    popUpTo("profileSetup") { inclusive = true }
                    launchSingleTop = true
                }
            }
        ) {
            Text("저장하고 시작하기")
        }
    }
}

@Composable
private fun GenderChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // 선택된 쪽은 Filled, 아닌 쪽은 Outlined 느낌으로 표시
    if (selected) {
        FilledTonalButton(onClick = onClick) {
            Text(label)
        }
    } else {
        OutlinedButton(onClick = onClick) {
            Text(label)
        }
    }
}
