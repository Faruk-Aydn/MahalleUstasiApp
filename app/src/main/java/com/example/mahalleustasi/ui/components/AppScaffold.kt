package com.example.mahalleustasi.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    actions: @Composable () -> Unit = {},
    // Hatanın çözülmesini sağlayan yeni parametre
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        },
        // Yeni parametreyi burada kullanıyoruz
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        content = content
    )
}