package com.example.mahalleustasi.domain.usecase

import com.example.mahalleustasi.data.model.User
import com.example.mahalleustasi.data.repository.UsersRepository
import javax.inject.Inject

/**
 * Kullanıcı profilini güncelleyen UseCase
 */
class UpdateUserProfileUseCase @Inject constructor(
    private val usersRepository: UsersRepository
) {
    suspend operator fun invoke(user: User): Result<Unit> {
        return try {
            usersRepository.upsertUser(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}