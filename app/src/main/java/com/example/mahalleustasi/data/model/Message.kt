package com.example.mahalleustasi.data.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String? = null,
    val text: String? = null,
    val imageUrl: String? = null,
    val readBy: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
