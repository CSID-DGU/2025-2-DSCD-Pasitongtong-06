package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pasitongtong.fitforu.model.RecommendResult
import com.pasitongtong.fitforu.model.SavedOutfit
import com.pasitongtong.fitforu.model.toSavedOutfit
import com.pasitongtong.fitforu.viewmodel.MainViewModel
import java.time.LocalDate


private fun shapeToKorean(shape: String?): String {
    return when (shape) {
        "Rectangle" -> "직사각형"
        "invertedtriangle" -> "역삼각형"
        "Triangle" -> "삼각형"
        "Hourglass" -> "모래시계형"
        "square" -> "사각형"
        "bigsquare" -> "큰사각형"
        "smallinvertedtriangle" -> "작은역삼각형"
        else -> shape ?: "-"
    }
}


// ----------------------------------------------------------------------
//  공용 콜라주 레이아웃: URL 4개로 상/하/아우터/원피스 배치
// ----------------------------------------------------------------------
@Composable
fun OutfitCollageCore(
    topUrl: String?,
    bottomUrl: String?,
    outerUrl: String?,
    onepieceUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color(0xFFF8F8F8))
            .padding(12.dp)
    ) {
        if (onepieceUrl != null) {
            // 원피스 하나만 있는 경우
            AsyncImage(
                model = onepieceUrl,
                contentDescription = "원피스 코디",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            // 상의 + 하의 + 아우터 콜라주
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (topUrl != null) {
                    AsyncImage(
                        model = topUrl,
                        contentDescription = "상의",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Fit
                    )
                }
                if (bottomUrl != null) {
                    Spacer(Modifier.width(8.dp))
                    AsyncImage(
                        model = bottomUrl,
                        contentDescription = "하의",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Fit
                    )
                }
                if (outerUrl != null) {
                    Spacer(Modifier.width(8.dp))
                    AsyncImage(
                        model = outerUrl,
                        contentDescription = "아우터",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
//  콜라주 컴포넌트: RecommendResult → 상/하/아우터/원피스 조합으로 보여주기
// ----------------------------------------------------------------------
@Composable
private fun OutfitCollage(
    recommend: RecommendResult,
    modifier: Modifier = Modifier
) {
    OutfitCollageCore(
        topUrl = recommend.top?.imageUrl,
        bottomUrl = recommend.bottom?.imageUrl,
        outerUrl = recommend.outer?.imageUrl,
        onepieceUrl = recommend.onepiece?.imageUrl,
        modifier = modifier
    )
}


// ----------------------------------------------------------------------
//  메인 화면: 코디 추천 상세 + 저장
// ----------------------------------------------------------------------
@Composable
fun OutfitDetailScreen(
    navController: NavController,
    viewModel: MainViewModel,
    // ✅ 캘린더에서 넘어오면 그 날짜, 아니면 null
    selectedDate: LocalDate? = null,
    onSaveOutfit: (SavedOutfit) -> Unit = {}
) {
    val uiState by viewModel.outfitUiState.collectAsState()

    // 화면 입장 시 한 번만 API 요청
    LaunchedEffect(Unit) {
        viewModel.loadOutfitRecommendations()
    }

    // 체크(저장) 버튼 선택 상태
    var isSelected by remember { mutableStateOf(false) }

    val current = uiState.current               // RecommendResult?
    val totalCount = uiState.data?.recommendations?.size ?: 0

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
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.CenterStart)
                            .clickable { navController.popBackStack() }
                    )

                    Text(
                        text = "FitForU",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Spacer(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.CenterEnd)
                    )
                }
            }
        ) { innerPadding ->

            when {
                uiState.isLoading -> {
                    Box(
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "코디 추천을 불러오지 못했어요.\n${uiState.error}",
                            textAlign = TextAlign.Center
                        )
                    }
                }

                current == null -> {
                    Box(
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("추천할 코디가 없습니다.")
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 상단 타이틀
                        Text(
                            text = "핏포유 코디는 어때요? ✨",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Start)
                        )

                        Spacer(Modifier.height(12.dp))

                        // 메인 콜라주 영역 + 오른쪽 버튼들
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 🔥 콜라주 컴포넌트 사용
                            OutfitCollage(
                                recommend = current,
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(320.dp)
                            )

                            // 👉 오른쪽 화살표 / 체크 버튼
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 20.dp)
                                    .offset(y = 40.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // ➡ 다음 코디
                                Surface(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clickable {
                                            viewModel.showNextRecommendation()
                                            isSelected = false       // 다음으로 넘기면 선택 해제
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

                                // ✓ 저장 버튼
                                Surface(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clickable {
                                            isSelected = !isSelected
                                            if (isSelected && current != null) {
                                                // ✅ 날짜: 캘린더에서 넘어왔으면 그 날짜, 아니면 오늘
                                                val dateForSave = selectedDate ?: LocalDate.now()
                                                val saved = current.toSavedOutfit(dateForSave)
                                                onSaveOutfit(saved)
                                            }
                                        },
                                    shape = CircleShape,
                                    color = if (isSelected) Color(0xFF4C6FFF) else Color(0xFFE6ECFF)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "코디 저장",
                                            tint = if (isSelected) Color.White else Color(0xFF4C6FFF)
                                        )
                                    }
                                }
                            }

                            // 하단 페이지 인디케이터
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(totalCount) { idx ->
                                    val isCurrent = idx == uiState.currentIndex
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .size(if (isCurrent) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCurrent) Color(0xFF4C6FFF)
                                                else Color(0xFFCDD3E6)
                                            )
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // 설명 텍스트들
                        Text(
                            text = "💡 코디 선택 방법",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "추천 코디가 마음에 들면 ‘체크’, 다른 코디는 ‘다음’을 누르세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(18.dp))



                        Text(
                            text = "사용자님은 '${shapeToKorean(uiState.data?.userShape)}' 입니다.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        // 💬 코멘트 문자열 후처리
                        val formattedComment = remember(current.comment) {
                            current.comment

                                // 💥 "사용자님은 ~~ 입니다." 형태 제거
                                .replace(Regex("사용자님은[^.]*입니다[.!]?"), "")
                                .trim()

                                // 1) 체형 이름 한글로 치환
                                .replace("Rectangle형", "직사각형")
                                .replace("invertedtriangle형", "역삼각형")
                                .replace("Triangle형", "삼각형")
                                .replace("Hourglass형", "모래시계형")
                                .replace("smallinvertedtriangle형", "작은역삼각형")
                                .replace("square형", "사각형")
                                .replace("bigsquare형", "큰사각형")
                                // 2) Tip 앞에 줄바꿈 넣기
                                .replace(" Tip:", "\n\nTip:")   // 앞에 공백 붙어서 올 수도 있어서
                                .replace("TIP:", "\n\nTip:")    // 혹시 대문자로 올 경우
                                .replace("Tip.", "\n\nTip.")    // 마침표 버전 대비
                        }
                        Text(
                            text = formattedComment,          // ✅ 가공된 문자열 사용
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(8.dp))

                    }
                }
            }
        }
    }
}
