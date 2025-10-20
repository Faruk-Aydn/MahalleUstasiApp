package com.example.mahalleustasi.domain.usecase

import com.example.mahalleustasi.data.model.User
import com.example.mahalleustasi.data.repository.UsersRepository
import javax.inject.Inject

/**
 * Mevcut kullanıcı bilgilerini getiren UseCase
 */
class GetCurrentUserUseCase @Inject constructor(
    private val usersRepository: UsersRepository
) {
    suspend operator fun invoke(): User? {
        return usersRepository.getCurrentUser()
    }
}