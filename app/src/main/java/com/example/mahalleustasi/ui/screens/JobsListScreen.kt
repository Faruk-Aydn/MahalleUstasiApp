package com.example.mahalleustasi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mahalleustasi.ui.viewmodel.JobsViewModel
import com.example.mahalleustasi.ui.components.AppScaffold
import com.example.mahalleustasi.ui.components.FormTextField
import com.example.mahalleustasi.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsListScreen(navController: NavController, viewModel: JobsViewModel) {
    val jobs by viewModel.jobs.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var category by remember { mutableStateOf("") }
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var filterError by remember { mutableStateOf<String?>(null) }

    // Gelişmiş filtre durumları
    var isCashOption by remember { mutableStateOf("Hepsi") } // Hepsi | Nakit | Kart
    val isCashValue: Boolean? = when (isCashOption) {
        "Nakit" -> true
        "Kart" -> false
        else -> null
    }
    var hasPhotos by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("createdAt") } // createdAt | price
    var sortAsc by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startListening()
        viewModel.refresh()
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FormTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = "Kategori",
                        modifier = Modifier.weight(1f)
                    )
                    FormTextField(
                        value = minPrice,
                        onValueChange = { if (it.all { c -> c.isDigit() }) minPrice = it },
                        label = "Min Fiyat",
                        modifier = Modifier.weight(1f)
                    )
                    FormTextField(
                        value = maxPrice,
                        onValueChange = { if (it.all { c -> c.isDigit() }) maxPrice = it },
                        label = "Max Fiyat",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isCashOption = when (isCashOption) {
                                "Hepsi" -> "Nakit"
                                "Nakit" -> "Kart"
                                else -> "Hepsi"
                            }
                        },
                        enabled = true
                    ) { Text("Ödeme: $isCashOption") }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Fotoğraflı")
                        Switch(checked = hasPhotos, onCheckedChange = { hasPhotos = it })
                    }

                    OutlinedButton(
                        onClick = { sortBy = if (sortBy == "createdAt") "price" else "createdAt" },
                        enabled = true
                    ) { Text("Sırala: ${if (sortBy == "createdAt") "Tarih" else "Fiyat"}") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Artan")
                        Switch(checked = sortAsc, onCheckedChange = { sortAsc = it })
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val min = minPrice.toDoubleOrNull()
                            val max = maxPrice.toDoubleOrNull()
                            if ((minPrice.isNotBlank() && min == null) || (maxPrice.isNotBlank() && max == null)) {
                                filterError = "Fiyat alanları sadece rakam olmalı."
                            } else {
                                filterError = null
                                viewModel.applyAdvancedFilters(
                                    category = category.ifBlank { null },
                                    minPrice = min,
                                    maxPrice = max,
                                    isCash = isCashValue,
                                    hasPhotos = if (hasPhotos) true else null,
                                    sortBy = sortBy,
                                    sortAsc = sortAsc
                                )
                                showFilterSheet = false
                            }
                        },
                    ) { Text("Filtrele") }
                    OutlinedButton(
                        onClick = {
                            category = ""
                            minPrice = ""
                            maxPrice = ""
                            isCashOption = "Hepsi"
                            hasPhotos = false
                            sortBy = "createdAt"
                            sortAsc = false
                            filterError = null
                            viewModel.applyAdvancedFilters(null, null, null, null, null, null, false)
                            showFilterSheet = false
                        },
                    ) { Text("Temizle") }
                }
                filterError?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red)
                        Text(text = it, color = Color.Red, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }

    AppScaffold(
        title = "İşler",
        actions = {
            IconButton(onClick = { showFilterSheet = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filtrele")
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Filtre alanları artık AppBar'daki filtre ikonu ile açılan ModalBottomSheet içerisinde

                    Spacer(modifier = Modifier.height(12.dp))

                    if (loading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (jobs.isEmpty()) {
                        EmptyState(title = "Henüz iş bulunamadı", description = "İlk işi sen oluştur!")
                    } else {
                        jobs.forEach { job ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { navController.navigate("job_detail/${job.id}") }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = job.title, style = MaterialTheme.typography.titleMedium)
                                    Text(text = job.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (job.status != "open") {
                                            Text(
                                                text = "Durum: ${job.status}",
                                                color = Color(0xFFD32F2F),
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                        }
                                        job.location?.address?.let { addr ->
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF1976D2))
                                            Text(text = addr, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    error?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red)
                            Text("Hata: $it", color = Color.Red, modifier = Modifier.padding(start = 4.dp))
                            OutlinedButton(
                                onClick = { viewModel.refresh() },
                                modifier = Modifier.padding(start = 8.dp)
                            ) { Text("Tekrar Dene") }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { navController.navigate("job_create") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "İş Oluştur")
                }
            }
        }
    )
}

