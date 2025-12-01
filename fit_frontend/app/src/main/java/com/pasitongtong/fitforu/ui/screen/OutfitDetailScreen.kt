package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pasitongtong.fitforu.R
import com.pasitongtong.fitforu.model.SavedOutfit

@Composable
fun OutfitDetailScreen(
    navController: NavController,
    onSaveOutfit: (SavedOutfit) -> Unit = {}
) {
    // 샘플 코디 리스트
    val outfitList = listOf(
        SavedOutfit(
            imageRes = R.drawable.top1,
            title = "TOP 1",
            bodyText = "와이드 팬츠와 아우터 조합이 체형의 균형을 잘 잡아주고, 상·하의 명도 대비로 시선을 분산시켜 전체적으로 슬림해 보이게 합니다.",
            tip = "Tip: 아우터를 오픈해서 레이어드를 보여주면 내추럴 체형의 장점이 더 극대화돼요."
        ),
        SavedOutfit(
            imageRes = R.drawable.top2,
            title = "TOP 2",
            bodyText = "크롭 기장의 상의와 하이웨이스트 팬츠 조합이 다리를 길어 보이게 해줍니다.",
            tip = "Tip: 허리선을 강조하는 벨트를 더해주면 비율이 더욱 좋아 보여요."
        ),
        SavedOutfit(
            imageRes = R.drawable.top3,
            title = "TOP 3",
            bodyText = "톤온톤 컬러 매치로 전체적으로 차분하면서도 세련된 인상을 줍니다.",
            tip = "Tip: 포인트 악세서리를 하나 더해주면 스타일이 훨씬 살아나요."
        )
    )

    var selectedIndex by remember { mutableStateOf(0) }
    var isSelected by remember { mutableStateOf(false) } // 체크 아이콘 상태
    val currentOutfit = outfitList[selectedIndex]

    // 🔥 전체 화면 흰색 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // ⬅️ 뒤로가기 버튼 (왼쪽 정렬)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.CenterStart)
                            .clickable { navController.popBackStack() }
                    )

                    // 🔥 FitForU 정중앙 제목
                    Text(
                        text = "FitForU",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // 👉 오른쪽 빈 공간 균형 맞춤(버튼 없어도 중앙 유지됨)
                    Spacer(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.CenterEnd)
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 상단 질문 텍스트
                Text(
                    text = "핏포유 코디는 어때요? ✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()        // 좌측 정렬을 적용하기 위해 폭 확장
                        .align(Alignment.Start) // 왼쪽 정렬
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ----- 메인 코디 이미지 + 오른쪽 버튼 컬럼 -----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 👕 이미지: 잘리지 않게 ContentScale.Fit 사용
                    Image(
                        painter = painterResource(id = currentOutfit.imageRes),
                        contentDescription = "코디 이미지",
                        modifier = Modifier
                            .fillMaxWidth(0.85f)      // 살짝 여백 생기게
                            .height(320.dp),          // 너무 크지 않게 고정
                        contentScale = ContentScale.Fit
                    )

                    // 오른쪽에 세로 정렬된 화살표 / 체크 버튼
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 20.dp)
                            .offset(y = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // ➡ 다음 코디 버튼
                        Surface(
                            modifier = Modifier
                                .size(54.dp)
                                .clickable {
                                    // 다음 코디로 이동
                                    selectedIndex = (selectedIndex + 1) % outfitList.size
                                    isSelected = false    // 새 코디에는 체크 초기화
                                },
                            shape = CircleShape,
                            color = Color(0xFFE6ECFF)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "다음 코디",
                                    tint = Color(0xFF2F3A5A)
                                )
                            }
                        }

                        // ✓ 선택(저장) 버튼
                        Surface(
                            modifier = Modifier
                                .size(54.dp)
                                .clickable {
                                    isSelected = !isSelected
                                    if (isSelected) {
                                        onSaveOutfit(currentOutfit)
                                    }
                                },
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF4C6FFF) else Color(0xFFE6ECFF)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "코디 선택",
                                    tint = if (isSelected) Color.White else Color(0xFF4C6FFF)
                                )
                            }
                        }
                    }

                    // 하단 페이지 인디케이터 (● ○ ○)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(outfitList.size) { index ->
                            val isCurrent = index == selectedIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (isCurrent) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent)
                                            Color(0xFF4C6FFF)
                                        else
                                            Color(0xFFCDD3E6)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 💡 코디 선택 방법
                Text(
                    text = "💡 코디 선택 방법",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "추천 코디가 마음에 들면 하단의 ‘체크’, 다른 코디는 ‘다음’ 버튼을 눌러주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(18.dp))

                // ----- 체형 문장(가운데 정렬만) -----
                Text(
                    text = "사용자님은 '모래시계형' 입니다.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 설명/Tip : 기본(좌측) 정렬
                Text(
                    text = currentOutfit.bodyText,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentOutfit.tip,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
