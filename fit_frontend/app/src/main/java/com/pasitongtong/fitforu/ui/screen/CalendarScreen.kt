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

data class OutfitThumbnail(
    val date: LocalDate,
    val imageResId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    outfits: List<OutfitThumbnail> = sampleOutfits(),
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val outfitsByDate = remember(outfits) { outfits.groupBy { it.date } }

    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.White,          // ← 원하는 색
            darkIcons = true              // 아이콘을 검정색으로 만들지 여부
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FitForU", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,          // 배경 흰색
                    navigationIconContentColor = Color.Black,
                    titleContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White   // ← ① 배경 흰색
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)      // ← ② 전체 Column 흰색
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(4.dp))

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

            // ─── 요일 헤더 ───
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

            // ── 요일 아래 검은 라인
            Divider(
                thickness = 0.6.dp,
                color = Color.Black,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            CalendarMonthGrid(
                month = currentMonth,
                selectedDate = selectedDate,
                outfitsByDate = outfitsByDate,
                onDayClick = { selectedDate = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = selectedDate?.let { "선택한 날: $it" } ?: "날짜를 선택해 주세요.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


/**
 * 한 달짜리 캘린더 그리드 (일~토)
 * 각 날짜 아래에 해당 날짜의 코디 썸네일을 보여줌
 */
@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    selectedDate: LocalDate?,
    outfitsByDate: Map<LocalDate, List<OutfitThumbnail>>,
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
    outfits: List<OutfitThumbnail>,
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

        // 썸네일 (최대 3개 정도만 표시)
        Row(
            modifier = Modifier
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            outfits.take(3).forEach { outfit ->
                Image(
                    painter = painterResource(id = outfit.imageResId),
                    contentDescription = "코디 썸네일",
                    modifier = Modifier
                        .size(20.dp)
                        .padding(horizontal = 1.dp),
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



