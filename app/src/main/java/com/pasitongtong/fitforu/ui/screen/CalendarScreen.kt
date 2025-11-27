package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)   // ← DatePicker는 실험적 API라 이게 없으면 빨간 줄 남
@Composable
fun CalendarScreen(navController: NavController) {

    // 날짜 선택 상태
    val datePickerState: DatePickerState = rememberDatePickerState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Calendar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- 실제 캘린더 ---
        DatePicker(
            state = datePickerState,
            showModeToggle = false,   // Year/Month 전환 버튼 숨김
            title = null,             // 상단 TITLE 제거
            headline = null           // 상단 헤드라인 제거
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 선택된 날짜 해석 ---
        val selectedDate: LocalDate? =
            datePickerState.selectedDateMillis?.let { millis ->
                Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }

        // --- 선택된 날짜에 대한 간단한 정보 영역 ---
        if (selectedDate != null) {
            Text(
                text = "Selected date: $selectedDate",
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            Text(
                text = "No date selected.",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}


