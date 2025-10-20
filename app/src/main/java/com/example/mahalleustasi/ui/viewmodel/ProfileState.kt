package com.example.mahalleustasi.ui.viewmodel

import com.example.mahalleustasi.data.model.User

/**
 * Profil ekranının durumunu temsil eden state sınıfı
 */
data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)