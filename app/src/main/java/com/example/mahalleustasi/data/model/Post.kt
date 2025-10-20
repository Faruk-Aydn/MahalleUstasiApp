package com.example.mahalleustasi.data.model


data class Post(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val userId: String = "",

    val userName: String = "",
    val userProfileImageUrl: String = "",
    val category: String = "",
    val timestamp: Long = System.currentTimeMillis()
)