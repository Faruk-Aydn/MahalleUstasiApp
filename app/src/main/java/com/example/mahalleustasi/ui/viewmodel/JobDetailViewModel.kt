package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.data.repository.JobsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val jobsRepository: JobsRepository
) : ViewModel() {

    private val _job = MutableStateFlow<Job?>(null)
    val job: StateFlow<Job?> = _job.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(jobId: String) {
        viewModelScope.launch {
            runCatching { jobsRepository.getJob(jobId) }
                .onSuccess { _job.value = it }
                .onFailure { _error.value = it.message }
        }
    }
}
