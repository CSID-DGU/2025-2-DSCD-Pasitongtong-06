package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.pasitongtong.fitforu.R
import com.pasitongtong.fitforu.ui.Screen
import com.pasitongtong.fitforu.viewmodel.MainViewModel

// ───────────────── BodyShapeInfo 정의 ─────────────────

data class BodyShapeInfo(
    val label: String,
    val imageRes: Int,
    val summary: String,
    val tip: String
)

/**
 *  백엔드 라벨(영어) + 한글 라벨 둘 다 처리
 */
fun getBodyShapeInfo(rawLabel: String?): BodyShapeInfo {

    val key = (rawLabel ?: "")
        .trim()
        .lowercase()
        .replace(" ", "")
        .replace("_", "")

    return when (key) {
        // ───── 여성 체형 ─────
        "hourglass", "모래시계형" -> BodyShapeInfo(
            label = "모래시계형",
            imageRes = R.drawable.shape_hourglass,
            summary = "상·하체 균형이 뛰어나고 허리선이 선명한 매력적인 체형이에요.",
            tip = "허리 라인이 드러나는 상의와 하이웨이스트 하의를 매치하면 가장 예쁘게 살릴 수 있어요."
        )

        "invertedtriangle", "역삼각형" -> BodyShapeInfo(
            label = "역삼각형",
            imageRes = R.drawable.shape_inverted_triangle,
            summary = "어깨가 비교적 넓고 하체가 슬림한 실루엣의 체형이에요.",
            tip = "하의에 밝은 색/포인트를 주고, 상의는 심플하게 잡아주면 비율이 좋아 보여요."
        )

        "triangle", "삼각형" -> BodyShapeInfo(
            label = "삼각형",
            imageRes = R.drawable.shape_triangle,
            summary = "골반과 허벅지 쪽 볼륨이 있는 체형으로, 하체가 강조되는 실루엣이에요.",
            tip = "시선을 위로 올리는 상의/아우터를 활용하면 더욱 균형 있는 실루엣을 만들 수 있어요."
        )

        "rectangle", "직사각형" -> BodyShapeInfo(
            label = "직사각형",
            imageRes = R.drawable.shape_rectangle,
            summary = "허리 굴곡이 크지 않고 전체적으로 곧은 실루엣의 체형이에요.",
            tip = "벨트나 셔링, 주름 디테일로 허리 라인을 만들어 주면 좋아요."
        )

        // ───── 남성 체형 ─────
        "smallinvertedtriangle", "작은역삼각형" -> BodyShapeInfo(
            label = "작은 역삼각형",
            imageRes = R.drawable.shape_small_inverted_triangle,
            summary = "상체가 살짝 더 존재감 있는 실루엣이지만 전체적으로 슬림한 체형이에요.",
            tip = "자연스러운 어깨 라인의 상의를 추천해요."
        )

        "square", "사각형" -> BodyShapeInfo(
            label = "사각형",
            imageRes = R.drawable.shape_square,
            summary = "상·하체 비율이 안정적이고 탄탄한 느낌의 체형이에요.",
            tip = "적당히 구조감 있는 재킷/셔츠 핏이 잘 어울려요."
        )

        "bigsquare", "큰사각형" -> BodyShapeInfo(
            label = "큰 사각형",
            imageRes = R.drawable.shape_big_square,
            summary = "전체적으로 안정감 있고 존재감 있는 실루엣의 체형이에요.",
            tip = "단색 + 스트레이트 핏으로 힘 있는 실루엣을 만들면 세련돼 보여요."
        )

        // ───── 알 수 없는 값 ─────
        else -> BodyShapeInfo(
            label = rawLabel?.ifBlank { "모래시계형" } ?: "모래시계형",
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
    mainViewModel: MainViewModel
) {
    // 🔥 1) 뷰모델에서 최신 체형 라벨 / 이미지 Uri 읽어오기
    val shapeTypeState = mainViewModel.bodyShapeType.collectAsState()
    val shapeType = shapeTypeState.value          // 예: "Rectangle"

    // (원하면 원본 라벨 디버깅용으로 화면에 잠깐 뿌려볼 수도 있어)
    val bodyShapeInfo = remember(shapeType) {
        getBodyShapeInfo(shapeType)
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
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "👏 당신의 체형 분석 결과입니다",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "이제 핏포유가 제안하는 코디를 만나보세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = bodyShapeInfo.imageRes),
                contentDescription = bodyShapeInfo.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                alignment = Alignment.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE6F4FF)
                ),
                shape = MaterialTheme.shapes.extraLarge
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
                                navController.navigate(Screen.SkeletalAnalysis.route) {
                                    popUpTo(Screen.SkeletalAnalysis.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Text("다시 진단하기")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // 1️⃣ 메인 뷰모델에 라벨 저장
                                mainViewModel.saveBodyShapeLabel(bodyShapeInfo.label)

                                // 2️⃣ 홈으로 이동
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
    val fakeViewModel = MainViewModel()   // 프리뷰용

    AnalysisResultScreen(
        navController = fakeNav,
        mainViewModel = fakeViewModel   // ⬅️ 여기! viewModel 말고 mainViewModel
    )
}


