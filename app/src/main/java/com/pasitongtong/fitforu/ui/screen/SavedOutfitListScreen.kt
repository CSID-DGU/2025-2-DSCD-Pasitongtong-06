package com.pasitongtong.fitforu.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pasitongtong.fitforu.viewmodel.OutfitViewModel
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SavedOutfitListScreen(navController: NavController, outfitViewModel: OutfitViewModel) {

    val outfits by outfitViewModel.savedOutfits.collectAsState()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { navController.navigateUp() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("저장한 코디", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { innerPadding ->

        if (outfits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("아직 저장한 코디가 없어요 😢")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(outfits.size) { index ->
                    val item = outfits[index]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = item.imageRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(item.title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(item.bodyText)
                            Spacer(Modifier.height(4.dp))
                            Text("Tip: ${item.tip}", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
