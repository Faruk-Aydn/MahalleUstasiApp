package com.example.mahalleustasi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mahalleustasi.data.model.Offer
import com.example.mahalleustasi.ui.theme.* // <-- DİKKAT: Tema dosyamızı import ettik!
import com.example.mahalleustasi.ui.viewmodel.OffersViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    navController: NavController,
    viewModel: OffersViewModel
) {
    val offers by viewModel.offers.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMyOffers()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // Temadan gelen açık gri zemin
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Tekliflerim",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MahalleTextPrimary
                    )
                },
                actions = {},
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // --- İSTATİSTİK ÖZETİ (DASHBOARD) ---
            if (offers.isNotEmpty()) {
                OffersSummaryHeader(offers)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- LİSTE ---
            if (loading && offers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (!loading && offers.isEmpty()) {
                ModernEmptyState(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(offers, key = { it.id }) { offer ->
                        PremiumOfferCard(
                            offer = offer,
                            onCardClick = { navController.navigate("job_detail/${offer.jobId}") },
                            onWithdrawClick = { viewModel.withdraw(offer.id, null) }
                        )
                    }
                }
            }
        }
    }
}

// --------------------------- BİLEŞENLER --------------------------- //

@Composable
fun OffersSummaryHeader(offers: List<Offer>) {
    val total = offers.size
    val pending = offers.count { it.status == "pending" }
    val accepted = offers.count { it.status == "accepted" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryChip(
            title = "Toplam",
            count = total,
            color = MaterialTheme.colorScheme.primary, // Mahalle Teal
            icon = Icons.Outlined.Assignment,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            title = "Bekleyen",
            count = pending,
            color = MahalleOrange, // Mahalle Orange
            icon = Icons.Default.AccessTime,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            title = "Onaylanan",
            count = accepted,
            color = StatusCompletedText, // Theme Yeşil
            icon = Icons.Default.CheckCircle,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryChip(title: String, count: Int, color: Color, icon: ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp // Biraz gölge ile öne çıkardık
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MahalleTextPrimary
            )
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = MahalleTextSecondary
            )
        }
    }
}

@Composable
fun PremiumOfferCard(
    offer: Offer,
    onCardClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    // Theme.kt içindeki renkleri kullanarak durum yönetimi
    val (statusColor, statusIcon, statusText, statusBg) = when (offer.status) {
        "pending" -> Quadruple(
            StatusPendingText, // Turuncu Metin
            Icons.Default.AccessTime,
            "Cevap Bekliyor",
            StatusPendingBg // Açık Turuncu Zemin
        )
        "accepted" -> Quadruple(
            StatusCompletedText, // Yeşil Metin
            Icons.Default.CheckCircle,
            "Teklif Onaylandı",
            StatusCompletedBg // Açık Yeşil Zemin
        )
        "rejected" -> Quadruple(
            MaterialTheme.colorScheme.error, // Standart Kırmızı
            Icons.Default.DoNotDisturbOn,
            "Reddedildi",
            MaterialTheme.colorScheme.errorContainer // Açık Kırmızı Zemin
        )
        "withdrawn" -> Quadruple(
            MahalleTextSecondary,
            Icons.Default.DoNotDisturbOn,
            "Geri çekildi",
            MaterialTheme.colorScheme.surfaceVariant
        )
        else -> Quadruple(
            MahalleTextSecondary,
            Icons.Default.AccessTime,
            "İşlemde",
            Color.LightGray.copy(alpha = 0.2f)
        )
    }

    Card(
        onClick = onCardClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Hafif derinlik
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // --- Header: Durum İkonu + Başlık + Fiyat ---
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    // Durum İkon Kutusu
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Başlık ve ID
                    Column {
                        // Eğer jobTitle yoksa zarif bir fallback gösteriyoruz
                        Text(
                            text = "Hizmet Teklifi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MahalleTextPrimary
                        )
                        Text(
                            text = "#${offer.jobId.takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MahalleTextSecondary
                        )
                    }
                }

                // Fiyat
                Text(
                    text = NumberFormat.getNumberInstance(Locale("tr")).format(offer.amount ?: 0.0) + " ₺",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary // Marka Rengi (Teal)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Not Alanı (Varsa) ---
            if (!offer.note.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.background, // Hafif gri zemin
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Note, null, tint = MahalleTextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = offer.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MahalleTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Divider(color = Color.Gray.copy(alpha = 0.1f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // --- Alt Footer: Durum Metni ve Tarih/Buton ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Durum Metni
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }

                // Buton veya Tarih
                if (offer.status == "pending") {
                    // Sadece beklemedeyse Geri Çek butonu
                    OutlinedButton(
                        onClick = onWithdrawClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Geri Çek", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Diğer durumlarda tarih göster
                    val date = SimpleDateFormat("dd MMM, HH:mm", Locale("tr")).format(Date(offer.createdAt))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, null, tint = MahalleTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(date, style = MaterialTheme.typography.labelSmall, color = MahalleTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ModernEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // İllüstrasyon Yerine İkon Grubu
        Box(
            modifier = Modifier.size(120.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape) // Tema Rengi Opak
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MonetizationOn,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // Küçük dekoratif ikon (Turuncu)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = 10.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp),
                    tint = MahalleOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Henüz Teklif Vermediniz",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MahalleTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Mahallenizdeki iş fırsatlarını inceleyerek ilk teklifinizi hemen verin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MahalleTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// Yardımcı Data Class
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)