package com.example.mahalleustasi.domain.repository

import com.example.mahalleustasi.data.model.User

/**
 * Kullanıcı işlemleri için repository arayüzü
 */
interface IUsersRepository {
    suspend fun getCurrentUser(): User?
    suspend fun getUser(uid: String): User?
    suspend fun upsertUser(user: User)
    suspend fun updatePhotoUrl(url: String)
}