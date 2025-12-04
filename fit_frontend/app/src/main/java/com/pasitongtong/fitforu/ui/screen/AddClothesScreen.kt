package com.pasitongtong.fitforu.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pasitongtong.fitforu.R
import com.pasitongtong.fitforu.ui.Screen
import com.pasitongtong.fitforu.viewmodel.MainViewModel
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClothesScreen(
    navController: NavController,
    mainViewModel: MainViewModel
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // 사용자가 고른(또는 촬영한) 원본 이미지 Uri
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    // 업로드 진행 상태
    var isUploading by rememberSaveable { mutableStateOf(false) }

    // 갤러리/사진 선택 런처
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
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
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
                    .clickable { launchPicker() },
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF4F4F4)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (selectedImageUri == null) {
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
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "선택한 옷 사진",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔥 버튼 동작:
            //    - 아직 사진이 없으면: 사진 선택
            //    - 사진이 있으면: /clothes/upload 로 업로드 후 옷장으로 이동
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                enabled = !isUploading,
                onClick = {
                    if (selectedImageUri == null) {
                        // 아직 사진을 안 골랐으면 사진 선택
                        launchPicker()
                    } else {
                        // 사진 있음 → 서버로 업로드
                        isUploading = true

                        mainViewModel.uploadClothesImage(
                            context = context,
                            imageUri = selectedImageUri!!
                        ) { success, urlOrError ->
                            isUploading = false

                            if (success && urlOrError != null) {
                                // TODO: 필요하면 여기서 Supabase wardrobe 테이블에 imageUrl 저장
                                Toast.makeText(
                                    context,
                                    "옷장이에 옷이 추가되었어요!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                navController.navigate(Screen.Closet.route) {
                                    popUpTo(Screen.Closet.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    "업로드 실패: ${urlOrError ?: "알 수 없는 오류"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            ) {
                val btnText = when {
                    selectedImageUri == null -> "사진 선택하기"
                    isUploading -> "업로드 중..."
                    else -> "저장하기"
                }
                Text(btnText)
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
        val fakeMainViewModel = MainViewModel()   // 👈 프리뷰용 가짜 VM

        AddClothesScreen(
            navController = navController,
            mainViewModel = fakeMainViewModel
        )
    }
}
