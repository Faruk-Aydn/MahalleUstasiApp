package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.Offer
import com.example.mahalleustasi.data.repository.OffersRepositoryContract
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val repo: OffersRepositoryContract
) : ViewModel() {

    private val _offers = MutableStateFlow<List<Offer>>(emptyList())
    val offers: StateFlow<List<Offer>> = _offers.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadForJob(jobId: String) {
        viewModelScope.launch {
            _loading.value = true
            runCatching { repo.listOffersForJob(jobId) }
                .onSuccess { _offers.value = it }
                .also { _loading.value = false }
        }
    }

    fun loadMyOffers() {
        viewModelScope.launch {
            _loading.value = true
            runCatching { repo.listOffersByPro() }
                .onSuccess { _offers.value = it }
                .also { _loading.value = false }
        }
    }

    fun submit(
        offer: Offer,
        onCreated: (String) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching { repo.submitOffer(offer) }
                .onSuccess(onCreated)
                .onFailure(onError)
        }
    }

    fun accept(offerId: String, jobId: String) {
        viewModelScope.launch {
            runCatching { repo.acceptOffer(offerId) }
                .onSuccess {
                    // Kabul sonrası ilgili işin teklif listesini tazele
                    loadForJob(jobId)
                }
        }
    }

    fun withdraw(offerId: String, jobId: String? = null) {
        viewModelScope.launch {
            runCatching { repo.updateStatus(offerId, "withdrawn") }
                .onSuccess {
                    if (jobId != null) loadForJob(jobId) else loadMyOffers()
                }
        }
    }

    fun reject(offerId: String, jobId: String) {
        viewModelScope.launch {
            runCatching { repo.updateStatus(offerId, "rejected") }
                .onSuccess { loadForJob(jobId) }
        }
    }
}
