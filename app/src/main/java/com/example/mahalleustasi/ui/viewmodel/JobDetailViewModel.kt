package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.data.repository.JobsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ListenerRegistration

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val jobsRepository: JobsRepository
) : ViewModel() {

    private val _job = MutableStateFlow<Job?>(null)
    val job: StateFlow<Job?> = _job.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private var jobListener: ListenerRegistration? = null

    fun load(jobId: String) {
        viewModelScope.launch {
            runCatching { jobsRepository.getJob(jobId) }
                .onSuccess { _job.value = it }
                .onFailure { _error.value = it.message }
        }
    }

    fun observe(jobId: String) {
        // Eski listener'ı kapat
        jobListener?.remove()
        jobListener = jobsRepository.listenJob(jobId,
            onUpdate = { _job.value = it },
            onError = { _error.value = it.message }
        )
    }

    override fun onCleared() {
        super.onCleared()
        jobListener?.remove()
        jobListener = null
    }

    fun markAwaitingConfirmation(jobId: String) {
        viewModelScope.launch {
            _updating.value = true
            runCatching { jobsRepository.markAwaitingConfirmation(jobId) }
                .onSuccess { 
                    Log.d("JobDetailVM", "markAwaitingConfirmation success for jobId=$jobId")
                    load(jobId)
                    _updating.value = false
                    _toast.value = "Tamamlandı bildirimin gönderildi. İşveren onayı bekleniyor."
                }
                .onFailure { 
                    Log.e("JobDetailVM", "markAwaitingConfirmation failed for jobId=$jobId", it)
                    _error.value = it.message
                    _updating.value = false
                    _toast.value = "Bildirim gönderilemedi: ${it.message ?: "Bilinmeyen hata"}"
                }
        }
    }

    fun markCompleted(jobId: String) {
        viewModelScope.launch {
            _updating.value = true
            runCatching { jobsRepository.markCompleted(jobId) }
                .onSuccess { 
                    load(jobId)
                    _updating.value = false
                }
                .onFailure { 
                    _error.value = it.message
                    _updating.value = false
                }
        }
    }

    fun markDisputed(jobId: String) {
        viewModelScope.launch {
            _updating.value = true
            runCatching { jobsRepository.markDisputed(jobId) }
                .onSuccess { 
                    load(jobId)
                    _updating.value = false
                }
                .onFailure { 
                    _error.value = it.message
                    _updating.value = false
                }
        }
    }

    fun clearToast() {
        _toast.value = null
    }
}
