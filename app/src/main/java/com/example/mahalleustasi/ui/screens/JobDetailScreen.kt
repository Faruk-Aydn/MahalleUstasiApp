package com.example.mahalleustasi.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.data.model.Offer
import com.example.mahalleustasi.data.model.Review
import com.example.mahalleustasi.data.model.User
import com.example.mahalleustasi.ui.theme.* // <-- DİKKAT: Tema dosyamızı import ettik!
import com.example.mahalleustasi.ui.viewmodel.JobDetailViewModel
import com.example.mahalleustasi.ui.viewmodel.OffersViewModel
import com.example.mahalleustasi.ui.viewmodel.ReviewsViewModel
import com.example.mahalleustasi.ui.viewmodel.UsersViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun JobDetailScreen(
    navController: NavController,
    jobId: String,
    offersViewModel: OffersViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    jobDetailViewModel: JobDetailViewModel = hiltViewModel(),
    usersVm: UsersViewModel = hiltViewModel(),
    reviewsVm: ReviewsViewModel = hiltViewModel()
) {
    // --- State Tanımları ---
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var previewUrl by remember { mutableStateOf<String?>(null) }

    var showOfferDialog by remember { mutableStateOf(false) }
    var showOffersListDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    var reviewRating by remember { mutableStateOf(0) }
    var reviewComment by remember { mutableStateOf("") }
    var reviewTargetId by remember { mutableStateOf<String?>(null) }
    var checkingReview by remember { mutableStateOf(false) }
    var lastCheckedJobStatus by remember { mutableStateOf<String?>(null) }

    val job by jobDetailViewModel.job.collectAsState()
    val offers by offersViewModel.offers.collectAsState()
    val userMap by usersVm.userMap.collectAsState()
    val updating by jobDetailViewModel.updating.collectAsState()
    val toastMessage by jobDetailViewModel.toast.collectAsState()

    val currentUid = Firebase.auth.currentUser?.uid
    val ctx = LocalContext.current

    // --- Side Effects ---
    LaunchedEffect(jobId) {
        offersViewModel.loadForJob(jobId)
        jobDetailViewModel.observe(jobId)
    }

    LaunchedEffect(offers) {
        usersVm.ensureUsers(offers.mapNotNull { it.proId })
    }

    LaunchedEffect(job) {
        val idsToLoad = mutableListOf<String>()
        job?.assignedProId?.let { idsToLoad.add(it) }
        job?.ownerId?.let { idsToLoad.add(it) }
        if (idsToLoad.isNotEmpty()) usersVm.ensureUsers(idsToLoad)

        job?.let { currentJob ->
            if (currentJob.status == "completed" &&
                currentJob.status != lastCheckedJobStatus &&
                currentUid != null && !checkingReview && !showReviewDialog
            ) {

                lastCheckedJobStatus = currentJob.status
                checkingReview = true
                val isOwner = currentJob.ownerId == currentUid
                val isAssignedPro = currentJob.assignedProId == currentUid

                if (isOwner || isAssignedPro) {
                    val revieweeId = if (isOwner) currentJob.assignedProId else currentJob.ownerId
                    if (revieweeId != null) {
                        reviewsVm.hasReviewed(jobId, currentUid, revieweeId) { hasReviewed ->
                            checkingReview = false
                            if (!hasReviewed) {
                                reviewTargetId = revieweeId
                                showReviewDialog = true
                            }
                        }
                    } else checkingReview = false
                } else checkingReview = false
            } else if (currentJob.status != "completed") {
                lastCheckedJobStatus = null
            }
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
            jobDetailViewModel.clearToast()
        }
    }

    val jobOwner = job?.ownerId?.let { userMap[it] }
    val isOwner = job?.ownerId == currentUid
    val isAssignedPro = job?.assignedProId == currentUid
    val myOffer = offers.firstOrNull { it.proId == currentUid }

    // --- UI Structure ---
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // Tema Rengi
        topBar = {
            ModernTopNav(onBackClick = { navController.navigateUp() })
        },
        bottomBar = {
            JobActionBottomBar(
                job = job,
                isOwner = isOwner,
                isAssignedPro = isAssignedPro,
                updating = updating,
                myOffer = myOffer,
                onMakeOfferClick = { showOfferDialog = true },
                onWithdrawOfferClick = { myOffer?.let { offersViewModel.withdraw(it.id, jobId) } },
                onMessageClick = {
                    val ownerId = job?.ownerId
                    val assignedProId = job?.assignedProId
                    val me = currentUid
                    fun buildChatId(u1: String, u2: String): String {
                        val a = if (u1 <= u2) u1 else u2
                        val b = if (u1 <= u2) u2 else u1
                        return "job_${jobId}_${a}_${b}"
                    }
                    val targetChatId = when {
                        assignedProId != null && ownerId != null -> buildChatId(ownerId, assignedProId)
                        !isOwner && ownerId != null && me != null -> buildChatId(ownerId, me)
                        else -> null
                    }
                    targetChatId?.let { navController.navigate("chat/$it") }
                },
                onShowOffersClick = { showOffersListDialog = true },
                onMarkDoneClick = { jobDetailViewModel.markAwaitingConfirmation(jobId) },
                onOwnerApproveClick = { jobDetailViewModel.markCompleted(jobId) },
                onDisputeClick = { jobDetailViewModel.markDisputed(jobId) },
                onReviewClick = {
                    job?.let { currentJob ->
                        if (currentJob.status == "completed" && currentUid != null) {
                            val isOwnerRole = currentJob.ownerId == currentUid
                            val isAssignedRole = currentJob.assignedProId == currentUid
                            if (isOwnerRole || isAssignedRole) {
                                val revieweeId = if (isOwnerRole) currentJob.assignedProId else currentJob.ownerId
                                if (revieweeId != null) {
                                    checkingReview = true
                                    reviewsVm.hasReviewed(jobId, currentUid, revieweeId) { hasReviewed ->
                                        checkingReview = false
                                        if (!hasReviewed) {
                                            reviewTargetId = revieweeId
                                            showReviewDialog = true
                                        } else {
                                            Toast.makeText(ctx, "Bu işi zaten puanladınız", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (job == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                ModernJobContent(
                    job = job!!,
                    jobOwner = jobOwner,
                    onPhotoClick = { url -> previewUrl = url },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    jobId = jobId,
                    onOwnerClick = {
                        job!!.ownerId?.let { ownerId ->
                            navController.navigate("public_profile/$ownerId")
                        }
                    }
                )
            }
        }
    }

    // --- DIALOGS ---
    if (previewUrl != null) {
        ModernPhotoPreviewDialog(url = previewUrl!!, onDismiss = { previewUrl = null })
    }

    if (showOfferDialog) {
        ModernMakeOfferDialog(
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
                            amount = ""; note = ""; isSubmitting = false; showOfferDialog = false
                            offersViewModel.loadForJob(jobId)
                        },
                        onError = { t -> error = t.message; isSubmitting = false }
                    )
                }
            }
        )
    }

    if (isOwner && showOffersListDialog) {
        ModernOffersListDialog(
            offers = offers,
            usersVm = usersVm,
            onAccept = { offersViewModel.accept(it, jobId); showOffersListDialog = false },
            onReject = { offersViewModel.reject(it, jobId); showOffersListDialog = false },
            onMessage = { proId ->
                val ownerId = job?.ownerId ?: return@ModernOffersListDialog
                val (a, b) = if (ownerId <= proId) ownerId to proId else proId to ownerId
                navController.navigate("chat/job_${jobId}_${a}_${b}")
            },
            onDismiss = { showOffersListDialog = false }
        )
    }

    if (showReviewDialog && job != null && reviewTargetId != null && currentUid != null) {
        val loading by reviewsVm.loading.collectAsState()
        val revieweeName = reviewTargetId?.let { userMap[it]?.name ?: "Kullanıcı" } ?: "Kullanıcı"

        ModernReviewDialog(
            rating = reviewRating,
            onRatingChange = { reviewRating = it },
            comment = reviewComment,
            onCommentChange = { reviewComment = it },
            loading = loading,
            onDismiss = { showReviewDialog = false; reviewTargetId = null },
            onSubmit = {
                if (reviewRating in 1..5) {
                    val rev = Review(
                        jobId = jobId,
                        reviewerId = currentUid,
                        revieweeId = reviewTargetId!!,
                        rating = reviewRating,
                        comment = reviewComment.ifBlank { null }
                    )
                    reviewsVm.submit(rev) {
                        showReviewDialog = false; reviewTargetId = null; jobDetailViewModel.load(jobId)
                    }
                }
            },
            revieweeName = revieweeName
        )
    }
}

// --------------------------- MODERN GÖRSEL BİLEŞENLER --------------------------- //

@Composable
fun ModernTopNav(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .zIndex(1f)
    ) {
        Surface(
            onClick = onBackClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = MahalleTextPrimary // Color.kt'den
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ModernJobContent(
    job: Job,
    jobOwner: User?,
    onPhotoClick: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    jobId: String,
    onOwnerClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        // --- ÜST GÖRSEL ALANI ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
        ) {
            with(sharedTransitionScope) {
                val heroUrl = job.photoUrls?.firstOrNull()
                if (heroUrl != null) {
                    AsyncImage(
                        model = heroUrl,
                        contentDescription = "Job Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedElement(
                                rememberSharedContentState(key = "card-$jobId"),
                                animatedVisibilityScope = animatedContentScope,
                            ),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary) // Tema Rengi
                    ) {
                        Icon(
                            Icons.Default.Category, contentDescription = null,
                            tint = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(120.dp).align(Alignment.Center)
                        )
                    }
                }
            }

            // Geliştirilmiş Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.8f)
                            ),
                            startY = 100f
                        )
                    )
            )

            // Başlık ve Fiyat Bilgisi
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 24.dp, vertical = 40.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary, // Tema Rengi
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = job.category ?: "Genel",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = job.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- İÇERİK KARTI ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-24).dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background, // Tema Rengi
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Fiyat ve Durum Satırı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${job.price?.toInt() ?: 0} ₺",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary // Tema Rengi
                    )
                    ModernStatusChip(status = job.status)
                }

                // İlan Sahibi Kartı
                ModernOwnerCard(jobOwner = jobOwner, job = job, onClick = onOwnerClick)

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 1.dp)

                // İstatistikler
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernInfoCard(
                        icon = Icons.Outlined.CalendarToday,
                        title = "Tarih",
                        value = job.createdAt?.let { SimpleDateFormat("dd MMM", Locale("tr")).format(Date(it)) } ?: "Bugün",
                        modifier = Modifier.weight(1f)
                    )
                    ModernInfoCard(
                        icon = Icons.Outlined.Payments,
                        title = "Ödeme",
                        value = if (job.isCash) "Nakit" else "Kart",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Açıklama
                Column {
                    SectionHeader(title = "İş Açıklaması")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = job.description.ifBlank { "Açıklama belirtilmemiş." },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MahalleTextSecondary, // Tema Rengi
                        lineHeight = 24.sp
                    )
                }

                // Konum
                if (job.location != null) {
                    SectionHeader(title = "Konum")
                    ModernLocationCard(address = job.location.address, lat = job.location.lat, lng = job.location.lng)
                }

                // Fotoğraflar
                if (!job.photoUrls.isNullOrEmpty()) {
                    SectionHeader(title = "Fotoğraflar")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(job.photoUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onPhotoClick(url) }
                                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MahalleTextPrimary // Tema Rengi
    )
}

