package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.repository.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileGateViewModel @Inject constructor(
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _needsProfile = MutableStateFlow<Boolean?>(null)
    val needsProfile: StateFlow<Boolean?> = _needsProfile.asStateFlow()

    fun check() {
        viewModelScope.launch {
            val me = usersRepository.getCurrentUser()
            val complete = me?.name?.isNotBlank() == true
            _needsProfile.value = !complete
        }
    }
}
