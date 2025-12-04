package com.pasitongtong.fitforu.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pasitongtong.fitforu.R
import com.pasitongtong.fitforu.viewmodel.MainViewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Settings
import com.pasitongtong.fitforu.ui.Screen
import androidx.compose.runtime.collectAsState

import coil.compose.AsyncImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.Card
import com.pasitongtong.fitforu.model.WardrobeItem
import androidx.compose.ui.text.style.TextAlign



// 카테고리 정보 (이모지로 아이콘 표현)
data class ClosetCategory(
    val id: String,
    val label: String,
    val emoji: String
)

// 실제로 사용할 카테고리들
private val closetCategories = listOf(
    ClosetCategory(id = "ALL",    label = "전체",  emoji = "\uD83D\uDDC4"),  // 전체 = 티셔츠 아이콘
    ClosetCategory(id = "OUTER",  label = "아우터", emoji = "🧥"),
    ClosetCategory(id = "TOP",    label = "상의",  emoji = "👚"),
    ClosetCategory(id = "BOTTOM", label = "하의",  emoji = "👖"),
    ClosetCategory(id = "DRESS",  label = "원피스", emoji = "👗")
)

// 카테고리 정보(백엔드 연동 시 id 값만 맞춰 쓰면 됨)
private enum class ClothesCategory(val id: String, val label: String, val emoji: String) {
    ALL("ALL", "전체", "\uD83D\uDDC4"),      // 옷걸이 느낌 이모지
    OUTER("OUTER", "아우터", "\uD83E\uDDE5"), // 🧥
    TOP("TOP", "상의", "\uD83D\uDC55"),      // 👕
    BOTTOM("BOTTOM", "하의", "\uD83D\uDC56"), // 👖
    DRESS("DRESS", "원피스", "\uD83D\uDC57")  // 👗
}

@Composable
fun ClosetScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    var selectedCategoryId by remember { mutableStateOf("ALL") }
    val closetUiState by viewModel.closetUiState.collectAsState()

    // 🔥 1) 총 개수 = 서버에서 가져온 옷 개수
    val totalCount = closetUiState.items.size

    Scaffold(
        containerColor = Color.White,
        topBar = {
            ClosetTopBar(
                onBackClick = { navController.popBackStack() },
                onAddClick = { navController.navigate(Screen.AddClothes.route) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "내 옷",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                closetCategories.forEach { category ->
                    ClosetCategoryChip(
                        category = category,
                        selected = selectedCategoryId == category.id,
                        onClick = { selectedCategoryId = category.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "총 ${totalCount}개",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "저장한 순",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "정렬 변경",
                        modifier = Modifier
                            .size(18.dp)
                            .padding(start = 2.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ------- 옷 리스트 영역 -------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF3F4F7)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                val filteredItems = remember(selectedCategoryId, closetUiState.items) {
                    when (selectedCategoryId) {
                        "ALL" -> closetUiState.items
                        "OUTER" -> closetUiState.items.filter { it.major_category == "아우터" }
                        "TOP" -> closetUiState.items.filter { it.major_category == "상의" }
                        "BOTTOM" -> closetUiState.items.filter { it.major_category == "하의" }
                        "DRESS" -> closetUiState.items.filter { it.major_category == "원피스" }
                        else -> closetUiState.items
                    }
                }


                if (filteredItems.isEmpty()) {
                    // 옷이 하나도 없을 때
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "아직 등록된 옷이 없어요.\n+ 버튼을 눌러 옷을 추가해보세요!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // 🔥 3) 서버에서 받은 옷 리스트를 그리드로 표시
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = filteredItems,
                            key = { it.id }
                        ) { item ->
                            ClosetNetworkItemCard(
                                item = item,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClosetNetworkItemCard(
    item: WardrobeItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(3f / 4f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = item.image_url,
                contentDescription = "${item.major_category} - ${item.minor_category}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}



/**
 * 상단 AppBar
 */
@Composable
private fun ClosetTopBar(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        // ⬅️ 뒤로가기 버튼
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "뒤로가기"
            )
        }

        // 🟣 중앙 텍스트
        Text(
            text = "FitForU",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        // ➕ 추가 버튼
        IconButton(
            onClick = onAddClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "옷 추가하기"
            )
        }
    }

}

/**
 * 카테고리 원형 아이콘 Row
 */
@Composable
private fun CategoryChipRow(
    selected: ClothesCategory,
    onSelectedChange: (ClothesCategory) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClothesCategory.values().forEach { category ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelectedChange(category) }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected == category)
                                Color(0xFFE0E0E0)   // 선택됨
                            else
                                Color(0xFFF0F0F0)   // 기본
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.emoji,
                        fontSize = 28.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * "내 옷" 영역에 들어가는 옷 카드
 * - 이미지 + 오른쪽 위 북마크 아이콘
 * - 오른쪽 아래 설정(톱니바퀴) 아이콘
 */
@Composable
private fun ClosetThumbnailCard(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier
) {
    var bookmarked by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .aspectRatio(0.75f), // 살짝 세로로 긴 비율
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "옷 이미지",
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )

            // 북마크 (상단 오른쪽)
            IconButton(
                onClick = { bookmarked = !bookmarked },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.BookmarkBorder,
                    contentDescription = "저장하기",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 설정 (하단 오른쪽)
            IconButton(
                onClick = {
                    // TODO: 옷 상세 설정 화면으로 이동
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "설정",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
private fun ClosetCategoryChip(
    category: ClosetCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 동그란 배경
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (selected) Color(0xFFE6E8F5) else Color(0xFFF2F3F7)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.emoji,
                fontSize = 28.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
@Composable
private fun ClosetItemCard(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier
) {
    var isBookmarked by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .aspectRatio(3f / 4f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "옷 이미지",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            )

            // 북마크 (위 오른쪽)
            IconButton(
                onClick = { isBookmarked = !isBookmarked },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = if (isBookmarked)
                        Icons.Filled.Bookmark
                    else
                        Icons.Filled.BookmarkBorder,
                    contentDescription = null,
                    tint = if (isBookmarked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 설정 (아래 오른쪽)
            IconButton(
                onClick = { /* TODO: 설정/편집 화면으로 이동 */ },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "설정",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
