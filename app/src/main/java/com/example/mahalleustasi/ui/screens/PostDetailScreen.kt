package com.example.mahalleustasi.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mahalleustasi.data.model.Post
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(navController: NavController, postId: String) {
    var post by remember { mutableStateOf<Post?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val currentUser = Firebase.auth.currentUser

    // Bu effect, ekran açıldığında postId ile Firestore'dan veri çeker
    LaunchedEffect(key1 = postId) {
        val db = Firebase.firestore
        db.collection("posts").document(postId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    post = document.toObject(Post::class.java)?.copy(id = document.id)
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("İlan Detayı") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (post != null) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(text = post!!.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = post!!.description, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Yayınlanma Tarihi: ${formatTimestamp(post!!.timestamp)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "İlanı Veren ID: ${post!!.userId}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // --- YENİ EKLENEN BÖLÜM BAŞLANGICI ---

                    Spacer(modifier = Modifier.weight(1f)) // Butonu en alta iter

                    // Eğer ilanı açan kişi, mevcut kullanıcı değilse butonu göster
                    if (currentUser != null && currentUser.uid != post!!.userId) {
                        Button(
                            onClick = {
                                val db = Firebase.firestore
                                val offerData = hashMapOf(
                                    "applicantId" to currentUser.uid,
                                    "timestamp" to System.currentTimeMillis()
                                )

                                // İlgili post'un altına 'offers' koleksiyonu oluşturup yeni teklifi ekle
                                db.collection("posts").document(postId)
                                    .collection("offers").document(currentUser.uid)
                                    .set(offerData)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Teklifin başarıyla iletildi!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Bir hata oluştu.", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Bu İşi Ben Yaparım!")
                        }
                    }
                    // --- YENİ EKLENEN BÖLÜM SONU ---
                }
            } else {
                Text("İlan bulunamadı.", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

// Zaman damgasını (timestamp) okunabilir bir tarihe çeviren yardımcı fonksiyon
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr"))
    val date = Date(timestamp)
    return sdf.format(date)
}