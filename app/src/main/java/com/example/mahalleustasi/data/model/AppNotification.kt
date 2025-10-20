package com.example.mahalleustasi.data.model

data class AppNotification(
    val id: String = "",
    val userId: String = "",
    val type: String = "generic", // new_offer | offer_accepted | job_completed | message | generic
    val title: String = "",
    val body: String = "",
    val data: Map<String, String>? = null,
    val read: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
