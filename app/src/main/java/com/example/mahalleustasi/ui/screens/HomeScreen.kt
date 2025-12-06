package com.example.mahalleustasi.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.ui.viewmodel.CurrentUserViewModel
import com.example.mahalleustasi.ui.viewmodel.HomeViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay

// --- ÖZEL RENK PALETİ ---
private val AppPrimaryColor = Color(0xFF00695C) // Koyu Turkuaz (Güven)
private val AppAccentColor = Color(0xFFFFB74D)  // Turuncu (Aksiyon)
private val AppBackgroundColor = Color(0xFFF7F9FC) // Çok açık gri-mavi (Zemin)

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val ownedJobs by viewModel.ownedJobs.collectAsState()
    val assignedJobs by viewModel.assignedJobs.collectAsState()

    val currentUserViewModel: CurrentUserViewModel = hiltViewModel()
    val currentUser by currentUserViewModel.user.collectAsState()

    val firebaseUser = Firebase.auth.currentUser
    val displayName = when {
        !currentUser?.name.isNullOrBlank() -> currentUser?.name!!
        firebaseUser?.displayName.isNullOrBlank().not() -> firebaseUser?.displayName!!
        !firebaseUser?.email.isNullOrBlank() -> firebaseUser?.email!!.substringBefore("@")
        else -> "Mahalle Sakini"
    }

    // Home ekrana her gelindiğinde kullanıcının iş listesini yenile
    LaunchedEffect(Unit) {
        viewModel.refreshUserJobs()
    }

    HomeScreenContent(
        navController = navController,
        ownedJobs = ownedJobs,
        assignedJobs = assignedJobs,
        displayName = displayName
    )
}

@Composable
fun HomeScreenContent(
    navController: NavController,
    ownedJobs: List<Job>,
    assignedJobs: List<Job>,
    displayName: String
) {
    val scrollState = rememberScrollState()

    // --- MENÜ İKONLARI VE İSİMLERİ ---
    val navItems = listOf(
        NavItem(
            route = "home",
            label = "Anasayfa",
            selectedIcon = Icons.Rounded.Home,       // Dolu ev
            unselectedIcon = Icons.Rounded.Home      // (Varsa HomeOutlined kullanabilirsin)
        ),
        NavItem(
            route = "jobs", // Tüm işlerin olduğu genel liste
            label = "İş Ara", // "İşler" yerine "İş Ara" demek daha anlaşılır (Search mantığı)
            selectedIcon = Icons.Rounded.Search,     // Büyüteç: İş arama / Keşfetme
            unselectedIcon = Icons.Rounded.Search
        ),
        NavItem(
            route = "offers",
            label = "Teklifler",
            selectedIcon = Icons.Rounded.Notifications, // Bildirim/Teklif
            unselectedIcon = Icons.Rounded.Notifications
        ),
        NavItem(
            route = "profile",
            label = "Profil",
            selectedIcon = Icons.Rounded.Person,
            unselectedIcon = Icons.Rounded.Person
        )
    )

    Scaffold(
        containerColor = AppBackgroundColor,
        bottomBar = {
            // Yüzen (Floating) Beyaz Alt Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 0.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                color = Color.White
            ) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0.dp),
                    modifier = Modifier.height(70.dp) // Yüksekliği biraz azalttık, daha zarif olsun
                ) {
                    val backStack by navController.currentBackStackEntryAsState()
                    val current = backStack?.destination?.route

                    navItems.forEach { item ->
                        val selected = current == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                // Seçili ikon biraz daha büyük
                                val size = if (selected) 26.dp else 24.dp
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(size)
                                )
                            },
                            label = {
                                // Etiketleri görünür yapıyoruz ki "Anlaşılır" olsun
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = AppPrimaryColor.copy(alpha = 0.1f), // Çok hafif turkuaz hale
                                selectedIconColor = AppPrimaryColor, // İkon koyu turkuaz
                                selectedTextColor = AppPrimaryColor, // Yazı koyu turkuaz
                                unselectedIconColor = Color.Gray.copy(alpha = 0.6f),
                                unselectedTextColor = Color.Gray.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(scrollState)
        ) {
            // 1. HEADER
            HeaderSection(displayName = displayName, onLogout = {
                Firebase.auth.signOut()
                navController.navigate("login") { popUpTo(0) }
            })

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-30).dp)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 2. YENİ İLAN OLUŞTUR BUTONU
                CreateJobActionCard { navController.navigate("job_create") }

                // 3. TEKLİF UYARISI
                if (assignedJobs.isNotEmpty()) {
                    val pendingCount = assignedJobs.filter { it.status == "pending" }.size
                    if(pendingCount > 0) {
                        NotificationBanner(count = pendingCount) {
                            navController.navigate("offers")
                        }
                    }
                }

                // 4. İLANLARIM (Senin oluşturdukların)
                HomeSection(
                    title = "Yayınladığın İlanlar",
                    onSeeAll = { navController.navigate("my_jobs") }
                ) {
                    if (ownedJobs.isEmpty()) {
                        EmptyStateCard(message = "Henüz bir ilan yayınlamadın.\nEvin için bir usta bulmaya ne dersin?")
                    } else {
                        ownedJobs.take(3).forEachIndexed { index, job ->
                            JobItemCard(job = job, index = index) {
                                navController.navigate("job_detail/${job.id}")
                            }
                        }
                    }
                }

                // 5. ÜZERİNDE ÇALIŞTIKLARIN (Sana atananlar)
                HomeSection(
                    title = "Üzerinde Çalıştığın İşler",
                    onSeeAll = { navController.navigate("my_jobs_assigned") }
                ) {
                    if (assignedJobs.isEmpty()) {
                        EmptyStateCard(
                            message = "Henüz sana atanan bir iş yok.\n'İş Ara' sekmesinden ilanlara bakabilirsin.",
                            icon = Icons.Rounded.Search // Burayı da Search ikonuyla güncelledim
                        )
                    } else {
                        assignedJobs.take(3).forEachIndexed { index, job ->
                            JobItemCard(job = job, index = index, isAssigned = true) {
                                navController.navigate("job_detail/${job.id}")
                            }
                        }
                    }
                }

                // Alt barın üstünde biraz boşluk bırak (Scroll bitince içerik barın altında kalmasın)
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// --------------------------- BİLEŞENLER --------------------------- //

