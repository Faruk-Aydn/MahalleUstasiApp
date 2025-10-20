package com.example.mahalleustasi.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Geri oku ikonu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar // Modern üst çubuk
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.data.model.Offer
import com.example.mahalleustasi.data.model.User // Bu modelin sizde olduğunu varsayıyoruz
import com.example.mahalleustasi.ui.viewmodel.JobDetailViewModel
import com.example.mahalleustasi.ui.viewmodel.OffersViewModel
import com.example.mahalleustasi.ui.viewmodel.UsersViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Ana Ekran Fonksiyonu
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar için gerekli
@Composable
fun JobDetailScreen(
    navController: NavController,
    jobId: String,
    offersViewModel: OffersViewModel,
    jobDetailViewModel: JobDetailViewModel = hiltViewModel(),
    usersVm: UsersViewModel = hiltViewModel()
) {
    // State Değişkenleri
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var showOfferDialog by remember { mutableStateOf(false) }
    var showOffersListDialog by remember { mutableStateOf(false) }

    // ViewModel'lerden veri çekme
    val job by jobDetailViewModel.job.collectAsState()
    val offers by offersViewModel.offers.collectAsState()
    val userMap by usersVm.userMap.collectAsState()
    val currentUid = Firebase.auth.currentUser?.uid

    // Veri Yükleme Efektleri
    LaunchedEffect(jobId) {
        offersViewModel.loadForJob(jobId)
        jobDetailViewModel.load(jobId)
    }

    LaunchedEffect(offers) {
        usersVm.ensureUsers(offers.mapNotNull { it.proId })
    }

    LaunchedEffect(job) {
        val idsToLoad = mutableListOf<String>()
        job?.assignedProId?.let { idsToLoad.add(it) }
        job?.ownerId?.let { idsToLoad.add(it) }
        if (idsToLoad.isNotEmpty()) {
            usersVm.ensureUsers(idsToLoad)
        }
    }

    val jobOwner = job?.ownerId?.let { userMap[it] }
    val isOwner = job?.ownerId == currentUid
    val myOffer = offers.firstOrNull { it.proId == currentUid }

    // Ekranın ana yapısını (Scaffold) oluşturma
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // YENİ: Modern üst çubuk (geri oku + başlık)
            JobDetailTopBar(
                title = job?.title ?: "İş Detayı",
                onBackClick = { navController.navigateUp() }
            )
        },
        bottomBar = {
            // YENİLENDİ: Daha şık ve bilgilendirici alt eylem çubuğu
            JobDetailBottomBar(
                job = job,
                isOwner = isOwner,
                myOffer = myOffer,
                onMakeOfferClick = { showOfferDialog = true },
                onWithdrawOfferClick = {
                    myOffer?.let { offersViewModel.withdraw(it.id, jobId) }
                },
                onMessageClick = {
                    val ownerId = job?.ownerId
                    val assignedProId = job?.assignedProId
                    val me = currentUid
                    // chatId: job bazlı ve iki kullanıcı deterministik
                    fun buildChatId(u1: String, u2: String): String {
                        val a = if (u1 <= u2) u1 else u2
                        val b = if (u1 <= u2) u2 else u1
                        return "job_${jobId}_${a}_${b}"
                    }
                    val targetChatId = when {
                        // Atama yapıldıysa: iş sahibi ile atanan usta konuşur
                        assignedProId != null && ownerId != null -> buildChatId(ownerId, assignedProId)
                        // Usta kendi teklifi üzerinden iş sahibiyle yazışmak isterse
                        !isOwner && ownerId != null && me != null -> buildChatId(ownerId, me)
                        else -> null
                    }
                    targetChatId?.let { navController.navigate("chat/$it") }
                },
                onShowOffersClick = { showOffersListDialog = true }
            )
        }
    ) { innerPadding ->
        // Yükleniyor durumu
        if (job == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Kaydırılabilir içerik alanı
            JobDetailContent(
                modifier = Modifier.padding(innerPadding), // Üst ve alt çubuktan gelen padding'i uygula
                job = job!!,
                jobOwner = jobOwner,
                onPhotoClick = { url -> previewUrl = url }
            )
        }
    }

    // Fotoğraf Önizleme Dialog'u
    previewUrl?.let { url ->
        PhotoPreviewDialog(
            url = url,
            onDismiss = { previewUrl = null }
        )
    }

    // Teklif Verme Dialog'u
    if (showOfferDialog) {
        MakeOfferDialog(
            amount = amount,
            onAmountChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
            note = note,
            onNoteChange = { note = it },
            error = error,
            isSubmitting = isSubmitting,
            onDismiss = { showOfferDialog = false },
            onSubmit = {
                error = null
                val amt = amount.toDoubleOrNull()
                if (amt == null || amt <= 0.0) {
                    error = "Geçerli bir teklif tutarı giriniz."
                } else {
                    isSubmitting = true
                    val offer = Offer(jobId = jobId, amount = amt, note = note.ifBlank { null })
                    offersViewModel.submit(offer,
                        onCreated = {
                            amount = ""
                            note = ""
                            offersViewModel.loadForJob(jobId) // Teklif listesini yenile
                            isSubmitting = false
                            showOfferDialog = false
                        },
                        onError = { t ->
                            error = t.message
                            isSubmitting = false
                        }
                    )
                }
            }
        )
    }

    // İş sahibine özel: Teklifleri görüntüleme dialog'u
    if (isOwner && showOffersListDialog) {
        OffersListDialog(
            offers = offers,
            usersVm = usersVm,
            onAccept = { offerId ->
                offersViewModel.accept(offerId, jobId)
                showOffersListDialog = false
            },
            onReject = { offerId ->
                offersViewModel.reject(offerId, jobId)
                showOffersListDialog = false
            },
            onMessage = { proId ->
                val ownerId = job?.ownerId ?: return@OffersListDialog
                val (a, b) = if (ownerId <= proId) ownerId to proId else proId to ownerId
                val chatId = "job_${jobId}_${a}_${b}"
                navController.navigate("chat/$chatId")
            },
            onDismiss = { showOffersListDialog = false }
        )
    }
}

