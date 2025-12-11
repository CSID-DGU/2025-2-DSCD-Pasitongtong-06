package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pasitongtong.fitforu.model.SavedOutfit
import com.pasitongtong.fitforu.viewmodel.SavedOutfitViewModel
import androidx.compose.foundation.layout.PaddingValues
import java.time.format.DateTimeFormatter

/**
 * SavedOutfit → 콜라주(상의/하의/아우터/원피스)로 보여주는 컴포넌트
 */
@Composable
fun SavedOutfitCollage(
    outfit: SavedOutfit,
    modifier: Modifier = Modifier
) {
    OutfitCollageCore(
        topUrl = outfit.topImageUrl,
        bottomUrl = outfit.bottomImageUrl,
        outerUrl = outfit.outerImageUrl,
        onepieceUrl = outfit.onepieceImageUrl,
        modifier = modifier
    )
}

/**
 * 나만의 코디 북 화면
 * - 날짜와 상관없이 사용자가 저장한 모든 코디(SavedOutfit)를 보여줌
 */
@Composable
fun StylebookScreen(
    navController: NavController,
    savedOutfitViewModel: SavedOutfitViewModel
) {
    // ✅ ViewModel 에 저장된 코디 목록 구독
    val outfits by savedOutfitViewModel.savedOutfits.collectAsState()

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // TODO: 필요하면 여기서 코디 추천 화면으로 이동 등
                    // navController.navigate(Screen.OutfitDetail.route)
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "새 코디 추가")
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
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

            Spacer(modifier = Modifier.height(8.dp))

            if (outfits.isEmpty()) {
                // ✅ 아직 저장된 코디가 없을 때
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 저장한 코디가 없어요.\n코디 추천 화면에서 체크 버튼을 눌러 저장해 보세요!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = rememberLazyGridState(),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(outfits) { outfit ->
                        SavedOutfitCard(outfit = outfit)
                    }
                }
            }
        }
    }
}

/**
 * 실제 저장된 한 개의 코디 카드
 */
@Composable
fun SavedOutfitCard(outfit: SavedOutfit) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    Card(
        modifier = Modifier
            .padding(8.dp)
            .height(220.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF4F5FA)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔥 상의 / 하의 / 아우터 / 원피스 콜라주
            SavedOutfitCollage(
                outfit = outfit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🔸 코디 제목
            Text(
                text = outfit.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            // 🔸 저장된 날짜 (있을 때만)
            outfit.date?.let { date ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = date.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StylebookScreenPreview() {
    // ⚠️ 진짜 ViewModel 대신, 프리뷰용 더미 ViewModel을 쓰는게 가장 안전하지만
    // 여기서는 SavedOutfitViewModel() 이 기본 생성자라고 가정하고 사용.
    StylebookScreen(
        navController = rememberNavController(),
        savedOutfitViewModel = SavedOutfitViewModel()
    )
}
