package com.pasitongtong.fitforu.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pasitongtong.fitforu.viewmodel.MainViewModel
import com.pasitongtong.fitforu.R
import androidx.compose.foundation.BorderStroke

@Composable
fun ClosetScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    Scaffold(
        topBar = {
            ClosetTopBar(
                onBackClick = { navController.popBackStack() },
                onCalendarClick = {
                    // TODO: 캘린더 화면으로 이동할 때 연결
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(modifier = Modifier.height(12.dp))

            // ================= 상단 두 개 카드 =================
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ClosetHeaderCard(
                    title = "나의 옷장 만들기",
                    icon = Icons.Filled.Collections,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        // TODO: 옷장 생성 화면으로 이동
                    }
                )
                ClosetHeaderCard(
                    title = "내 옷장",
                    icon = Icons.Filled.Person,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        // TODO: 내 옷장 상세 화면
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================= "모든 옷" 섹션 =================
            Text(
                text = "모든 옷",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 큰 박스 안에 2x2 정사각형 그리드 ( + 카드 + 샘플 옷 3개 )
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // (+) 카드 – 추후 Supabase 업로드/선택 트리거
                        AddClothesCard(
                            onClick = {
                                // TODO: 갤러리/카메라 or Supabase 업로드 연결
                            }
                        )

                        ClosetThumbnailCard(
                            imageRes = R.drawable.closet1,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClosetThumbnailCard(
                            imageRes = R.drawable.closet2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        ClosetThumbnailCard(
                            imageRes = R.drawable.closet3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================= 보관함 / 저장한 코디 보기 =================
            ActionRowCard(
                title = "보관함",
                leadingIcon = Icons.Filled.CloudUpload,
                onClick = {
                    // TODO: 보관함 화면 이동
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ActionRowCard(
                title = "저장한 코디 보기",
                leadingIcon = Icons.Filled.BookmarkBorder,
                onClick = {
                    // TODO: 저장된 코디 목록 화면 이동
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 상단 AppBar
 */
@Composable
private fun ClosetTopBar(
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "뒤로가기"
                )
            }
            Text(
                text = "Closet",
                // Skeletal Analysis 와 비슷한 크기로 맞추기
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onCalendarClick) {
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = "캘린더"
            )
        }
    }
}

/**
 * 상단의 "나의 옷장 만들기 / 내 옷장" 카드
 * → 테두리 있는 라운드 카드
 */
@Composable
private fun ClosetHeaderCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * (+) 정사각형 카드
 */
@Composable
private fun AddClothesCard(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f) // 정사각형
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "옷 추가하기",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
    }
}

/**
 * "모든 옷"에 들어가는 썸네일 카드
 * - 정사각형 안에 이미지 전체가 보이도록 ContentScale.Fit 사용
 * - 추후 Supabase 이미지를 넣을 수 있도록 imageUrl 파라미터도 열어둠
 */
@Composable
private fun ClosetThumbnailCard(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
    imageUrl: String? = null // TODO: Coil AsyncImage 등으로 Supabase URL 렌더링
) {
    Card(
        modifier = modifier
            .aspectRatio(1f), // 정사각형
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        // 현재는 로컬 리소스만 사용
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = "옷 이미지",
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentScale = ContentScale.Fit // 잘리지 않게
        )
    }
}

/**
 * 아래쪽 "보관함 / 저장한 코디 보기" 한 줄 카드
 * - 높이·패딩을 줄여서 타이포 크기와 비율 맞추기
 */
@Composable
private fun ActionRowCard(
    title: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
