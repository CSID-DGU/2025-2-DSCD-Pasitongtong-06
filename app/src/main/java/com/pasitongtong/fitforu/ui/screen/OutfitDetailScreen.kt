package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pasitongtong.fitforu.model.SavedOutfit
import com.pasitongtong.fitforu.R


@Composable
fun OutfitDetailScreen(
    navController: NavController,
    onSaveOutfit: (SavedOutfit) -> Unit = {}
) {
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
    var isBookmarked by remember { mutableStateOf(false) }
    val currentOutfit = outfitList[selectedIndex]

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { navController.popBackStack() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Best Clothes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                // 🔥 책갈피 아이콘 (저장 버튼)
                Icon(
                    imageVector = if (isBookmarked)
                        Icons.Filled.Bookmark
                    else
                        Icons.Filled.BookmarkBorder,
                    contentDescription = "저장",
                    tint = if (isBookmarked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            isBookmarked = !isBookmarked
                            if (isBookmarked) onSaveOutfit(currentOutfit)
                        }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentOutfit.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Image(
                painter = painterResource(id = currentOutfit.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                itemsIndexed(outfitList) { index, item ->
                    Image(
                        painter = painterResource(id = item.imageRes),
                        contentDescription = "썸네일",
                        modifier = Modifier
                            .size(92.dp)
                            .padding(4.dp)
                            .clickable { selectedIndex = index }
                            .border(
                                width = if (selectedIndex == index) 3.dp else 1.dp,
                                color = if (selectedIndex == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.medium
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "사용자님은 내추럴형입니다.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = currentOutfit.bodyText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentOutfit.tip,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
