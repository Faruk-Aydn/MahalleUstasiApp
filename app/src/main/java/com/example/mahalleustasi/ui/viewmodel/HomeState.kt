package com.example.mahalleustasi.ui.viewmodel

import com.example.mahalleustasi.data.model.Post

// Bu sınıf, HomeScreen'in tüm durumunu (veri, yüklenme durumu vb.) tek bir yerde tutar.
data class HomeState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true
)