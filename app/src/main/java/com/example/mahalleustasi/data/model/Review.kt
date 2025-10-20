package com.example.mahalleustasi.data.model

data class Review(
    val id: String = "",
    val jobId: String = "",
    val reviewerId: String = "",
    val revieweeId: String = "",
    val rating: Int = 0,
    val comment: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