@Composable
fun OffersListDialog(
    offers: List<Offer>,
    usersVm: UsersViewModel,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Teklifler",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (offers.isEmpty()) {
                    Text(
                        text = "Bu iş için henüz teklif yok.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(offers) { offer ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val proName = usersVm.displayName(offer.proId) ?: offer.proId
                                Text(text = proName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(text = "Tutar: ${offer.amount ?: 0.0} TL", style = MaterialTheme.typography.bodyMedium)
                                offer.note?.let { Text(text = "Not: $it", style = MaterialTheme.typography.bodySmall) }
                                Text(
                                    text = "Durum: ${offer.status}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (offer.status == "pending") {
                                        Button(onClick = { onAccept(offer.id) }) { Text("Kabul Et") }
                                        OutlinedButton(onClick = { onReject(offer.id) }) { Text("Reddet") }
                                    }
                                    OutlinedButton(onClick = { onMessage(offer.proId!!) }) { Text("Mesajlaş") }
                                }
                            }
                            Divider()
                        }
                    }
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Kapat") }
            }
        }
    }
}

// YENİ: Üst Çubuk (TopAppBar)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailTopBar(title: String, onBackClick: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri"
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}


// Kaydırılabilir ana içeriği barındıran fonksiyon
@Composable
fun JobDetailContent(
    modifier: Modifier = Modifier,
    job: Job,
    jobOwner: User?,
    onPhotoClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp), // Alt çubukla çakışmaması için
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. YENİ: Kahraman (Hero) Bölümü
        item {
            JobHeroSection(job = job)
        }

        // 2. Ana Bilgi Bölümü (Başlık, Sahip, Çipler)
        item {
            JobMainInfoSection(
                job = job,
                jobOwner = jobOwner
            )
        }

        item { Divider(modifier = Modifier.padding(horizontal = 16.dp)) }

        // 3. Açıklama Bölümü
        job.description?.takeIf { it.isNotBlank() }?.let { desc ->
            item {
                JobDescriptionSection(description = desc)
            }
        }

        // 4. Konum Bölümü (Harita görseli olmadan)
        job.location?.let { loc ->
            item {
                JobLocationSection(
                    address = loc.address,
                    lat = loc.lat,
                    lng = loc.lng
                )
            }
        }

        item { Divider(modifier = Modifier.padding(horizontal = 16.dp)) }


        // 5. Fotoğraflar Bölümü
        val photos = job.photoUrls ?: emptyList()
        if (photos.isNotEmpty()) {
            item {
                JobPhotosSection(
                    photos = photos,
                    onPhotoClick = onPhotoClick
                )
            }
        }
    }
}

// YENİ: Kahraman (Hero) Bölümü
@Composable
fun JobHeroSection(job: Job) {
    // Varsa ilk fotoğrafı, yoksa haritayı, o da yoksa bir yedek alanı göster
    val heroUrl = job.photoUrls?.firstOrNull() ?: job.location?.let {
        "https://staticmap.openstreetmap.de/staticmap.php?center=${it.lat},${it.lng}&zoom=15&size=600x400&markers=${it.lat},${it.lng},lightblue1"
    }

    if (heroUrl != null) {
        AsyncImage(
            model = heroUrl,
            contentDescription = "İş Görseli",
            contentScale = ContentScale.Crop, // Görüntüyü yay
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp) // Belirgin bir yükseklik
        )
    } else {
        // Hiç görsel yoksa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("Görsel Yok", color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

// YENİ: Ana Bilgileri Gruplayan Bölüm
@Composable
fun JobMainInfoSection(job: Job, jobOwner: User?) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp), // İçeriden padding
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Kategori
        job.category?.let {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        // Başlık
        Text(
            text = job.title,
            style = MaterialTheme.typography.headlineSmall, // Biraz daha küçük ama güçlü
            fontWeight = FontWeight.Bold
        )

        // İlan Sahibi
        val ownerName = jobOwner?.name ?: job.ownerName ?: "Bilinmeyen Kullanıcı"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = jobOwner?.photoUrl,
                contentDescription = "Sahip Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Text(
                text = ownerName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Bilgi Çipleri
        JobInfoChips(
            status = job.status,
            price = job.price,
            isCash = job.isCash
        )

        // İlan Tarihi
        val created = job.createdAt?.let {
            SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr")).format(Date(it))
        }
        if (!created.isNullOrBlank()) {
            Text(
                text = "İlan Tarihi: $created",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


// Bilgi Çipleri (Değişiklik yok, öncekiyle aynı)
@Composable
fun JobInfoChips(status: String, price: Double?, isCash: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = { },
            label = { Text("Durum: ${status.replaceFirstChar { it.titlecase() }}") }
        )
        val priceText = price?.let { "${it} TL" } ?: "Belirtilmemiş"
        AssistChip(
            onClick = { },
            label = { Text("Fiyat: $priceText") }
        )
        val payText = if (isCash) "Nakit" else "Kart"
        AssistChip(
            onClick = { },
            label = { Text("Ödeme: $payText") }
        )
    }
}

// Açıklama (Padding dışarıdan verilecek şekilde düzenlendi)
@Composable
fun JobDescriptionSection(description: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "İş Açıklaması",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp
        )
    }
}

