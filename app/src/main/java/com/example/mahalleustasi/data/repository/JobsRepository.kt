package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.data.model.Job
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

class JobsRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage

    suspend fun listOpenJobs(limit: Long = 50): List<Job> {
        val snap = db.collection("jobs")
            .whereEqualTo("status", "open")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Job::class.java)?.copy(id = it.id) }
    }

    suspend fun listOpenJobsFiltered(
        category: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        isCash: Boolean? = null,
        hasPhotos: Boolean? = null,
        sortBy: String? = null, // "createdAt" | "price"
        sortDir: Query.Direction = Query.Direction.DESCENDING,
        limit: Long = 50
    ): List<Job> {
        var q: Query = db.collection("jobs")
            .whereEqualTo("status", "open")

        if (!category.isNullOrBlank()) q = q.whereEqualTo("category", category)
        if (isCash != null) q = q.whereEqualTo("isCash", isCash)
        if (minPrice != null) q = q.whereGreaterThanOrEqualTo("price", minPrice)
        if (maxPrice != null) q = q.whereLessThanOrEqualTo("price", maxPrice)

        // Sıralama mantığı: fiyat aralığı varsa Firestore gereği ilk orderBy fiyat olmalı
        val wantsPriceOrder = (minPrice != null || maxPrice != null) || sortBy == "price"
        if (wantsPriceOrder) {
            q = q.orderBy("price", sortDir)
            // İkincil sıralama eklemek isterseniz: createdAt
            q = q.orderBy("createdAt", Query.Direction.DESCENDING)
        } else {
            val by = sortBy ?: "createdAt"
            q = q.orderBy(by, sortDir)
        }

        q = q.limit(limit)

        val snap = q.get().await()
        var list = snap.documents.mapNotNull { it.toObject(Job::class.java)?.copy(id = it.id) }

        // Firestore dizi uzunluğuna göre filtreleyemediği için istemci tarafında uygula
        if (hasPhotos == true) {
            list = list.filter { it.photoUrls.isNotEmpty() }
        }

        return list
    }

    suspend fun getJob(jobId: String): Job? {
        val doc = db.collection("jobs").document(jobId).get().await()
        return doc.toObject(Job::class.java)?.copy(id = doc.id)
    }

    suspend fun listOwnedJobs(ownerId: String, status: String? = null, limit: Long = 100): List<Job> {
        var q: Query = db.collection("jobs").whereEqualTo("ownerId", ownerId)
        if (!status.isNullOrBlank()) q = q.whereEqualTo("status", status)
        // orderBy kaldırıldı: composite index gereksinimini azaltmak için
        q = q.limit(limit)
        val snap = q.get().await()
        return snap.documents.mapNotNull { it.toObject(Job::class.java)?.copy(id = it.id) }
    }

    suspend fun listAssignedJobs(proId: String, status: String? = null, limit: Long = 100): List<Job> {
        var q: Query = db.collection("jobs").whereEqualTo("assignedProId", proId)
        if (!status.isNullOrBlank()) q = q.whereEqualTo("status", status)
        // orderBy kaldırıldı: composite index gereksinimini azaltmak için
        q = q.limit(limit)
        val snap = q.get().await()
        return snap.documents.mapNotNull { it.toObject(Job::class.java)?.copy(id = it.id) }
    }

    suspend fun createJob(job: Job): String {
        val ownerId = job.ownerId.ifEmpty { auth.currentUser?.uid ?: throw IllegalStateException("Not logged in") }
        val ref = db.collection("jobs").document()
        // Kullanıcı adını users koleksiyonundan oku (varsa)
        val ownerDoc = db.collection("users").document(ownerId).get().await()
        val ownerName = ownerDoc.getString("name")
        val data = job.copy(ownerId = ownerId, ownerName = ownerName, id = ref.id)
        ref.set(data).await()
        return ref.id
    }

    suspend fun uploadJobImage(jobId: String, bytes: ByteArray, fileName: String): String {
        val ref = storage.reference.child("jobImages/$jobId/$fileName")
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun addJobPhoto(jobId: String, photoUrl: String) {
        db.collection("jobs").document(jobId)
            .update("photoUrls", FieldValue.arrayUnion(photoUrl))
            .await()
    }

    suspend fun updateStatus(jobId: String, status: String) {
        db.collection("jobs").document(jobId).update("status", status).await()
    }

    suspend fun markInProgress(jobId: String) {
        updateStatus(jobId, "in_progress")
    }

    suspend fun markAwaitingConfirmation(jobId: String) {
        updateStatus(jobId, "awaiting_confirmation")
    }

    suspend fun markCompleted(jobId: String) {
        updateStatus(jobId, "completed")
    }

    suspend fun markDisputed(jobId: String) {
        updateStatus(jobId, "disputed")
    }

    suspend fun assignJob(jobId: String, proId: String) {
        val updates = mapOf(
            "status" to "assigned",
            "assignedProId" to proId
        )
        db.collection("jobs").document(jobId).update(updates).await()
    }

    fun listenOpenJobs(onUpdate: (List<Job>) -> Unit, onError: (Exception) -> Unit = {}) : ListenerRegistration {
        return db.collection("jobs")
            .whereEqualTo("status", "open")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(Job::class.java)?.copy(id = it.id) } ?: emptyList()
                onUpdate(list)
            }
    }
}
