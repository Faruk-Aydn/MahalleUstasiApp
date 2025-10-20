package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.data.model.Payment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class PaymentsRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    suspend fun listPaymentsForJob(jobId: String): List<Payment> {
        val snap = db.collection("payments")
            .whereEqualTo("jobId", jobId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Payment::class.java)?.copy(id = it.id) }
    }

    suspend fun listMyPayments(): List<Payment> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snap = db.collection("payments")
            .whereIn("payerId", listOf(uid))
            .get().await()
        val snap2 = db.collection("payments")
            .whereIn("payeeId", listOf(uid))
            .get().await()
        val a = snap.documents.mapNotNull { it.toObject(Payment::class.java)?.copy(id = it.id) }
        val b = snap2.documents.mapNotNull { it.toObject(Payment::class.java)?.copy(id = it.id) }
        return (a + b).sortedByDescending { it.createdAt }
    }

    suspend fun recordCashPayment(payment: Payment): String {
        val payerId = payment.payerId.ifEmpty { auth.currentUser?.uid ?: throw IllegalStateException("Not logged in") }
        val ref = db.collection("payments").add(payment.copy(payerId = payerId)).await()
        return ref.id
    }

    suspend fun confirmPayment(paymentId: String, byPayer: Boolean? = null, byPayee: Boolean? = null) {
        val updates = mutableMapOf<String, Any>()
        byPayer?.let { updates["confirmedByPayer"] = it }
        byPayee?.let { updates["confirmedByPayee"] = it }
        if (updates.isNotEmpty()) {
            db.collection("payments").document(paymentId).update(updates).await()
        }
    }
}
