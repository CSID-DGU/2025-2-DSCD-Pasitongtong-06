package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pasitongtong.fitforu.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import androidx.compose.ui.layout.ContentScale
import com.google.accompanist.systemuicontroller.rememberSystemUiController

import com.pasitongtong.fitforu.viewmodel.SavedOutfitViewModel
import com.pasitongtong.fitforu.model.SavedOutfit
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll



data class OutfitThumbnail(
    val date: LocalDate,
    val imageResId: Int
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    savedOutfitViewModel: SavedOutfitViewModel
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // ✅ 실제 저장된 코디 목록
    val savedOutfits by savedOutfitViewModel.savedOutfits.collectAsState()

    // 날짜별로 저장된 코디 그룹핑
    val outfitsByDate = remember(savedOutfits) {
        savedOutfits
            .filter { it.date != null }              // 날짜 있는 것만
            .groupBy { it.date!! }                  // Map<LocalDate, List<SavedOutfit>>
    }

    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.White,
            darkIcons = true
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FitForU", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color.Black,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(4.dp))

            // ── 상단 월 표시 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${currentMonth.year}년 ${currentMonth.monthValue}월 ▾",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 요일 헤더 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { index, label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = when (index) {
                            0 -> Color(0xFFE53935)
                            6 -> Color(0xFF1E88E5)
                            else -> Color.Black
                        }
                    )
                }
            }

            Divider(
                thickness = 0.6.dp,
                color = Color.Black,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            // ── 실제 달력 그리드 (여기서 outfitsByDate 를 넘김) ──
            CalendarMonthGrid(
                month = currentMonth,
                selectedDate = selectedDate,
                outfitsByDate = outfitsByDate,   // Map<LocalDate, List<SavedOutfit>>
                onDayClick = { clicked ->
                    selectedDate = clicked
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── 선택된 날짜 + 해당 날짜 코디 목록 표시 ──
            val todaysOutfits: List<SavedOutfit> =
                selectedDate?.let { date -> outfitsByDate[date].orEmpty() } ?: emptyList()


            Spacer(modifier = Modifier.height(8.dp))

            // 🔽 캘린더 아래 나오는 코디 리스트 영역 (스크롤 가능하게)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)   // 남은 화면을 전부 이 영역에 배분
            ) {
                if (selectedDate == null) {
                    // 날짜 안 골랐을 때
                    Text(
                        text = "날짜를 선택해 주세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                } else {
                    val todaysOutfits: List<SavedOutfit> =
                        outfitsByDate[selectedDate].orEmpty()

                    if (todaysOutfits.isEmpty()) {
                        Text(
                            text = "이 날 저장한 코디가 없어요.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            todaysOutfits.forEach { outfit ->
                                CalendarOutfitCard(outfit = outfit)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun CalendarOutfitCard(outfit: SavedOutfit) {

    // 1) 체형 문구 제거 + 한글 치환 + Tip 줄바꿈
    val formattedBody = remember(outfit.bodyText) {
        outfit.bodyText
            // 🔥🔥 ① 체형 설명 문장 삭제
            .replace(Regex("사용자님은[^\\.]*\\."), "")  // 첫 문장 제거

            // ② 체형 이름 한글 변환
            .replace("Rectangle형", "직사각형")
            .replace("invertedtriangle형", "역삼각형")
            .replace("Triangle형", "삼각형")
            .replace("Hourglass형", "모래시계형")
            .replace("smallinvertedtriangle형", "작은 역삼각형")
            .replace("square형", "사각형")
            .replace("bigsquare형", "큰 사각형")

            // ③ Tip 앞에 줄바꿈 넣기
            .replace(" Tip:", "\n\nTip:")
            .replace("TIP:", "\n\nTip:")
            .replace("Tip.", "\n\nTip.")

            // ④ 불필요한 공백 정리
            .trimStart()
    }

    val formattedTip = remember(outfit.tip) {
        outfit.tip
            .replace("Rectangle형", "직사각형")
            .replace("invertedtriangle형", "역삼각형")
            .replace("Triangle형", "삼각형")
            .replace("Hourglass형", "모래시계형")
            .replace("smallinvertedtriangle형", "작은 역삼각형")
            .replace("square형", "사각형")
            .replace("bigsquare형", "큰 사각형")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F5FA)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 콜라주
            OutfitCollageCore(
                topUrl = outfit.topImageUrl,
                bottomUrl = outfit.bottomImageUrl,
                outerUrl = outfit.outerImageUrl,
                onepieceUrl = outfit.onepieceImageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ⭐ 체형 문구 제거된 본문 출력
            if (formattedBody.isNotBlank()) {
                Text(
                    text = formattedBody,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (formattedTip.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tip. $formattedTip",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}




/**
 * 한 달짜리 캘린더 그리드 (일~토)
 * 각 날짜 아래에 해당 날짜의 코디 썸네일을 보여줌
 */
@Composable
fun CalendarMonthGrid(
    month: YearMonth,
    selectedDate: LocalDate?,
    outfitsByDate: Map<LocalDate, List<SavedOutfit>>,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val firstDayIndex = if (firstDayOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0 else firstDayOfMonth.dayOfWeek.value
    val totalCells = firstDayIndex + daysInMonth
    val weeks = ceil(totalCells / 7.0).toInt()

    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(weeks) { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (dow in 0 until 7) {
                    val idx = week * 7 + dow
                    val day = idx - firstDayIndex + 1
                    if (day in 1..daysInMonth) {
                        val date = month.atDay(day)
                        CalendarDayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            outfits = outfitsByDate[date].orEmpty(),
                            modifier = Modifier.weight(1f),
                            onClick = { onDayClick(date) }
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 🔥 각 주(week) 아래 구분선
            Divider(thickness = 0.6.dp, color = Color.Black)
        }
    }
}


/**
 * 한 날짜 셀: 숫자 + 코디 썸네일들
 */
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isSelected: Boolean,
    outfits: List<SavedOutfit>,       // ✅ 이렇게
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(70.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 날짜 숫자 (선택 시 동그랗게 표시)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }

        // 🔽 이 부분이 예전에 imageResId 쓰던 자리일 거야
        if (outfits.isNotEmpty()) {
            val first = outfits.first()
            val thumbUrl =
                first.topImageUrl
                    ?: first.onepieceImageUrl
                    ?: first.outerImageUrl
                    ?: first.bottomImageUrl

            if (thumbUrl != null) {
                AsyncImage(
                    model = thumbUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * 미리보기용 더미 데이터
 */
private fun sampleOutfits(): List<OutfitThumbnail> {
    val base = LocalDate.now().withDayOfMonth(24)
    return listOf(
        OutfitThumbnail(base, R.drawable.closet1),
        OutfitThumbnail(base.plusDays(0), R.drawable.closet2),
        OutfitThumbnail(base.plusDays(1), R.drawable.closet3), // 25일
        OutfitThumbnail(base.plusDays(2), R.drawable.closet1)  // 26일
    )
}



