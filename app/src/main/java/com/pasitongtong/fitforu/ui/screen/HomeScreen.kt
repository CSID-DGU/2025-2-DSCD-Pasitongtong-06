package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pasitongtong.fitforu.ui.Screen
import com.pasitongtong.fitforu.viewmodel.HomeViewModel

/**
 * 앱의 메인 랜딩 화면으로, 오늘의 날씨와 추천 코디를 보여줍니다.
 */
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel
) {
    // ViewModel 의 상태를 구독
    val uiState by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "오늘의 추천 코디",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 🔹 체형 분석 CTA 카드 (이미 구현해둔 거 있으면 그대로 두고, 없으면 간단 버전)
        BodyAnalysisCtaCard(
            onClick = { navController.navigate(Screen.SkeletalAnalysis.route) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TodayOutfitCard(
            temperature = uiState.temperature,
            weatherText = uiState.weatherText,
            loading = uiState.loading,
            onRefreshClick = { viewModel.refresh() },
            onCardClick = { navController.navigate(Screen.OutfitDetail.route) } // ✅ 회색 카드 탭 시 이동
        )
    }
}

/**
 * 상단 파란 안내 카드 (이미 있으면 건너뛰어도 됨)
 */
@Composable
private fun BodyAnalysisCtaCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "체형 분석으로 정확도 높이기",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "1분 만에 다시 진단하고\n추천 코디를 더 내 체형에 맞게 조정해요.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * 오늘의 코디 카드 (회색 박스)
 */
@Composable
fun TodayOutfitCard(
    temperature: String,
    weatherText: String,
    loading: Boolean,
    onRefreshClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),   // ✅ 전체 카드 탭 시 상세 화면 이동
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Weather Icon",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (temperature.isNotBlank())
                            "$weatherText, $temperature"
                        else
                            "날씨 정보를 불러오는 중...",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "다른 조합 보기"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "[상의 이미지]",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(8.dp)
                )
                Text(text = "+", fontSize = 20.sp)
                Text(
                    text = "[하의 이미지]",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (loading)
                        "체형 분석 결과와 날씨 정보를 불러오는 중입니다..."
                    else
                        "회색 카드를 탭하면 TOP3 코디를 자세히 볼 수 있어요.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
