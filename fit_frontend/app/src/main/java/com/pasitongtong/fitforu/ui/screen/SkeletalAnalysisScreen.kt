package com.pasitongtong.fitforu.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pasitongtong.fitforu.R
import com.pasitongtong.fitforu.ui.Screen
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.background

@Composable
fun SkeletalAnalysisScreen(navController: NavController) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    Scaffold(
        topBar = { TopHeader(navController) },
        containerColor = Color.White      // 🔥 전체 화면 흰색
    ) { innerPadding ->

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .background(Color.White)  // 🔥 내부도 흰색
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            PhotoUploadCard(
                imageUri = selectedImageUri,
                onClick = { galleryLauncher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate(Screen.AnalysisResult.route) },
                enabled = selectedImageUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("분석하기")
            }

            Spacer(modifier = Modifier.height(28.dp))

            BodyTipSection()
            Spacer(modifier = Modifier.height(28.dp))

            SkeletalTypeInfo()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
fun TopHeader(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp)
    ) {
        // ⬅️ Back 버튼 (왼쪽 정렬)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "뒤로가기",
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterStart)
                .clickable {
                    navController.navigate(route = "home") {
                        popUpTo("home") { inclusive = true }
                        launchSingleTop = true
                    }
                }
        )

        // 🔥 중앙 제목
        Text(
            text = "FitForU",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        // ➕ 오른쪽 여백을 맞추는 목적 (필요하면)
        Spacer(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterEnd)
        )
    }
}


@Composable
fun PhotoUploadCard(imageUri: Uri?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0F0F0)
        )
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "선택된 전신 사진",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "사진 추가",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("전신 사진을 추가해주세요!", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "정확한 골격 진단을 위해 가벼운 옷차림을 권장합니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 *  “! 꼭 읽어보세요 / [정확한 체형 분석 Tip] + body.png + bullet 텍스트”
 */
@Composable
fun BodyTipSection() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "! 꼭 읽어보세요",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "[정확한 체형 분석 Tip]",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.body),
            contentDescription = "전신 촬영 가이드",
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "• 머리: 긴 머리는 어깨 뒤로 넘겨주세요.\n" +
                    "• 손: 손바닥을 허벅지 옆에 살짝 떼어 두세요.\n" +
                    "• 의상: 신체 라인이 드러나는 상·하의를 착용해주세요.\n" +
                    "• 조명: 밝고 그림자가 적은 환경에서 촬영해주세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 *  하단 체형 타입 안내 섹션 (women.png / men.png 사용)
 */
@Composable
fun SkeletalTypeInfo() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 안내 아이콘 + 문구
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "핏포유는 다음과 같은 체형으로 구분합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --------- [여성 사용자일 경우] ----------
        Text(
            text = "[여성 사용자일 경우]",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = painterResource(id = R.drawable.women),
            contentDescription = "여성 체형 타입 안내",
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )

        // 🔹 여성 체형 이름 4개 (모래시계 / 역삼각형 / 삼각형 / 직사각형)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("모래시계형", "역삼각형", "삼각형", "직사각형").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --------- [남성 사용자일 경우] ----------
        Text(
            text = "[남성 사용자일 경우]",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = painterResource(id = R.drawable.men),
            contentDescription = "남성 체형 타입 안내",
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )

        // 🔹 남성 체형 이름 3개 (작은 역삼각형 / 사각형 / 큰 사각형)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("작은 역삼각형", "사각형", "큰 사각형").forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
