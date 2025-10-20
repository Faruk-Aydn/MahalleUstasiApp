package com.example.mahalleustasi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox // Boş durum için ikon
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
// Pull-to-Refresh bağımlılığı mevcut değil, bu yüzden kaldırıldı
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
// nestedScroll bağlantısı Pull-to-Refresh ile birlikte kaldırıldı
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mahalleustasi.data.model.Offer
import com.example.mahalleustasi.ui.viewmodel.OffersViewModel
import com.example.mahalleustasi.ui.viewmodel.UsersViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date

// ÖNEMLİ NOT:
// Bu tasarımın tam olarak çalışması için `Offer` veri modelinizin
// teklif verilen işin başlığı gibi denormalize edilmiş verileri içermesi gerekir.
// Örneğin:
// data class Offer(
//     ...
//     val jobTitle: String? = null,
//     val jobCategory: String? = null
// )
// Eğer bu veriler yoksa, kartta `offer.jobId` göstermek zorunda kalırsınız,
// bu da kullanıcı dostu olmaz.


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffersScreen(
    navController: NavController,
    viewModel: OffersViewModel,
    usersVm: UsersViewModel = hiltViewModel() // Bu artık gerekmeyebilir
) {
    val offers by viewModel.offers.collectAsState()
    val loading by viewModel.loading.collectAsState()
    // val userMap by usersVm.userMap.collectAsState() // Artık kendi adımızı göstermiyoruz

    // Sadece bir kez yükle
    LaunchedEffect(Unit) {
        viewModel.loadMyOffers()
    }

    // Artık kendi kullanıcı verimizi çekmemize gerek yok.
    // LaunchedEffect(offers) { usersVm.ensureUsers(offers.mapNotNull { it.proId }) }

    // Pull-to-Refresh kullanmıyoruz; üstteki Yenile butonunu kullanın

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tekliflerim", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                // 1. Durum: İlk Yükleme
                loading && offers.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // 2. Durum: Boş Liste
                !loading && offers.isEmpty() -> {
                    EmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        message = "Henüz gönderdiğiniz bir teklif bulunmuyor."
                    )
                }

                // 3. Durum: Dolu Liste
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(offers, key = { it.id }) { offer ->
                            OfferItemCard(
                                offer = offer,
                                onCardClick = {
                                    navController.navigate("job_detail/${offer.jobId}")
                                },
                                onWithdrawClick = {
                                    viewModel.withdraw(offer.id, offer.jobId)
                                }
                            )
                        }
                    }
                }
            }

            // Yenile butonu
            OutlinedButton(
                onClick = { viewModel.loadMyOffers() },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Text("Yenile")
            }
        }
    }
}

/**
 * Tek bir teklif kartını gösteren modern Composable.
 */
@Composable
fun OfferItemCard(
    offer: Offer,
    onCardClick: () -> Unit,
    onWithdrawClick: () -> Unit
) {
    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Üst Satır: Kategori ve Durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Kategori (Offer modelinizde olduğunu varsayıyoruz)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        // Not: Denormalize alan (offer.jobCategory) yoksa güvenli fallback
                        text = "Kategori Yok",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                // Durum
                StatusChip(status = offer.status)
            }

            // Orta Alan: İş Başlığı ve Not
            Column {
                Text(
                    // Not: İş ID gösterilmez; denormalize başlık yoksa sabit başlık kullan
                    text = "Teklif",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                offer.note?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = "Notunuz: \"$it\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))

            // Alt Satır: Tutar, Tarih ve Eylem
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tutar
                val amountText = NumberFormat.getNumberInstance().format(offer.amount ?: 0.0) + " TL"
                Column {
                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    // Oluşturulma tarihi (opsiyonel)
                    val created = SimpleDateFormat("dd.MM.yyyy HH:mm").format(Date(offer.createdAt))
                    Text(
                        text = created,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // "Geri Çek" Butonu (sadece beklemedeyse)
                if (offer.status == "pending") {
                    Button(
                        onClick = onWithdrawClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text("Geri Çek")
                    }
                }
            }
        }
    }
}

/**
 * Teklifin durumunu gösteren renkli bir çip.
 */
@Composable
fun StatusChip(status: String) {
    val (text, containerColor, contentColor) = when (status) {
        "pending" -> Triple(
            "Beklemede",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        "accepted" -> Triple(
            "Kabul Edildi",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        "rejected" -> Triple(
            "Reddedildi",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        else -> Triple(
            status.uppercase(),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Liste boş olduğunda gösterilecek Composable.
 */
@Composable
fun EmptyState(modifier: Modifier = Modifier, message: String) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = "Boş Kutu",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}