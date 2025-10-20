package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.data.repository.JobsRepository
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class JobsViewModel @Inject constructor(
    private val repo: JobsRepository
) : ViewModel() {

    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var listener: ListenerRegistration? = null

    private var lastCategory: String? = null
    private var lastMinPrice: Double? = null
    private var lastMaxPrice: Double? = null
    private var lastIsCash: Boolean? = null
    private var lastHasPhotos: Boolean? = null
    private var lastSortBy: String? = null // "createdAt" | "price"
    private var lastSortDir: Query.Direction = Query.Direction.DESCENDING

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching {
                if (
                    lastCategory != null || lastMinPrice != null || lastMaxPrice != null ||
                    lastIsCash != null || lastHasPhotos != null || lastSortBy != null
                ) {
                    repo.listOpenJobsFiltered(
                        category = lastCategory,
                        minPrice = lastMinPrice,
                        maxPrice = lastMaxPrice,
                        isCash = lastIsCash,
                        hasPhotos = lastHasPhotos,
                        sortBy = lastSortBy,
                        sortDir = lastSortDir
                    )
                } else {
                    repo.listOpenJobs()
                }
            }
                .onSuccess { _jobs.value = it }
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun startListening() {
        // Tek sefer kur, tekrar kurma
        if (listener != null) return
        listener = repo.listenOpenJobs(
            onUpdate = { _jobs.value = it },
            onError = { _error.value = it.message }
        )
    }

    fun stopListening() {
        listener?.remove()
        listener = null
    }

    fun create(job: Job, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repo.createJob(job) }
                .onSuccess(onCreated)
        }
    }

    fun applyFilters(category: String?, minPrice: Double?, maxPrice: Double?) {
        lastCategory = category?.ifBlank { null }
        lastMinPrice = minPrice
        lastMaxPrice = maxPrice
        refresh()
    }

    fun applyAdvancedFilters(
        category: String?,
        minPrice: Double?,
        maxPrice: Double?,
        isCash: Boolean?,
        hasPhotos: Boolean?,
        sortBy: String?, // "createdAt" | "price"
        sortAsc: Boolean
    ) {
        lastCategory = category?.ifBlank { null }
        lastMinPrice = minPrice
        lastMaxPrice = maxPrice
        lastIsCash = isCash
        lastHasPhotos = hasPhotos
        lastSortBy = sortBy
        lastSortDir = if (sortAsc) Query.Direction.ASCENDING else Query.Direction.DESCENDING
        refresh()
    }

    fun uploadAndAttachPhoto(
        jobId: String,
        bytes: ByteArray,
        fileName: String,
        onDone: (String) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching {
                val url = repo.uploadJobImage(jobId, bytes, fileName)
                repo.addJobPhoto(jobId, url)
                url
            }.onSuccess { onDone(it) }.onFailure(onError)
        }
    }

    suspend fun uploadAndAttachPhotoAwait(
        jobId: String,
        bytes: ByteArray,
        fileName: String
    ): String {
        val url = repo.uploadJobImage(jobId, bytes, fileName)
        repo.addJobPhoto(jobId, url)
        return url
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    // Seçili fotoğraflar (JobCreateScreen navigasyonları arasında korunur)
    private val _selectedPhotos = MutableStateFlow<List<ByteArray>>(emptyList())
    val selectedPhotos: StateFlow<List<ByteArray>> = _selectedPhotos.asStateFlow()

    fun addSelectedPhoto(bytes: ByteArray) {
        _selectedPhotos.value = _selectedPhotos.value + bytes
    }

    fun clearSelectedPhotos() {
        _selectedPhotos.value = emptyList()
    }

    fun removeSelectedPhoto(bytes: ByteArray) {
        _selectedPhotos.value = _selectedPhotos.value.filter { it !== bytes }
    }
}
