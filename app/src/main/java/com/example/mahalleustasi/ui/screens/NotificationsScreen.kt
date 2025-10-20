package com.example.mahalleustasi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mahalleustasi.ui.viewmodel.NotificationsViewModel

@Composable
fun NotificationsScreen(navController: NavController, viewModel: NotificationsViewModel = hiltViewModel()) {
    val items = viewModel.items.collectAsState().value
    val loading = viewModel.loading.collectAsState().value

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row {
            Button(onClick = { viewModel.refresh() }, enabled = !loading) { Text(if (loading) "Yükleniyor..." else "Yenile") }
        }
        Spacer(Modifier.height(12.dp))
        items.forEach { n ->
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { n.data?.get("jobId")?.let { navController.navigate("job_detail/$it") } }) {
                Text(n.title)
                Text(n.body)
                Row(modifier = Modifier.padding(top = 6.dp)) {
                    if (!n.read) Button(onClick = { viewModel.markAsRead(n.id) }) { Text("Okundu") }
                }
            }
        }
    }
}
