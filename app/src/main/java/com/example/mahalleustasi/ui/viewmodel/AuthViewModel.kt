package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val firebaseUser: StateFlow<FirebaseUser?> =
        authRepository.authState().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = authRepository.currentUser()
        )

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun register(email: String, password: String, name: String, onSuccess: (String) -> Unit = {}) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { authRepository.registerWithEmail(email, password, name) }
                .onSuccess(onSuccess)
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { authRepository.loginWithEmail(email, password) }
                .onSuccess { onSuccess() }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun loginWithGoogleIdToken(idToken: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { authRepository.loginWithGoogleIdToken(idToken) }
                .onSuccess { onSuccess() }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }
}
