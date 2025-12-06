package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.Message
import com.example.mahalleustasi.data.repository.MessagesRepository
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: MessagesRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var listener: ListenerRegistration? = null

    fun start(chatId: String) {
        stop()
        listener = repo.listenChatMessages(
            chatId = chatId,
            onUpdate = { _messages.value = it },
            onError = { _error.value = it.message }
        )
    }

    fun stop() {
        listener?.remove()
        listener = null
    }

    fun sendText(chatId: String, text: String) {
        viewModelScope.launch {
            runCatching { repo.sendTextMessage(chatId, text) }
                .onFailure {
                    _error.value = it.message
                    Log.d("ChatViewModel", "sendText error", it)
                }
        }
    }

    fun sendImage(chatId: String, bytes: ByteArray, fileName: String) {
        viewModelScope.launch {
            runCatching {
                val url = repo.uploadChatImage(chatId, bytes, fileName)
                repo.sendImageMessage(chatId, url)
            }.onFailure {
                _error.value = it.message
                Log.d("ChatViewModel", "sendImage error", it)
            }
        }
    }

    fun markAsRead(chatId: String, messageId: String) {
        viewModelScope.launch {
            runCatching { repo.markAsRead(chatId, messageId) }
                .onFailure { _error.value = it.message }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
