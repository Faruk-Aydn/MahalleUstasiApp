package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun signOut(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _loading.value = true
            authRepository.signOut()
            _loading.value = false
            onDone()
        }
    }
}
