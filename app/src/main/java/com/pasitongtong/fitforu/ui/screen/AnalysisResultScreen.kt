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
import androidx.compose.ui.graphics.Color          // ✅ Color import 추가
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pasitongtong.fitforu.R

import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
// 🔹 서버에서 오는 체형 타입을 표현하는 타입
data class BodyShapeInfo(
    val label: String,     // "모래시계형"
    val imageRes: Int,     // R.drawable.모래시계형 (또는 네가 쓴 리소스 id)
    val summary: String,   // 체형 설명
    val tip: String        // 코디 TIP
)

/**
 * 서버에서 받은 label(예: "모래시계형")을 기반으로
 * 이미지 + 설명 + TIP 을 돌려주는 함수
 */
fun getBodyShapeInfo(label: String): BodyShapeInfo {
    return when (label.trim()) {
        "모래시계형" -> BodyShapeInfo(
            label = "모래시계형",
            imageRes = R.drawable.shape_hourglass,   // ← 실제 리소스 id 로 맞춰둔 상태라고 가정
            summary = "상·하체 균형이 뛰어나고 허리선이 선명한 매력적인 체형이에요.",
            tip = "허리 라인이 드러나는 상의와 하이웨이스트 하의를 매치하면 가장 예쁘게 살릴 수 있어요."
        )

        "역삼각형" -> BodyShapeInfo(
            label = "역삼각형",
            imageRes = R.drawable.shape_inverted_triangle,
            summary = "어깨가 비교적 넓고 하체가 슬림한 실루엣의 체형이에요.",
            tip = "하의에 밝은 색/포인트를 주고, 상의는 단정하고 심플하게 잡아주면 비율이 좋아 보여요."
        )

        "삼각형" -> BodyShapeInfo(
            label = "삼각형",
            imageRes = R.drawable.shape_triangle,
            summary = "골반과 허벅지 쪽 볼륨이 있는 체형으로, 하체가 강조되는 실루엣이에요.",
            tip = "허리 위로 올라오는 상의나 아우터로 시선을 위로 올려주면 균형 잡힌 느낌을 줄 수 있어요."
        )

        "직사각형" -> BodyShapeInfo(
            label = "직사각형",
            imageRes = R.drawable.shape_rectangle,
            summary = "허리 굴곡이 크지 않고 전체적으로 곧은 실루엣의 체형이에요.",
            tip = "벨트, 주름, 셔링 등으로 허리 라인을 살려주거나, 일자 핏 아이템으로 미니멀한 느낌을 강조해보세요."
        )

        "작은 역삼각형" -> BodyShapeInfo(
            label = "작은 역삼각형",
            imageRes = R.drawable.shape_small_inverted_triangle,
            summary = "상체가 조금 더 존재감 있는 실루엣이지만 전반적으로 슬림한 체형이에요.",
            tip = "너무 과한 퍼프/숄더보다는 자연스러운 어깨 라인의 상의를 추천해요."
        )

        "사각형" -> BodyShapeInfo(
            label = "사각형",
            imageRes = R.drawable.shape_square,
            summary = "상·하체 비율이 안정적이고 체격이 탄탄한 느낌의 체형이에요.",
            tip = "어깨와 허리를 적당히 드러내는 재킷/셔츠 핏이 잘 어울려요."
        )

        "큰 사각형" -> BodyShapeInfo(
            label = "큰 사각형",
            imageRes = R.drawable.shape_big_square,
            summary = "전체적으로 안정감 있고 존재감 있는 실루엣의 체형이에요.",
            tip = "단색 + 스트레이트 핏으로 정돈된 실루엣을 만들면 세련된 느낌을 줄 수 있어요."
        )

        // 기본값
        else -> BodyShapeInfo(
            label = label.ifBlank { "모래시계형" },
            imageRes = R.drawable.shape_hourglass,
            summary = "서버에서 전달된 체형 이름에 대한 설명이 아직 등록되지 않았어요.",
            tip = "허리선과 비율이 잘 살아나는 기본 코디를 먼저 추천해볼게요."
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)   // ✅ Experimental API opt-in
@Composable
fun AnalysisResultScreen(
    navController: NavController,
    // TODO: 나중에 ViewModel이나 NavArg로 서버에서 받은 label을 내려주면 됨
    bodyShapeLabelFromServer: String = "모래시계형"
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 설명
            Text(
                text = "🍊 당신의 체형 분석 결과입니다",
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

            // 중앙 체형 이미지
            Image(
                painter = painterResource(id = bodyShapeInfo.imageRes),
                contentDescription = bodyShapeInfo.label,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                alignment = Alignment.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 하단 결과 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEFF6FF) // 살짝 파란 톤의 연한 배경
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
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
                        color = MaterialTheme.colorScheme.primary,
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
                                // 다시 진단하기 → 이전(골격 분석) 화면으로
                                navController.popBackStack()
                            }
                        ) {
                            Text("다시 진단하기")
                        }

                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // TODO: 결과를 "보관함" 등에 저장하는 로직 연결
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
fun PreviewAnalysisResultScreen() {
    val navController = rememberNavController()
    AnalysisResultScreen(
        navController = navController,
        bodyShapeLabelFromServer = "모래시계형" // 다른 체형으로 바꿔가며 테스트 가능
    )
}
