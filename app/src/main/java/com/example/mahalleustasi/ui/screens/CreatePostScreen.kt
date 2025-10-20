package com.example.mahalleustasi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mahalleustasi.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(navController: NavController, viewModel: HomeViewModel) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni İlan Oluştur") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Başlık") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Açıklama") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (title.isNotBlank() && description.isNotBlank()) {
                        viewModel.addPost(title, description)
                        navController.popBackStack() // Bir önceki ekrana dön
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "İlanı Yayınla")
            }
        }
    }
}