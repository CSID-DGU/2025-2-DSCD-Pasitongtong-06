package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pasitongtong.fitforu.ui.Screen
import com.pasitongtong.fitforu.viewmodel.HomeViewModel
import androidx.compose.foundation.background

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel
) {
    val uiState by viewModel.state.collectAsState()
    val bodyShapeInfo = viewModel.bodyShapeInfo   // 🔹 HomeViewModel 에 추가한 체형 결과

    // 🔥 전체 화면 흰색 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── 상단 앱 이름 ───
            Text(
                text = "FitForU",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ─── 상단 날씨 카드 ───
            WeatherSummaryCard(
                dateText = uiState.dateText,
                maxTemp = uiState.maxTemp,
                minTemp = uiState.minTemp,
                diffText = uiState.diffText,
                loading = uiState.loading,
                weatherEmoji = uiState.weatherIcon,
                onRefreshClick = { viewModel.refresh() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── 체형 분석 카드 (결과 없으면 CTA, 있으면 결과 카드) ───
            if (bodyShapeInfo == null) {
                BodyAnalysisCtaCard(
                    onClick = { navController.navigate(Screen.SkeletalAnalysis.route) }
                )
            } else {
                BodyAnalysisResultCard(info = bodyShapeInfo)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─── "오늘 날씨에 딱이에요 👏" 헤더 + 리프레시 ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "오늘 날씨에 딱이에요 👏",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "다른 코디 보기"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── 오늘의 코디 회색 카드 ───
            OutfitResultCard(
                loading = uiState.loading,
                onCardClick = { navController.navigate(Screen.OutfitDetail.route) }
            )
        }
    }
}


@Composable
private fun WeatherSummaryCard(
    dateText: String,
    maxTemp: String,
    minTemp: String,
    diffText: String,
    loading: Boolean,
    weatherEmoji: String,
    onRefreshClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF6D4)
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = weatherEmoji,
                    fontSize = 40.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$maxTemp / $minTemp",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onRefreshClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "날씨 새로고침",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = diffText,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👕",
                    fontSize = 50.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 10.dp)
                )
                Text(
                    text = "👖",
                    fontSize = 42.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-8).dp, y = (-6).dp)
                )
            }
        }
    }
}

@Composable
private fun BodyAnalysisCtaCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7F3FF)
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔍", fontSize = 26.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
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

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "➜",
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 2.dp)
            )
        }
    }
}

/**
 * 🔹 체형 분석 결과가 저장된 경우에 보여줄 카드
 *    (왼쪽에 캐릭터 이미지 + 오른쪽에 텍스트)
 */
@Composable
private fun BodyAnalysisResultCard(info: BodyShapeInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE7F3FF)
        ),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 왼쪽 캐릭터/체형 이미지
            Icon(
                painter = painterResource(id = info.imageRes),
                contentDescription = info.label,
                modifier = Modifier
                    .size(56.dp),
                tint = Color.Unspecified        // 원본 색 유지
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "사용자님은 ‘${info.label}’ 입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = info.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun OutfitResultCard(
    loading: Boolean,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable(onClick = onCardClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF1F2F6)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (loading)
                    "체형 분석 결과와 날씨 정보를 불러오는 중입니다..."
                else
                    "체형 분석 결과를 반영한 코디 조합이에요.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp
            )
        }
    }
}