// Konum (YENİLENDİ: Harita görseli kaldırıldı)
@Composable
fun JobLocationSection(address: String?, lat: Double, lng: Double) {
    val ctx = LocalContext.current

    fun openMaps() {
        val geoUri = Uri.parse("geo:${lat},${lng}?q=${lat},${lng}(${Uri.encode(address ?: "İş Konumu")})")
        val intent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        ctx.startActivity(intent)
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Konum",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = address ?: "Konum bilgisi yok",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        // Haritada Aç Butonu
        FilledTonalButton(
            onClick = { openMaps() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Haritada Aç")
        }
    }
}

// Fotoğraflar (Padding dışarıdan verilecek şekilde düzenlendi)
@Composable
fun JobPhotosSection(photos: List<String>, onPhotoClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(), // LazyRow'un tam genişlik alması için
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Fotoğraflar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp) // Başlığa padding
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp) // Fotoğraf listesine padding
        ) {
            items(photos) { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "İş Fotoğrafı",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPhotoClick(url) }
                )
            }
        }
    }
}

// YENİLENDİ: Alt Eylem Çubuğu (Bottom CTA Bar)
@Composable
fun JobDetailBottomBar(
    job: Job?,
    isOwner: Boolean,
    myOffer: Offer?,
    onMakeOfferClick: () -> Unit,
    onWithdrawOfferClick: () -> Unit,
    onMessageClick: () -> Unit,
    onShowOffersClick: () -> Unit
) {
    if (job == null || job.status != "open") {
        // İş kapalıysa veya yükleniyorsa sade bir çubuk göster
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Text(
                text = if (job == null) "Yükleniyor..." else "Bu iş artık 'açık' durumda değil.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    // Gerçek BottomAppBar'ı kullan
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface, // Veya surfaceContainer
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when {
                // 1. İlan Sahibi
                isOwner -> {
                    // İlan sahibi: atama varsa mesajlaşma aç
                    if (job.assignedProId != null) {
                        Button(onClick = onMessageClick, modifier = Modifier.fillMaxWidth()) { Text("Mesajlaş") }
                    } else {
                        Button(onClick = onShowOffersClick, modifier = Modifier.fillMaxWidth()) { Text("Teklifleri Görüntüle") }
                    }
                }

                // 2. Kullanıcının Teklifi Var
                myOffer != null -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "Teklifin (${myOffer.status}):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${myOffer.amount} TL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (myOffer.status == "pending") {
                            OutlinedButton(onClick = onWithdrawOfferClick) { Text("Geri Çek") }
                        }
                        // Usta, teklif aşamasındayken de iş sahibiyle yazışabilsin
                        Button(onClick = onMessageClick) { Text("Mesajlaş") }
                    }
                }

                // 3. Kullanıcı Teklif Verebilir (En "WOW" durum)
                else -> {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "İş Fiyatı:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val priceText = job.price?.let { "$it TL" } ?: "Belirtilmemiş"
                        Text(
                            priceText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(onClick = onMakeOfferClick) { Text("Teklif Ver") }
                }
            }
        }
    }
}


// --- DIALOG COMPOSABLES (Değişiklik yok, öncekiyle aynı) ---

@Composable
fun PhotoPreviewDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AsyncImage(
                    model = url,
                    contentDescription = "Fotoğraf Önizleme",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                )
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Kapat")
                }
            }
        }
    }
}

@Composable
fun MakeOfferDialog(
    amount: String,
    onAmountChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    error: String?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Teklifini Gönder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Teklif Tutarı (TL)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = { Text("Not (Opsiyonel)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("İptal")
                    }
                    Button(
                        onClick = onSubmit,
                        enabled = !isSubmitting,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Gönder")
                        }
                    }
                }
            }
        }
    }
}