@Composable
fun HeaderSection(displayName: String, onLogout: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppPrimaryColor,
                        Color(0xFF004D40) // Biraz daha koyusu, degrade için
                    )
                )
            )
    ) {
        // Dekoratif daireler
        Box(
            modifier = Modifier
                .offset(x = 240.dp, y = (-50).dp)
                .size(250.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-60).dp, y = 40.dp)
                .size(150.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hoş geldin,",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .size(44.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                    contentDescription = "Çıkış",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun CreateJobActionCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(AppAccentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = AppAccentColor, // Turuncu ikon
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Yeni İlan Oluştur",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF212121)
                )
                Text(
                    text = "İhtiyacın olan ustayı bul",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}

@Composable
fun NotificationBanner(count: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0) // Turuncu tint
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = Color(0xFFEF6C00)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$count yeni teklif var!",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFE65100)
            )
        }
    }
}

@Composable
fun HomeSection(
    title: String,
    onSeeAll: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF263238)
            )
            TextButton(onClick = onSeeAll) {
                Text(
                    "Tümünü Gör",
                    fontWeight = FontWeight.Bold,
                    color = AppPrimaryColor,
                    fontSize = 13.sp
                )
            }
        }
        content()
    }
}

@Composable
fun JobItemCard(
    job: Job,
    index: Int,
    isAssigned: Boolean = false,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 100L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(initialOffsetY = { 50 })
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = job.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAssigned) "İşveren: ${job.ownerName ?: "Bilinmiyor"}" else "İlan Tarihi: Bugün",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    Text(
                        text = "${job.price?.toInt() ?: 0} ₺",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = AppPrimaryColor
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HomeStatusChip(status = job.status)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Detay",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeStatusChip(status: String) {
    val (containerColor, contentColor, text) = when (status.lowercase()) {
        "pending", "bekliyor" -> Triple(Color(0xFFFFF8E1), Color(0xFFF57C00), "Bekliyor")
        "active", "aktif", "assigned" -> Triple(Color(0xFFE0F2F1), Color(0xFF00695C), "Aktif")
        "completed", "tamamlandı" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Tamamlandı")
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF757575), status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.height(24.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

@Composable
fun EmptyStateCard(message: String, icon: ImageVector = Icons.Rounded.AddHome) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(50.dp).background(Color(0xFFF5F5F5), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}