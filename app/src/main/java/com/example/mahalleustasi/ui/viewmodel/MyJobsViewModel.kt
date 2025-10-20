package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.data.repository.JobsRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MyJobsViewModel @Inject constructor(
    private val jobsRepo: JobsRepository
) : ViewModel() {

    private val _ownedJobs = MutableStateFlow<List<Job>>(emptyList())
    val ownedJobs: StateFlow<List<Job>> = _ownedJobs.asStateFlow()

    private val _assignedJobs = MutableStateFlow<List<Job>>(emptyList())
    val assignedJobs: StateFlow<List<Job>> = _assignedJobs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadOwned(status: String? = null) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { jobsRepo.listOwnedJobs(uid, status) }
                .onSuccess { _ownedJobs.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun loadAssigned(status: String? = null) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { jobsRepo.listAssignedJobs(uid, status) }
                .onSuccess { _assignedJobs.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }
}
