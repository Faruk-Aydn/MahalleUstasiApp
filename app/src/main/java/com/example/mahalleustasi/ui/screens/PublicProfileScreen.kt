package com.example.mahalleustasi.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mahalleustasi.data.model.Review
import com.example.mahalleustasi.ui.theme.*
import com.example.mahalleustasi.ui.viewmodel.ReviewsViewModel
import com.example.mahalleustasi.ui.viewmodel.UsersViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    navController: NavController,
    userId: String,
    usersVm: UsersViewModel = hiltViewModel(),
    reviewsVm: ReviewsViewModel = hiltViewModel()
) {
    val userMap by usersVm.userMap.collectAsState()
    val user = userMap[userId]

    var receivedReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var givenReviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(userId) {
        usersVm.ensureUsers(listOf(userId))
        reviewsVm.list(userId) { receivedReviews = it }
        reviewsVm.listGiven(userId) { givenReviews = it }
    }

    // Değerlendirmelerdeki diğer kullanıcıların isimlerini yükle
    LaunchedEffect(receivedReviews, givenReviews) {
        val reviewerIds = receivedReviews.mapNotNull { it.reviewerId }
        val revieweeIds = givenReviews.mapNotNull { it.revieweeId }
        val allIds = (reviewerIds + revieweeIds).toSet().filter { it.isNotBlank() }
        if (allIds.isNotEmpty()) {
            usersVm.ensureUsers(allIds)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = user?.name ?: "Kullanıcı Profili",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MahalleTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
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
            if (user == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                return@Column
            }

            // Header
            PublicProfileHeader(userName = user.name, phone = user.phone, photoUrl = user.photoUrl, ratingAvg = user.ratingAvg, ratingCount = user.ratingCount)

            Spacer(Modifier.height(16.dp))

            // Tabs: Aldığı / Verdiği değerlendirmeler
            val tabs = listOf("Aldığı Değerlendirmeler", "Verdiği Değerlendirmeler")
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val list = if (selectedTab == 0) receivedReviews else givenReviews
                if (list.isEmpty()) {
                    item {
                        Text(
                            text = if (selectedTab == 0) "Henüz bu kullanıcı için yapılmış bir değerlendirme yok." else "Bu kullanıcı henüz değerlendirme yapmamış.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MahalleTextSecondary
                        )
                    }
                } else {
                    items(list, key = { it.id }) { review ->
                        val otherUserId = if (selectedTab == 0) review.reviewerId else review.revieweeId
                        val otherUserName = usersVm.displayName(otherUserId) ?: "Kullanıcı"
                        PublicReviewItem(
                            review = review,
                            isGiven = (selectedTab == 1),
                            otherUserId = otherUserId,
                            otherUserName = otherUserName,
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicProfileHeader(
    userName: String,
    phone: String?,
    photoUrl: String?,
    ratingAvg: Double?,
    ratingCount: Int?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            } else {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userName.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(userName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MahalleTextPrimary)
        phone?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MahalleTextSecondary)
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val avgText = ratingAvg?.let { String.format(Locale("tr"), "%.1f", it) } ?: "-"
            Text("Puan: $avgText", style = MaterialTheme.typography.bodyMedium, color = MahalleTextSecondary)
            Text("(${ratingCount ?: 0} değerlendirme)", style = MaterialTheme.typography.bodySmall, color = MahalleTextSecondary)
        }
    }
}

@Composable
private fun PublicReviewItem(
    review: Review,
    isGiven: Boolean,
    otherUserId: String,
    otherUserName: String,
    navController: NavController
) {
    val dateText = remember(review.createdAt) {
        try {
            val formatter = SimpleDateFormat("dd MMM yyyy", Locale("tr"))
            formatter.format(Date(review.createdAt))
        } catch (e: Exception) {
            ""
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(review.rating.coerceIn(1, 5)) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MahalleOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MahalleTextSecondary
                )
            }

            val label = if (isGiven) "Verilen değerlendirme" else "Alınan değerlendirme"
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MahalleTextSecondary
            )

            val nameLabel = if (isGiven) "Değerlendirilen" else "Değerlendiren"
            Text(
                text = "$nameLabel: $otherUserName",
                style = MaterialTheme.typography.bodySmall,
                color = MahalleTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(enabled = otherUserId.isNotBlank()) {
                    if (otherUserId.isNotBlank()) {
                        navController.navigate("public_profile/$otherUserId")
                    }
                }
            )

            (review.comment ?: "Yorum yazılmamış.").takeIf { it.isNotBlank() }?.let { commentText ->
                Text(
                    text = commentText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
