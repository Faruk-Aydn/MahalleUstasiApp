package com.example.mahalleustasi.domain.usecase

import com.example.mahalleustasi.data.model.User
import com.example.mahalleustasi.data.repository.UsersRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class GetCurrentUserUseCaseTest {
    
    private lateinit var usersRepository: UsersRepository
    private lateinit var getCurrentUserUseCase: GetCurrentUserUseCase
    
    @Before
    fun setup() {
        usersRepository = Mockito.mock(UsersRepository::class.java)
        getCurrentUserUseCase = GetCurrentUserUseCase(usersRepository)
    }
    
    @Test
    fun `getCurrentUser returns user when repository returns user`() = runBlocking {
        // Arrange
        val expectedUser = User(uid = "test-uid", name = "Test User")
        `when`(usersRepository.getCurrentUser()).thenReturn(expectedUser)
        
        // Act
        val result = getCurrentUserUseCase()
        
        // Assert
        assertEquals(expectedUser, result)
    }
    
    @Test
    fun `getCurrentUser returns null when repository returns null`() = runBlocking {
        // Arrange
        `when`(usersRepository.getCurrentUser()).thenReturn(null)
        
        // Act
        val result = getCurrentUserUseCase()
        
        // Assert
        assertEquals(null, result)
    }
}