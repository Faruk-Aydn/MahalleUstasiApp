package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.Review
import com.example.mahalleustasi.data.repository.ReviewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val repo: ReviewsRepository
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun submit(review: Review, onDone: (String) -> Unit = {}) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            runCatching { repo.submitReview(review) }
                .onSuccess(onDone)
                .onFailure { _error.value = it.message }
            _loading.value = false
        }
    }

    fun list(userId: String, onLoaded: (List<Review>) -> Unit) {
        viewModelScope.launch { runCatching { repo.listReviewsForUser(userId) }.onSuccess(onLoaded) }
    }

    fun listGiven(reviewerId: String, onLoaded: (List<Review>) -> Unit) {
        viewModelScope.launch { runCatching { repo.listReviewsByReviewer(reviewerId) }.onSuccess(onLoaded) }
    }

    fun hasReviewed(jobId: String, reviewerId: String, revieweeId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            runCatching { repo.hasUserReviewedJob(jobId, reviewerId, revieweeId) }
                .onSuccess(onResult)
                .onFailure { _error.value = it.message }
        }
    }
}
