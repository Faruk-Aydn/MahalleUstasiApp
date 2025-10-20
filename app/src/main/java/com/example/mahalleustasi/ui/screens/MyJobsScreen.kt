package com.example.mahalleustasi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.ui.viewmodel.MyJobsViewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobsScreen(navController: NavController, viewModel: MyJobsViewModel = hiltViewModel()) {
    val owned by viewModel.ownedJobs.collectAsState()
    val assigned by viewModel.assignedJobs.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    // Auth kullanıcısı hazır olduğunda yükle
    val uid = Firebase.auth.currentUser?.uid
    LaunchedEffect(uid) {
        if (uid != null) {
            viewModel.loadOwned()
            viewModel.loadAssigned()
        }
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "İşlerim",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.loadOwned(); viewModel.loadAssigned() }, enabled = !loading) { Text("Yenile") }
            }
            if (error != null) {
                Text(text = "Hata: ${error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Sahibi Olduğum", modifier = Modifier.padding(12.dp)) }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Atandıklarım", modifier = Modifier.padding(12.dp)) }
            }

            val list = if (selectedTab == 0) owned else assigned
            when {
                loading -> {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
                list.isEmpty() -> {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Kayıt bulunamadı.", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = if (selectedTab == 0) "Sahibi olduğunuz bir iş bulunamadı." else "Atandığınız bir iş bulunamadı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(list, key = { it.id }) { job ->
                            JobItemCard(
                                job = job,
                                onOpen = { navController.navigate("job_detail/${job.id}") },
                                onMessage = {
                                    val ownerId = job.ownerId
                                    job.assignedProId?.let { proId ->
                                        val (a, b) = if (ownerId <= proId) ownerId to proId else proId to ownerId
                                        val chatId = "job_${job.id}_${a}_${b}"
                                        navController.navigate("chat/$chatId")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobItemCard(job: Job, onOpen: () -> Unit, onMessage: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(job.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(job.status.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            job.category?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            val priceText = job.price?.let { "${it} TL" } ?: "Belirtilmemiş"
            Text("Fiyat: $priceText", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen) { Text("Detay") }
                if (job.assignedProId != null) {
                    Button(onClick = onMessage) { Text("Mesajlaş") }
                }
            }
        }
    }
}
