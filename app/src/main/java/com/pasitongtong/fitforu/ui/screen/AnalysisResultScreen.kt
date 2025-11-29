package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pasitongtong.fitforu.R
import com.pasitongtong.fitforu.ui.Screen

//for 프리뷰
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController


// ───────────────── BodyShapeInfo 정의 ─────────────────

data class BodyShapeInfo(
    val label: String,
    val imageRes: Int,
    val summary: String,
    val tip: String
)

fun getBodyShapeInfo(label: String): BodyShapeInfo {
    return when (label.trim()) {
        "모래시계형" -> BodyShapeInfo(
            label = "모래시계형",
            imageRes = R.drawable.shape_hourglass,
            summary = "상·하체 균형이 뛰어나고 허리선이 선명한 매력적인 체형이에요.",
            tip = "허리 라인이 드러나는 상의와 하이웨이스트 하의를 매치하면 가장 예쁘게 살릴 수 있어요."
        )

        "역삼각형" -> BodyShapeInfo(
            label = "역삼각형",
            imageRes = R.drawable.shape_inverted_triangle,
            summary = "어깨가 비교적 넓고 하체가 슬림한 실루엣의 체형이에요.",
            tip = "하의에 밝은 색/포인트를 주고, 상의는 심플하게 잡아주면 비율이 좋아 보여요."
        )

        "삼각형" -> BodyShapeInfo(
            label = "삼각형",
            imageRes = R.drawable.shape_triangle,
            summary = "골반과 허벅지 쪽 볼륨이 있는 체형으로, 하체가 강조되는 실루엣이에요.",
            tip = "시선을 위로 올리는 상의/아우터를 활용하면 더욱 균형 있는 실루엣을 만들 수 있어요."
        )

        "직사각형" -> BodyShapeInfo(
            label = "직사각형",
            imageRes = R.drawable.shape_rectangle,
            summary = "허리 굴곡이 크지 않고 전체적으로 곧은 실루엣의 체형이에요.",
            tip = "벨트나 셔링, 주름 디테일로 허리 라인을 만들어 주면 좋아요."
        )

        "작은 역삼각형" -> BodyShapeInfo(
            label = "작은 역삼각형",
            imageRes = R.drawable.shape_small_inverted_triangle,
            summary = "상체가 살짝 더 존재감 있는 실루엣이지만 전체적으로 슬림한 체형이에요.",
            tip = "자연스러운 어깨 라인의 상의를 추천해요."
        )

        "사각형" -> BodyShapeInfo(
            label = "사각형",
            imageRes = R.drawable.shape_square,
            summary = "상·하체 비율이 안정적이고 탄탄한 느낌의 체형이에요.",
            tip = "적당히 구조감 있는 재킷/셔츠 핏이 잘 어울려요."
        )

        "큰 사각형" -> BodyShapeInfo(
            label = "큰 사각형",
            imageRes = R.drawable.shape_big_square,
            summary = "전체적으로 안정감 있고 존재감 있는 실루엣의 체형이에요.",
            tip = "단색 + 스트레이트 핏으로 힘 있는 실루엣을 만들면 세련돼 보여요."
        )

        else -> BodyShapeInfo(
            label = label.ifBlank { "모래시계형" },
            imageRes = R.drawable.shape_hourglass,
            summary = "서버에서 전달된 체형 이름에 대한 설명이 아직 등록되지 않았어요.",
            tip = "기본 비율을 살리는 코디를 우선 추천해볼게요."
        )
    }
}

// ───────────────── 결과 화면 ─────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    navController: NavController,
    bodyShapeLabelFromServer: String = "모래시계형",
    onSaveResult: (BodyShapeInfo) -> Unit = {}
) {
    val bodyShapeInfo = remember(bodyShapeLabelFromServer) {
        getBodyShapeInfo(bodyShapeLabelFromServer)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FitForU", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                }
            )
        },
        // 🔹 전체 배경은 테마 기본색 (거의 흰색)
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ─── 상단 텍스트 ───
            Text(
                text = "👏 당신의 체형 분석 결과입니다",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "이제 핏포유가 제안하는 코디를 만나보세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── 중앙 로봇 / 캐릭터 이미지 ───
            Image(
                painter = painterResource(id = bodyShapeInfo.imageRes),
                contentDescription = bodyShapeInfo.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                alignment = Alignment.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── 하단 연파랑 카드 (피그마 스타일) ───
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    // ⬅ 여기 색만 연한 파랑으로
                    containerColor = Color(0xFFE6F4FF)
                ),
                shape = MaterialTheme.shapes.extraLarge,
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "사용자님은 ‘${bodyShapeInfo.label}’ 입니다.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = bodyShapeInfo.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "코디 TIP: ${bodyShapeInfo.tip}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // 다시 진단하기 → 바로 이전(체형분석)으로
                                navController.popBackStack()
                            }
                        ) {
                            Text("다시 진단하기")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // 1) 홈 뷰모델에 결과 저장
                                onSaveResult(bodyShapeInfo)

                                // 2) 홈 화면으로 이동
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Text("저장하기")
                        }

                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_AnalysisResultScreen() {
    val fakeNav = rememberNavController()

    AnalysisResultScreen(
        navController = fakeNav,
        bodyShapeLabelFromServer = "모래시계형",
        onSaveResult = {}
    )
}
