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

data class OutfitThumbnail(
    val date: LocalDate,
    val imageResId: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    // 나중에 ViewModel 에서 받아올 데이터
    outfits: List<OutfitThumbnail> = sampleOutfits()
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // 날짜별로 그룹핑
    val outfitsByDate: Map<LocalDate, List<OutfitThumbnail>> =
        remember(outfits) { outfits.groupBy { it.date } }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "FitForU", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
        ) {

            // ─── 년/월 헤더 ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentMonth.year}년 ${currentMonth.monthValue}월 ▾",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ─── 요일 헤더 ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf("일", "월", "화", "수", "목", "금", "토")
                days.forEachIndexed { index, label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        color = when (index) {
                            0 -> Color(0xFFE53935) // 일(빨강)
                            6 -> Color(0xFF1E88E5) // 토(파랑)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── 날짜 그리드 ───
            CalendarMonthGrid(
                month = currentMonth,
                selectedDate = selectedDate,
                outfitsByDate = outfitsByDate,
                onDayClick = { clicked ->
                    selectedDate = clicked
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 선택된 날짜 텍스트 (필요 없으면 제거해도 됨)
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

    // java.time.DayOfWeek: MON(1)~SUN(7) → 우리는 일요일 0부터 시작하고 싶음
    val firstDayIndex = when (firstDayOfMonth.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        else -> firstDayOfMonth.dayOfWeek.value
    }

    val totalCells = firstDayIndex + daysInMonth
    val weeks = ceil(totalCells / 7.0).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        repeat(weeks) { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (dayOfWeek in 0 until 7) {
                    val cellIndex = week * 7 + dayOfWeek
                    val dayNumber = cellIndex - firstDayIndex + 1

                    if (dayNumber < 1 || dayNumber > daysInMonth) {
                        // 빈 칸
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        )
                    } else {
                        val date = month.atDay(dayNumber)
                        val outfits = outfitsByDate[date].orEmpty()

                        CalendarDayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            outfits = outfits,
                            modifier = Modifier.weight(1f),
                            onClick = { onDayClick(date) }
                        )
                    }
                }
            }
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



