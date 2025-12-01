package com.pasitongtong.fitforu.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

                // 3) 백엔드/DB 용 gender 값
                val genderValue = if (selectedGender == "M") "male" else "female"

                // ✅ 백엔드에 프로필 등록 요청
                viewModel.postBackendProfile(genderValue) { success ->
                    if (success) {
                        // 성공 → 메인으로
                        navController.navigate("main") {
                            popUpTo("profileSetup") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "프로필 저장에 실패했어요. 다시 시도해 주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
