package com.example.mahalleustasi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
 
import com.example.mahalleustasi.ui.components.AppScaffold
import com.example.mahalleustasi.ui.viewmodel.HomeViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

// Gezinme çubuğu öğelerini Temsil eden veri sınıfı, seçili/seçilmemiş ikonları da içerir
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel) {
    // Ana sayfa artık post göstermiyor; hızlı erişim odaklı
    HomeScreenContent(
        navController = navController,
        isLoading = false // Gerekirse ViewModel'den yüklenme durumu eklenebilir
    )
}

@Composable
fun HomeScreenContent(
    navController: NavController,
    isLoading: Boolean
) {
    // Alt menü öğeleri için liste. Seçili ve seçilmemiş ikonlar ile daha modern bir görünüm.
    val navItems = listOf(
        BottomNavItem("home", "Anasayfa", Icons.Filled.Home, Icons.Outlined.Home),
        BottomNavItem("jobs", "İşler", Icons.Filled.List, Icons.Outlined.List),
        BottomNavItem("offers", "Teklifler", Icons.Filled.Notifications, Icons.Outlined.Notifications),
        BottomNavItem("profile", "Profil", Icons.Filled.Face, Icons.Outlined.Face)
    )

    AppScaffold(
        title = "Mahalledeki İşler",
        actions = {
            // Üst çubuktaki + butonu FAB ile aynı işi yaptığı için arayüzü sadeleştirmek adına kaldırıldı.
            IconButton(onClick = {
                Firebase.auth.signOut()
                navController.navigate("login") { popUpTo(0) }
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Çıkış Yap"
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface, // Arka plan rengi
                tonalElevation = 8.dp // Hafif bir yükselme efekti
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Veriler yükleniyorsa bir yüklenme animasyonu göster
                AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                    LoadingScreen()
                }

                // Hızlı erişim kartları
                AnimatedVisibility(visible = !isLoading, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "Hoş geldin!", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "Hızlıca bir ilan oluşturabilir, mevcut işleri inceleyebilir veya tekliflerini görüntüleyebilirsin.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { navController.navigate("job_create") }, modifier = Modifier.weight(1f)) { Text("Yeni İlan") }
                            OutlinedButton(onClick = { navController.navigate("jobs") }, modifier = Modifier.weight(1f)) { Text("İşler") }
                            OutlinedButton(onClick = { navController.navigate("offers") }, modifier = Modifier.weight(1f)) { Text("Teklifler") }
                        }
                    }
                }
            }
        },
        // Floating Action Button'ı AppScaffold'un kendi parametresi ile eklemek daha doğrudur.
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("job_create") },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni İlan Ekle", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    )
}

 


@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyScreen(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}