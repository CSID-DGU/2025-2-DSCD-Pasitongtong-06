package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.PaddingValues

/**
 * 사용자가 저장한 코디 조합들을 모아보는 '코디 북' 화면입니다.
 */
@Composable
fun StylebookScreen(navController: NavController) {
    Scaffold(
        containerColor = Color.White,   // ⬅️ Scaffold 배경 흰색
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO */ }) {
                Icon(Icons.Default.Add, contentDescription = "새 코디 추가")
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)   // ⬅️ Column도 흰색
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "나만의 코디 북",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),   // ⬅️ 그리드도 흰색
                contentPadding = PaddingValues(bottom = 80.dp) // FAB와 겹침 방지
            ) {
                items(10) { index ->
                    SavedOutfitCard(index)
                }
            }
        }
    }
}


@Composable
fun SavedOutfitCard(index: Int) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .height(200.dp), // 카드 높이 지정
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TODO: 저장된 코디 이미지로 교체
            Text(text = "코디 ${index + 1}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "상의 이미지")
            Text(text = "+")
            Text(text = "하의 이미지")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StylebookScreenPreview() {
    StylebookScreen(navController = rememberNavController())
}
