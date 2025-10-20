package com.example.mahalleustasi.ui.viewmodel

import com.example.mahalleustasi.data.model.Message

/**
 * Sohbet ekranının durumunu temsil eden state sınıfı
 */
data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)