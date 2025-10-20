package com.example.mahalleustasi.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
 
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextField
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.ui.viewmodel.JobsViewModel
import com.example.mahalleustasi.utils.GeocodingUtils
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.Alignment
 
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobCreateScreen(navController: NavController, viewModel: JobsViewModel) {
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    val categoryOptions = listOf("Temizlik", "Boya & Badana", "Tesisat", "Elektrik", "Nakliyat", "Tamir", "Bahçe", "İnşaat", "Diğer")
    var description by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var isCash by rememberSaveable { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var pickedLatStr by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedLngStr by rememberSaveable { mutableStateOf<String?>(null) }
    var pickedAddress by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Seçilen fotoğraflar ViewModel state'inden gelir (navigasyonlar arasında korunur)
    val selectedPhotos by viewModel.selectedPhotos.collectAsState()
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    viewModel.addSelectedPhoto(stream.readBytes())
                }
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        bitmap?.let {
            val baos = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 90, baos)
            viewModel.addSelectedPhoto(baos.toByteArray())
        }
    }

    // LocationPicker dönüşünü yakala (LiveData ile güvenilir dinleme)
    val backEntry = navController.currentBackStackEntry
    val latState = backEntry?.savedStateHandle?.getLiveData<Double>("picked_lat")?.observeAsState()
    val lngState = backEntry?.savedStateHandle?.getLiveData<Double>("picked_lng")?.observeAsState()
    LaunchedEffect(latState?.value, lngState?.value) {
        val lat = latState?.value
        val lng = lngState?.value
        if (lat != null && lng != null) {
            pickedLatStr = lat.toString()
            pickedLngStr = lng.toString()
            // tek seferlik tüket
            backEntry?.savedStateHandle?.remove<Double>("picked_lat")
            backEntry?.savedStateHandle?.remove<Double>("picked_lng")
        }
    }

    // Reverse geocoding
    val context2 = context
    LaunchedEffect(pickedLatStr, pickedLngStr) {
        val lat = pickedLatStr?.toDoubleOrNull()
        val lng = pickedLngStr?.toDoubleOrNull()
        if (lat != null && lng != null) {
            pickedAddress = GeocodingUtils.reverseGeocode(context, lat, lng)
        } else {
            pickedAddress = null
        }
    }

    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Add these validation and save functions
    fun validateInputs(): Boolean {
        error = when {
            title.length < 5 -> "Başlık en az 5 karakter olmalıdır."
            category.isBlank() -> "Kategori boş bırakılamaz."
            description.length < 20 -> "Açıklama en az 20 karakter olmalıdır."
            price.isNotEmpty() && price.toDoubleOrNull() == null -> "Geçerli bir ücret giriniz."
            pickedLatStr == null || pickedLngStr == null -> "Lütfen konum seçiniz."
            else -> null
        }
        return error == null
    }

    fun saveJob() {
        isLoading = true
        val job = Job(
            title = title.trim(),
            description = description.trim(),
            price = price.toDoubleOrNull(),
            isCash = isCash,
            category = category.trim().ifBlank { null },
            location = run {
                val lat = pickedLatStr?.toDoubleOrNull()
                val lng = pickedLngStr?.toDoubleOrNull()
                if (lat != null && lng != null) {
                    Job.JobLocation(lat = lat, lng = lng, address = pickedAddress)
                } else null
            }
        )
        
        viewModel.create(job) { createdId ->
            scope.launch {
                if (selectedPhotos.isNotEmpty()) {
                    val tasks = selectedPhotos.mapIndexed { index, bytes ->
                        async {
                            viewModel.uploadAndAttachPhotoAwait(
                                jobId = createdId,
                                bytes = bytes,
                                fileName = "photo_${System.currentTimeMillis()}_${index}.jpg"
                            )
                        }
                    }
                    tasks.awaitAll()
                }
                isLoading = false
                viewModel.clearSelectedPhotos()
                navController.navigate("job_detail/$createdId") {
                    popUpTo("jobs") { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("İş İlanı Oluştur") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Başlık") },
                modifier = Modifier.fillMaxWidth(),
                isError = error?.contains("başlık", ignoreCase = true) == true,
                supportingText = { Text("En az 5 karakter giriniz") }
            )

            // Category Input (Dropdown)
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                TextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategori") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    isError = error?.contains("Kategori", ignoreCase = true) == true
                )

                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categoryOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Açıklama") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                isError = error?.contains("açıklama", ignoreCase = true) == true,
                supportingText = { Text("En az 20 karakter giriniz") }
            )

            // Price Input
            OutlinedTextField(
                value = price,
                onValueChange = { 
                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                        price = it
                    }
                },
                label = { Text("Ücret (TL)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Icon(Icons.Default.MonetizationOn, "Ücret") }
            )

            // Photo Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Fotoğraflar",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Galeri")
                        }
                        
                        FilledTonalButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Kamera")
                        }
                    }

                    if (selectedPhotos.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedPhotos) { photoBytes ->
                                Box {
                                    val bitmap = remember(photoBytes) {
                                        BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.size)
                                            .asImageBitmap()
                                    }
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(100.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = {
                                            viewModel.removeSelectedPhoto(photoBytes)
                                        },
                                        modifier = Modifier.align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Sil",
                                            tint = ComposeColor.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Location Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Konum",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    FilledTonalButton(
                        onClick = { navController.navigate("location_picker") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.LocationOn, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (pickedLatStr != null) "Konumu Değiştir" else "Konum Seç"
                        )
                    }

                    if (pickedLatStr != null && pickedLngStr != null) {
                        Text(
                            pickedAddress ?: "Adres yükleniyor...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (validateInputs()) {
                        saveJob()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("İlanı Yayınla")
                }
            }

            // Error Display
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
