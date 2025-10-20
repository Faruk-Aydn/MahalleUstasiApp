package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.AppNotification
import com.example.mahalleustasi.data.repository.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationsRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<AppNotification>>(emptyList())
    val items: StateFlow<List<AppNotification>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { repo.listMyNotifications() }
                .onSuccess { _items.value = it }
                .also { _loading.value = false }
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch { runCatching { repo.markAsRead(id) }.onSuccess { refresh() } }
    }
}
