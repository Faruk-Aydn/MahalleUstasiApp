package com.example.mahalleustasi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mahalleustasi.data.model.Review
import com.example.mahalleustasi.ui.viewmodel.ReviewsViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun ReviewsScreen(navController: NavController, revieweeId: String, jobId: String? = null, viewModel: ReviewsViewModel = hiltViewModel()) {
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    val loading by viewModel.loading.collectAsState(false)
    val currentUid = Firebase.auth.currentUser?.uid

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Puan Ver (1-5)")
        OutlinedTextField(value = rating.toString(), onValueChange = { rating = it.toIntOrNull()?.coerceIn(1,5) ?: rating }, label = { Text("Puan") })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Yorum") })
        Spacer(Modifier.height(12.dp))
        if (currentUid == null) {
            Text("Yorum yapabilmek için giriş yapmalısınız.")
        }
        Button(
            onClick = {
                val r = Review(
                    jobId = jobId ?: "",
                    reviewerId = currentUid ?: "",
                    revieweeId = revieweeId,
                    rating = rating,
                    comment = comment.ifBlank { null }
                )
                viewModel.submit(r) { navController.popBackStack() }
            },
            enabled = !loading && currentUid != null
        ) { Text(if (loading) "Gönderiliyor..." else "Gönder") }
    }
}

