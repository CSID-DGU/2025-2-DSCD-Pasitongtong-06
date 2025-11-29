package com.pasitongtong.fitforu.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.pasitongtong.fitforu.R
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClothesScreen(
    navController: NavController
) {
    val scrollState = rememberScrollState()

    // 선택한 이미지 Uri
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // 갤러리에서 사진 한 장 선택
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    fun launchPicker() {
        pickImageLauncher.launch("image/*")
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "FitForU",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState)       // 🔹 아래까지 스크롤 가능
        ) {

            Text(
                text = "나의 옷장, 나의 스타일 👕",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "사진을 추가해 나만의 옷장을 완성하세요!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ───── 큰 업로드 카드 ─────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable { launchPicker() },   // 🔹 카드 눌러도 갤러리 열림
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF4F4F4)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (selectedImageUri == null) {
                    // 아직 선택 안 했을 때 – 기존 안내 UI
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("+", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "옷 사진을 추가해주세요!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "옷의 핏과 스타일 기록을 위해, 직접 착용하고 전신 사진을 찍어주세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    // 사진을 선택한 후 – 선택한 이미지 보여주기
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "선택한 옷 사진",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ───── 추가하기 버튼: 갤러리 열기 ─────
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                onClick = { launchPicker() }
            ) {
                Text("추가하기")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "! 꼭 읽어보세요",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "[옷장 등록 Tip]",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ───── TIP 카드 (아래까지 스크롤 가능) ─────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.save_clothes),
                        contentDescription = "옷 촬영 예시",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• 포즈: 몸의 라인이 잘 보이도록 정면을 보고 곧게 서서 촬영하세요.\n" +
                                "• 배경: 주변에 불필요한 물건이 없는 곳에서 촬영하세요.\n" +
                                "• 조명: 옷의 색상이 잘 드러나도록 밝고 그림자가 없는 환경에서 촬영하세요.\n" +
                                "• 전신: 머리부터 발끝까지 모든 부분이 사진에 포함되도록 촬영해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAddClothesScreen() {
    MaterialTheme {
        val navController = rememberNavController()
        AddClothesScreen(navController = navController)
    }
}
