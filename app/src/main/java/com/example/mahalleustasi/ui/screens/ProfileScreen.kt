package com.example.mahalleustasi.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mahalleustasi.data.model.Review
import com.example.mahalleustasi.data.model.User
import com.example.mahalleustasi.ui.theme.* // Tema entegrasyonu
import com.example.mahalleustasi.ui.viewmodel.ProfileViewModel
import com.example.mahalleustasi.ui.viewmodel.ReviewsViewModel
import com.example.mahalleustasi.ui.viewmodel.UsersViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val vm: ProfileViewModel = hiltViewModel()
    val reviewsVm: ReviewsViewModel = hiltViewModel()
    val usersVm: UsersViewModel = hiltViewModel()
    val state by vm.state.collectAsState()
    val me = state.user
    val loading = state.isLoading
    val error = state.error

    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadMe() }

    LaunchedEffect(me) {
        me?.let {
            name = it.name
            phone = it.phone ?: ""
        }
    }

    LaunchedEffect(me?.uid) {
        val uid = me?.uid ?: return@LaunchedEffect
        reviewsVm.list(uid) { loaded -> reviews = loaded }
    }

    // Değerlendirmeyi yapan kullanıcıların isimlerini yükle (reviewerId)
    LaunchedEffect(reviews) {
        val reviewerIds = reviews.mapNotNull { it.reviewerId }.filter { it.isNotBlank() }.toSet()
        if (reviewerIds.isNotEmpty()) {
            usersVm.ensureUsers(reviewerIds)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.uploadProfileImage(it) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profilim", fontWeight = FontWeight.Bold, color = MahalleTextPrimary) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (loading && me == null) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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

            // 1. Profil Kartı (Avatar, İsim)
            item {
                ProfileHeaderCard(
                    user = me,
                    onEditPhotoClick = { imagePicker.launch("image/*") }
                )
            }

            // 2. İstatistikler
            item {
                ProfileStatsSection(
                    ratingAvg = me.ratingAvg,
                    ratingCount = me.ratingCount
                )
            }

            // 3. Bilgileri Düzenleme Formu
            item {
                ModernEditForm(
                    name = name,
                    onNameChange = { name = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    isLoading = loading,
                    localError = localError,
                    apiError = error,
                    isEditing = isEditing,
                    onToggleEditing = { isEditing = !isEditing },
                    onSaveClick = {
                        if (name.isBlank()) {
                            localError = "Ad Soyad zorunludur."
                        } else {
                            localError = null
                            vm.save(name = name, phone = phone.ifBlank { null })
                            isEditing = false // Kaydettikten sonra düzenlemeyi kapat
                        }
                    }
                )
            }

            // 4. Değerlendirmeler
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ProfileReviewsSection(
                    navController = navController,
                    reviews = reviews,
                    usersVm = usersVm
                )
            }
        }
    }
}

/**
 * Avatar, isim ve fotoğrafı modern bir kart içinde gösterir.
 */
@Composable
fun ProfileHeaderCard(user: User, onEditPhotoClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar ve Edit Butonu
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                val avatarUrl = user.photoUrl
                val initials = user.name.firstOrNull()?.uppercase() ?: "?"

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onEditPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Profil Fotoğrafı",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Düzenle Butonu
                Surface(
                    onClick = onEditPhotoClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Fotoğrafı Değiştir",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // İsim ve Telefon
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MahalleTextPrimary
                )
                if (!user.phone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.phone,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MahalleTextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Puan ve Değerlendirme sayılarını renkli kutucuklarda gösterir.
 */
@Composable
fun ProfileStatsSection(ratingAvg: Double?, ratingCount: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModernStatCard(
            icon = Icons.Rounded.Star,
            label = "Ortalama Puan",
            value = ratingAvg?.let { String.format("%.1f", it) } ?: "-",
            color = MahalleOrange, // Turuncu yıldız
            modifier = Modifier.weight(1f)
        )
        ModernStatCard(
            icon = Icons.Default.RateReview,
            label = "Değerlendirme",
            value = ratingCount?.toString() ?: "0",
            color = MaterialTheme.colorScheme.primary, // Teal
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ModernStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MahalleTextPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MahalleTextSecondary
            )
        }
    }
}

/**
 * Modern form yapısı.
 */
@Composable
fun ModernEditForm(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    isLoading: Boolean,
    localError: String?,
    apiError: String?,
    isEditing: Boolean,
    onToggleEditing: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kişisel Bilgiler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MahalleTextPrimary
                )
                TextButton(onClick = onToggleEditing) {
                    Text(
                        text = if (isEditing) "Vazgeç" else "Düzenle",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Ad Soyad") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                enabled = isEditing,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text("Telefon") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                enabled = isEditing,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            if (isEditing) {
                Text(
                    text = "Bu bilgiler diğer kullanıcılar tarafından görülebilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MahalleTextSecondary
                )
            }

            // Hata mesajları
            if (localError != null || apiError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = localError ?: apiError ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Kaydet Butonu (Sadece düzenleme modunda görünür)
            if (isEditing) {
                Button(
                    onClick = onSaveClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Değişiklikleri Kaydet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileReviewsSection(
    navController: NavController,
    reviews: List<Review>,
    usersVm: UsersViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Kullanıcı Değerlendirmeleri",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MahalleTextPrimary
        )

        if (reviews.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Henüz değerlendirme yapılmamış.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MahalleTextSecondary,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            reviews.forEach { review ->
                val reviewerId = review.reviewerId
                val reviewerName = usersVm.displayName(reviewerId) ?: "Kullanıcı"
                ModernReviewItem(
                    review = review,
                    reviewerId = reviewerId,
                    reviewerName = reviewerName,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun ModernReviewItem(
    review: Review,
    reviewerId: String,
    reviewerName: String,
    navController: NavController
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Yıldızlar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        val icon = if (index < review.rating) Icons.Rounded.Star else Icons.Default.Star // Dolu veya boş yıldız eklenebilir
                        val tint = if (index < review.rating) MahalleOrange else Color.LightGray.copy(0.4f)
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Tarih
                val dateText = remember(review.createdAt) {
                    try {
                        val formatter = SimpleDateFormat("dd MMM yyyy", Locale("tr"))
                        formatter.format(Date(review.createdAt))
                    } catch (e: Exception) { "" }
                }
                if (dateText.isNotBlank()) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MahalleTextSecondary
                    )
                }
            }

            // Değerlendiren kullanıcı adı
            if (reviewerId.isNotBlank()) {
                Text(
                    text = "Değerlendiren: $reviewerName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MahalleTextPrimary,
                    modifier = Modifier.clickable {
                        navController.navigate("public_profile/$reviewerId")
                    }
                )
            }

            if (!review.comment.isNullOrBlank()) {
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MahalleTextPrimary
                )
            } else {
                Text(
                    text = "Yorum yazılmamış.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MahalleTextSecondary.copy(0.6f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}