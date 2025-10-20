package com.example.mahalleustasi.data.model

data class Offer(
    val id: String = "",
    val jobId: String = "",
    val proId: String = "",
    val amount: Double? = null,
    val note: String? = null,
    val status: String = "pending", // pending | accepted | rejected | withdrawn
    val createdAt: Long = System.currentTimeMillis()
)
