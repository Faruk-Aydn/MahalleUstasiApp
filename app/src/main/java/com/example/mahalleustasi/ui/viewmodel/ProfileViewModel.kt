package com.example.mahalleustasi.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.User
import com.example.mahalleustasi.data.repository.StorageRepository
import com.example.mahalleustasi.domain.usecase.GetCurrentUserUseCase
import com.example.mahalleustasi.domain.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val storageRepo: StorageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    fun loadMe() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { getCurrentUserUseCase() }
                .onSuccess { user -> _state.update { it.copy(user = user, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(error = error.message, isLoading = false) } }
        }
    }

    fun save(name: String, phone: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val current = _state.value.user
            val uid = current?.uid ?: ""
            val newUser = User(
                uid = uid,
                name = name,
                phone = phone,
                photoUrl = current?.photoUrl,
                ratingAvg = current?.ratingAvg,
                ratingCount = current?.ratingCount,
                location = current?.location
            )
            
            updateUserProfileUseCase(newUser)
                .onSuccess { _state.update { it.copy(user = newUser, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(error = error.message, isLoading = false) } }
        }
    }

    fun uploadProfileImage(fileUri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val url = storageRepo.uploadProfileImage(fileUri)
                val current = _state.value.user
                if (current != null) {
                    val updatedUser = current.copy(photoUrl = url)
                    updateUserProfileUseCase(updatedUser)
                    updatedUser
                } else {
                    getCurrentUserUseCase()
                }
            }.onSuccess { user -> 
                _state.update { it.copy(user = user, isLoading = false) }
            }.onFailure { error -> 
                _state.update { it.copy(error = error.message, isLoading = false) }
            }
        }
    }
}
