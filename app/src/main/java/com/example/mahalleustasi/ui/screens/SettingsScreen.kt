package com.example.mahalleustasi.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mahalleustasi.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val loading = viewModel.loading.collectAsState().value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ayarlar")
        OutlinedButton(onClick = { navController.navigate("profile") }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Profil Düzenle")
        }
        OutlinedButton(onClick = { navController.navigate("notifications") }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Bildirimler")
        }
        OutlinedButton(onClick = { navController.navigate("offers") }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Tekliflerim")
        }
        Button(
            onClick = { viewModel.signOut { navController.navigate("login") { popUpTo(0) } } },
            enabled = !loading,
            modifier = Modifier.padding(top = 16.dp)
        ) { Text(if (loading) "Çıkış yapılıyor..." else "Çıkış Yap") }
    }
}
