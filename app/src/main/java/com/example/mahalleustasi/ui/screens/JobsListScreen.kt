package com.example.mahalleustasi.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mahalleustasi.ui.viewmodel.JobsViewModel
import com.example.mahalleustasi.ui.components.EmptyState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- RENK PALETİ ---
private val AppPrimaryColor = Color(0xFF00695C) // Koyu Turkuaz
private val AppAccentColor = Color(0xFFFFB74D)  // Canlı Turuncu
private val AppBackgroundColor = Color(0xFFF5F7FA) // Modern Gri-Mavi Zemin
private val TextHeadColor = Color(0xFF1A1C1E)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun JobsListScreen(
    navController: NavController,
    viewModel: JobsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope
) {
    val jobs by viewModel.jobs.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    // --- KATEGORİ LİSTESİ (Görselden alındı) ---
    val categories = listOf(
        "Temizlik",
        "Boya & Badana",
        "Tesisat",
        "Elektrik",
        "Nakliyat",
        "Tamir",
        "Bahçe",
        "İnşaat",
        "Diğer"
    )

    // Filtre State'leri
    var category by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) } // Dropdown açık/kapalı durumu

    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("createdAt") }
    var sortAsc by remember { mutableStateOf(false) }

    // Drawer ve Scroll State
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // FAB genişleme mantığı
    val isFabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    LaunchedEffect(Unit) {
        viewModel.startListening()
        viewModel.refresh()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        drawerContent = {
            // --- GELİŞMİŞ FİLTRE MENÜSÜ ---
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerContentColor = TextHeadColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "Detaylı Filtreleme",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = AppPrimaryColor)
                    )
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                    // --- KATEGORİ SEÇİMİ (DROPDOWN) ---
                    Text("Kategori", style = MaterialTheme.typography.titleSmall)

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (category.isEmpty()) "Tüm Kategoriler" else category,
                            onValueChange = {}, // ReadOnly olduğu için boş
                            readOnly = true, // Klavye açılmasın
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppPrimaryColor,
                                unfocusedContainerColor = Color(0xFFF9F9F9),
                                focusedContainerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor() // Menünün nerede açılacağını belirtir
                        )

                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            // Seçimi temizleme seçeneği
                            DropdownMenuItem(
                                text = { Text("Tüm Kategoriler (Temizle)", color = Color.Gray) },
                                onClick = {
                                    category = ""
                                    categoryExpanded = false
                                }
                            )
                            HorizontalDivider()
                            // Kategori Listesi
                            categories.forEach { selection ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            selection,
                                            fontWeight = if(category == selection) FontWeight.Bold else FontWeight.Normal,
                                            color = if(category == selection) AppPrimaryColor else Color.Black
                                        )
                                    },
                                    onClick = {
                                        category = selection
                                        categoryExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    // --- FİYAT ARALIĞI ---
                    Text("Fiyat Aralığı", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = minPrice,
                            onValueChange = { if (it.all { c -> c.isDigit() }) minPrice = it },
                            placeholder = { Text("Min") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppPrimaryColor,
                                unfocusedContainerColor = Color(0xFFF9F9F9)
                            )
                        )
                        OutlinedTextField(
                            value = maxPrice,
                            onValueChange = { if (it.all { c -> c.isDigit() }) maxPrice = it },
                            placeholder = { Text("Max") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppPrimaryColor,
                                unfocusedContainerColor = Color(0xFFF9F9F9)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // --- FİLTRELE BUTONU ---
                    Button(
                        onClick = {
                            viewModel.applyAdvancedFilters(
                                category = category.ifBlank { null },
                                minPrice = minPrice.toDoubleOrNull(),
                                maxPrice = maxPrice.toDoubleOrNull(),
                                isCash = null, hasPhotos = null, sortBy = sortBy, sortAsc = sortAsc
                            )
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimaryColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sonuçları Göster", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = AppBackgroundColor,
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text("Yeni İlan Ver", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { navController.navigate("job_create") },
                    expanded = isFabExpanded,
                    containerColor = AppAccentColor,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                )
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize()) {
                // Header Arka Planı
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(AppPrimaryColor, AppPrimaryColor.copy(alpha = 0.8f))
                            )
                        )
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // --- 1. MODERN HEADER ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Tüm İlanlar",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                "Mahallendeki iş fırsatlarını keşfet",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    // --- 2. HIZLI FİLTRE ÇUBUĞU ---
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        // Ana Filtre Butonu
                        item {
                            Surface(
                                onClick = { scope.launch { drawerState.open() } },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Eğer filtre aktifse ikon rengini değiştir
                                    val isFilterActive = category.isNotEmpty() || minPrice.isNotEmpty() || maxPrice.isNotEmpty()
                                    Icon(
                                        Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = if(isFilterActive) AppAccentColor else AppPrimaryColor
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Filtrele",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if(isFilterActive) AppAccentColor else AppPrimaryColor
                                    )
                                }
                            }
                        }

                        // Hızlı Sıralama
                        item {
                            QuickFilterChip(
                                label = "En Yeni",
                                isSelected = sortBy == "createdAt" && !sortAsc,
                                onClick = {
                                    sortBy = "createdAt"; sortAsc = false
                                    viewModel.applyAdvancedFilters(
                                        category = category.ifBlank { null },
                                        minPrice = minPrice.toDoubleOrNull(),
                                        maxPrice = maxPrice.toDoubleOrNull(),
                                        isCash = null, hasPhotos = null,
                                        sortBy = "createdAt", sortAsc = false
                                    )
                                }
                            )
                        }
                        item {
                            QuickFilterChip(
                                label = "Fiyat Artan",
                                isSelected = sortBy == "price" && sortAsc,
                                onClick = {
                                    sortBy = "price"; sortAsc = true
                                    viewModel.applyAdvancedFilters(
                                        category = category.ifBlank { null },
                                        minPrice = minPrice.toDoubleOrNull(),
                                        maxPrice = maxPrice.toDoubleOrNull(),
                                        isCash = null, hasPhotos = null,
                                        sortBy = "price", sortAsc = true
                                    )
                                }
                            )
                        }
                        item {
                            QuickFilterChip(
                                label = "Fiyat Azalan",
                                isSelected = sortBy == "price" && !sortAsc,
                                onClick = {
                                    sortBy = "price"; sortAsc = false
                                    viewModel.applyAdvancedFilters(
                                        category = category.ifBlank { null },
                                        minPrice = minPrice.toDoubleOrNull(),
                                        maxPrice = maxPrice.toDoubleOrNull(),
                                        isCash = null, hasPhotos = null,
                                        sortBy = "price", sortAsc = false
                                    )
                                }
                            )
                        }
                    }

                    // --- 3. LİSTE ALANI ---
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (loading) {
                            items(4) { ModernJobShimmer() }
                        } else if (jobs.isEmpty()) {
                            item {
                                EmptyState(
                                    title = "İlan Bulunamadı",
                                    description = "Şu an kriterlerine uygun ilan yok.\nYeni bir arama yapabilirsin."
                                )
                            }
                        } else {
                            items(jobs, key = { it.id }) { job ->
                                with(sharedTransitionScope) {
                                    ModernJobCard(
                                        job = job,
                                        onClick = { navController.navigate("job_detail/${job.id}") }
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// --------------------------- YARDIMCI BİLEŞENLER --------------------------- //

@Composable
fun QuickFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AppPrimaryColor else Color.White.copy(alpha = 0.9f),
        contentColor = if (isSelected) Color.White else Color.DarkGray,
        border = if (!isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)) else null,
        modifier = Modifier.shadow(
            elevation = if (isSelected) 4.dp else 0.dp,
            shape = RoundedCornerShape(12.dp),
            clip = false
        )
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        }
    }
}

@Composable
fun ModernJobCard(job: com.example.mahalleustasi.data.model.Job, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Kategori + Başlık + Fiyat
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(AppPrimaryColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = AppPrimaryColor)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = job.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextHeadColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = job.location?.address ?: "Bölge Belirtilmemiş",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Surface(
                    color = AppAccentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${job.price?.toInt() ?: 0} ₺",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = job.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))

            // Alt Bilgiler
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    val createdDateText = remember(job.createdAt) {
                        try {
                            val formatter = SimpleDateFormat("dd MMM", Locale("tr"))
                            formatter.format(Date(job.createdAt))
                        } catch (e: Exception) {
                            "Bugün"
                        }
                    }
                    Text("İlan Tarihi: $createdDateText", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if(job.status == "open") Color(0xFFE0F2F1) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if(job.status == "open") "Aktif" else "Bekliyor",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if(job.status == "open") AppPrimaryColor else Color(0xFFEF6C00),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = AppPrimaryColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernJobShimmer() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart), label = ""
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color.White, Color(0xFFEEEEEE), Color.White),
        start = Offset.Zero, end = Offset(x = translateAnim, y = translateAnim)
    )
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth().height(170.dp)
    ) {
        Box(Modifier.fillMaxSize().background(brush))
    }
}