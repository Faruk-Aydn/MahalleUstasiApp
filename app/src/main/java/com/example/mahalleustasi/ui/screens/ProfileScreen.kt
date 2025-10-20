package com.example.mahalleustasi.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mahalleustasi.data.model.User // User modelinizin burada olduğunu varsayıyoruz
import com.example.mahalleustasi.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val vm: ProfileViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val me = state.user
    val loading = state.isLoading
    val error = state.error

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.loadMe()
    }

    // `me` nesnesi değiştiğinde `remember` state'lerini güncelle
    LaunchedEffect(me) {
        me?.let {
            name = it.name
            phone = it.phone ?: ""
        }
    }

    // Görsel seçici
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { vm.uploadProfileImage(it) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profilim", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Yükleniyor durumu (eğer kullanıcı verisi henüz gelmediyse)
            if (loading && me == null) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                return@LazyColumn
            }

            if (me == null) {
                item {
                    Text(
                        "Profil yüklenemedi: ${error ?: "Bilinmeyen hata"}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                return@LazyColumn
            }

            // 1. Profil Başlığı (Avatar, İsim, Foto Değiştirme)
            item {
                ProfileHeader(
                    user = me,
                    onEditPhotoClick = { imagePicker.launch("image/*") }
                )
            }

            // 2. İstatistikler
            if (me.ratingAvg != null || me.ratingCount != null) {
                item {
                    ProfileStats(
                        ratingAvg = me.ratingAvg,
                        ratingCount = me.ratingCount
                    )
                }
                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
                }
            }

            // 3. Bilgileri Düzenleme Formu
            item {
                EditForm(
                    name = name,
                    onNameChange = { name = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    isLoading = loading, // 'Kaydediliyor' durumu için
                    localError = localError,
                    apiError = error,
                    onSaveClick = {
                        if (name.isBlank()) {
                            localError = "Ad Soyad zorunludur."
                        } else {
                            localError = null
                            vm.save(name = name, phone = phone.ifBlank { null })
                        }
                    }
                )
            }

            // 4. Diğer Eylemler
            item {
                NavigationActions(
                    onMyJobsClick = { navController.navigate("my_jobs") }
                )
            }
        }
    }
}

/**
 * Avatar, isim ve fotoğraf düzenleme butonunu gösteren başlık.
 */
@Composable
fun ProfileHeader(user: User, onEditPhotoClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar ve Düzenle Butonu
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            val avatarUrl = user.photoUrl
            val initials = user.name.firstOrNull()?.uppercase() ?: "?"

            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profil Fotoğrafı",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable { onEditPhotoClick() }
                )
            } else {
                // Yedek Baş Harf Görünümü
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onEditPhotoClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Düzenle İkon Butonu
            IconButton(
                onClick = onEditPhotoClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Fotoğrafı Değiştir",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // İsim ve Telefon
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            user.phone?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Puan ve Değerlendirme sayısı gibi istatistikleri gösterir.
 */
@Composable
fun ProfileStats(ratingAvg: Double?, ratingCount: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            icon = Icons.Default.Star,
            label = "Puan",
            value = ratingAvg?.let { String.format("%.1f", it) } ?: "-"
        )
        StatItem(
            icon = Icons.Default.RateReview,
            label = "Değerlendirme",
            value = ratingCount?.toString() ?: "0"
        )
    }
}

/**
 * ProfileStats içinde kullanılan küçük bir istatistik bileşeni.
 */
@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * İsim ve telefonun düzenlendiği form bölümü.
 */
@Composable
fun EditForm(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    localError: String?,
    apiError: String?,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Kişisel Bilgiler",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Ad Soyad") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Ad") }
        )
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Telefon") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Telefon") }
        )

        // Hata mesajları
        localError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        apiError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        // Kaydet Butonu
        Button(
            onClick = onSaveClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Değişiklikleri Kaydet")
            }
        }
    }
}

/**
 * Diğer sayfalara giden navigasyon butonları.
 */
@Composable
fun NavigationActions(onMyJobsClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider()
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Diğer Eylemler",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        FilledTonalButton(
            onClick = onMyJobsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("İlanlarım")
        }
    }
}