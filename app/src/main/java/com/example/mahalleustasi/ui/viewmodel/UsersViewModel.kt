package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.User
import com.example.mahalleustasi.data.repository.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _userMap = MutableStateFlow<Map<String, User>>(emptyMap())
    val userMap: StateFlow<Map<String, User>> = _userMap.asStateFlow()

    fun ensureUsers(uids: Collection<String>) {
        if (uids.isEmpty()) return
        viewModelScope.launch {
            val current = _userMap.value.toMutableMap()
            val missing = uids.filter { it.isNotBlank() && !current.containsKey(it) }
            missing.forEach { uid ->
                runCatching { usersRepository.getUser(uid) }
                    .onSuccess { user -> user?.let { current[uid] = it } }
            }
            _userMap.value = current
        }
    }

    fun displayName(uid: String?): String? = uid?.let { _userMap.value[it]?.name }
}