@Composable
fun ModernOwnerCard(jobOwner: User?, job: Job, onClick: () -> Unit) {
    val name = jobOwner?.name ?: job.ownerName ?: "Mahalle Sakini"
    val image = jobOwner?.photoUrl

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (image != null) {
                    AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MahalleTextPrimary)
                Text("İlan Sahibi", style = MaterialTheme.typography.bodySmall, color = MahalleTextSecondary)
            }

            Spacer(Modifier.weight(1f))

            IconButton(onClick = { /* Opsiyonel profil detayı */ }) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MahalleTextSecondary)
            }
        }
    }
}

@Composable
fun ModernInfoCard(icon: ImageVector, title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MahalleTextSecondary)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MahalleTextPrimary)
        }
    }
}

@Composable
fun ModernLocationCard(address: String?, lat: Double, lng: Double) {
    val ctx = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().clickable {
            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(address ?: "Konum")})")
            ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = address ?: "Konum bilgisi mevcut değil",
                style = MaterialTheme.typography.bodyMedium,
                color = MahalleTextPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun ModernStatusChip(status: String) {
    // Merkezi Color.kt renklerini kullanıyoruz
    val (bgColor, txtColor, label) = when (status.lowercase()) {
        "pending", "bekliyor" -> Triple(StatusPendingBg, StatusPendingText, "Onay Bekliyor")
        "active", "aktif", "open" -> Triple(StatusActiveBg, StatusActiveText, "Aktif")
        "assigned", "atandı" -> Triple(StatusActiveBg, MaterialTheme.colorScheme.primary, "Usta Atandı") // assigned için de teal
        "completed", "tamamlandı" -> Triple(StatusCompletedBg, StatusCompletedText, "Tamamlandı")
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), status.replaceFirstChar { it.uppercase() })
    }

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = txtColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun JobActionBottomBar(
    job: Job?,
    isOwner: Boolean,
    isAssignedPro: Boolean,
    updating: Boolean,
    myOffer: Offer?,
    onMakeOfferClick: () -> Unit,
    onWithdrawOfferClick: () -> Unit,
    onMessageClick: () -> Unit,
    onShowOffersClick: () -> Unit,
    onMarkDoneClick: () -> Unit,
    onOwnerApproveClick: () -> Unit,
    onDisputeClick: () -> Unit,
    onReviewClick: () -> Unit
) {
    if (job == null) return

    Surface(
        modifier = Modifier.fillMaxWidth().shadow(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            when {
                isOwner -> {
                    when (job.status) {
                        "open" -> {
                            Text(
                                text = "Bu ilana gelen teklifleri inceleyebilir ve uygun ustayı seçebilirsin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MahalleTextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ModernFullButton(text = "Teklifleri İncele", onClick = onShowOffersClick, color = MaterialTheme.colorScheme.primary)
                        }
                        "assigned", "in_progress" -> {
                            Text(
                                text = "İşin detaylarını netleştirmek için usta ile mesajlaşabilirsin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MahalleTextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ModernFullButton(text = "Usta ile Mesajlaş", onClick = onMessageClick, color = MahalleOrange, icon = Icons.Outlined.Chat)
                        }
                        "awaiting_confirmation" -> {
                            Text(
                                text = "Usta işi tamamladı. Her şey yolundaysa onaylayabilir veya sorun bildirebilirsin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MahalleTextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernExpandedButton(text = "İşi Onayla", onClick = onOwnerApproveClick, color = MaterialTheme.colorScheme.primary, enabled = !updating)
                                ModernExpandedButton(text = "Sorun Bildir", onClick = onDisputeClick, color = MaterialTheme.colorScheme.error, enabled = !updating)
                            }
                        }
                        "completed" -> {
                            Text(
                                text = "İş tamamlandı. Ustayı puanlayabilir veya tekrar mesajlaşabilirsin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MahalleTextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernExpandedButton(text = "Puan Ver", onClick = onReviewClick, color = MahalleOrange) // Yıldız rengi
                                ModernExpandedButton(text = "Mesaj", onClick = onMessageClick, color = Color.Gray, outline = true)
                            }
                        }
                        else -> Text("İşlem Durumu: ${job.status}", color = MahalleTextSecondary)
                    }
                }
                isAssignedPro -> {
                    when (job.status) {
                        "assigned", "in_progress" -> {
                            Text(
                                text = "İşi bitirdiğinde onaya göndermek için 'İşi Tamamla' butonunu kullan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MahalleTextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernExpandedButton(text = "İşi Tamamla", onClick = onMarkDoneClick, color = MaterialTheme.colorScheme.primary, enabled = !updating)
                                ModernExpandedButton(text = "Mesaj", onClick = onMessageClick, color = Color.Gray, outline = true)
                            }
                        }
                        "awaiting_confirmation" -> {
                            Text(
                                text = "İşverenin onayı bekleniyor. Bu süre içinde iş detayları için mesajlaşabilirsin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ModernFullButton(text = "Onay Bekleniyor...", onClick = {}, color = Color.Gray, enabled = false)
                        }
                        "completed" -> {
                            Text(
                                text = "İş tamamlandı. İşvereni puanlayabilir veya tekrar mesajlaşabilirsin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MahalleTextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ModernExpandedButton(text = "Puan Ver", onClick = onReviewClick, color = MahalleOrange)
                                ModernExpandedButton(text = "Mesaj", onClick = onMessageClick, color = Color.Gray, outline = true)
                            }
                        }
                    }
                }
                myOffer != null && job.status == "open" -> {
                    Text(
                        text = "Bu işe zaten bir teklif verdin. Gerekirse teklifi geri çekebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MahalleTextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernExpandedButton(text = "Teklifi Geri Çek", onClick = onWithdrawOfferClick, color = MaterialTheme.colorScheme.error)
                        ModernExpandedButton(text = "Mesaj", onClick = onMessageClick, color = MaterialTheme.colorScheme.primary)
                    }
                }
                job.status == "open" -> {
                    Text(
                        text = "Bu işe teklif vererek iş sahibiyle iletişime geçebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MahalleTextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // Teklif ver butonu "Secondary" (Turuncu) olsun ki dikkat çeksin
                    ModernFullButton(
                        text = "Bu İşe Teklif Ver",
                        onClick = onMakeOfferClick,
                        color = MaterialTheme.colorScheme.secondary,
                        icon = Icons.Outlined.LocalOffer
                    )
                }
            }
        }
    }
}

@Composable
fun ModernFullButton(text: String, onClick: () -> Unit, color: Color, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = if(color == MahalleOrange) Color.Black else Color.White // Turuncu üzerindeki yazı okunurluğu için
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        enabled = enabled
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun RowScope.ModernExpandedButton(text: String, onClick: () -> Unit, color: Color, enabled: Boolean = true, outline: Boolean = false) {
    if (outline) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, color),
            enabled = enabled
        ) {
            Text(text, fontWeight = FontWeight.Bold, color = color)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier.weight(1f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = if(color == MahalleOrange) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
            enabled = enabled
        ) {
            Text(text, fontWeight = FontWeight.Bold)
        }
    }
}

// --------------------------- DIALOGS (Yenilenmiş) --------------------------- //

@Composable
fun ModernPhotoPreviewDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ModernMakeOfferDialog(
    amount: String, onAmountChange: (String) -> Unit,
    note: String, onNoteChange: (String) -> Unit,
    error: String?, isSubmitting: Boolean,
    onDismiss: () -> Unit, onSubmit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Teklifini İlet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MahalleTextPrimary)
                Text("Fiyat ve açıklamanı girerek müşteriye ulaş.", style = MaterialTheme.typography.bodySmall, color = MahalleTextSecondary)

                if(error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(
                    value = amount, onValueChange = onAmountChange,
                    label = { Text("Tutar (₺)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                OutlinedTextField(
                    value = note, onValueChange = onNoteChange,
                    label = { Text("Müşteriye Notun") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernExpandedButton(text = "Vazgeç", onClick = onDismiss, color = MahalleTextSecondary, outline = true)
                    ModernExpandedButton(text = "Teklifi Gönder", onClick = onSubmit, color = MaterialTheme.colorScheme.primary, enabled = !isSubmitting)
                }
            }
        }
    }
}

@Composable
fun ModernOffersListDialog(
    offers: List<Offer>, usersVm: UsersViewModel,
    onAccept: (String) -> Unit, onReject: (String) -> Unit, onMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Gelen Teklifler", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MahalleTextPrimary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = MahalleTextPrimary) }
                }
                Spacer(Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (offers.isEmpty()) item { Text("Henüz teklif gelmedi.", color = MahalleTextSecondary, modifier = Modifier.padding(8.dp)) }
                    items(offers) { offer ->
                        val name = usersVm.displayName(offer.proId) ?: "Usta"
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.Gray.copy(0.1f))) {
                            Column(Modifier.padding(16.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(name, fontWeight = FontWeight.Bold, color = MahalleTextPrimary)
                                    Text("${offer.amount?.toInt()} ₺", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                                if(!offer.note.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(offer.note, style = MaterialTheme.typography.bodySmall, color = MahalleTextSecondary)
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if(offer.status == "pending") {
                                        Button(onClick = { onAccept(offer.id) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Kabul Et", fontSize = 12.sp) }
                                        Button(onClick = { onReject(offer.id) }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.error), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.3f)), shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("Reddet", fontSize = 12.sp) }
                                    }
                                    IconButton(onClick = { onMessage(offer.proId!!) }, modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape).size(40.dp)) { Icon(Icons.Outlined.Chat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernReviewDialog(
    rating: Int, onRatingChange: (Int) -> Unit,
    comment: String, onCommentChange: (String) -> Unit,
    loading: Boolean, onDismiss: () -> Unit, onSubmit: () -> Unit, revieweeName: String
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Hizmeti Değerlendir", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MahalleTextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("$revieweeName ile çalışmak nasıldı?", style = MaterialTheme.typography.bodyMedium, color = MahalleTextSecondary)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.Center) {
                    (1..5).forEach { i ->
                        Icon(
                            imageVector = if (i <= rating) Icons.Rounded.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (i <= rating) MahalleOrange else Color.Gray.copy(0.3f),
                            modifier = Modifier.size(44.dp).clickable { onRatingChange(i) }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = comment, onValueChange = onCommentChange,
                    label = { Text("Yorumunuz (İsteğe bağlı)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ModernExpandedButton(text = "İptal", onClick = onDismiss, color = MahalleTextSecondary, outline = true)
                    ModernExpandedButton(text = "Gönder", onClick = onSubmit, color = MaterialTheme.colorScheme.primary, enabled = !loading)
                }
            }
        }
    }
}