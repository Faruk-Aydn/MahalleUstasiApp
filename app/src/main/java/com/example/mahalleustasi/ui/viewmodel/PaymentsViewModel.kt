package com.example.mahalleustasi.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.Payment
import com.example.mahalleustasi.data.repository.PaymentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PaymentsViewModel @Inject constructor(
    private val repo: PaymentsRepository
) : ViewModel() {

    private val _payments = MutableStateFlow<List<Payment>>(emptyList())
    val payments: StateFlow<List<Payment>> = _payments.asStateFlow()

    fun loadForJob(jobId: String) {
        viewModelScope.launch {
            runCatching { repo.listPaymentsForJob(jobId) }
                .onSuccess { _payments.value = it }
        }
    }

    fun loadMine() {
        viewModelScope.launch {
            runCatching { repo.listMyPayments() }
                .onSuccess { _payments.value = it }
        }
    }

    fun recordCash(payment: Payment, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repo.recordCashPayment(payment) }
                .onSuccess(onCreated)
        }
    }

    fun confirm(paymentId: String, byPayer: Boolean? = null, byPayee: Boolean? = null) {
        viewModelScope.launch {
            runCatching { repo.confirmPayment(paymentId, byPayer, byPayee) }
        }
    }
}
