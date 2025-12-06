package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.data.model.Review
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class ReviewsRepository {
    private val db = Firebase.firestore

    suspend fun submitReview(review: Review): String {
        val ref = db.collection("reviews").document()
        val data = review.copy(id = ref.id)
        ref.set(data).await()
        // Kullanıcı ortalamasını güncelle (transaction)
        updateUserRating(review.revieweeId)
        return ref.id
    }

    suspend fun listReviewsForUser(userId: String): List<Review> {
        val snap = db.collection("reviews")
            .whereEqualTo("revieweeId", userId)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Review::class.java) }
    }

    suspend fun listReviewsByReviewer(reviewerId: String): List<Review> {
        val snap = db.collection("reviews")
            .whereEqualTo("reviewerId", reviewerId)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Review::class.java) }
    }

    suspend fun hasUserReviewedJob(jobId: String, reviewerId: String, revieweeId: String): Boolean {
        val snap = db.collection("reviews")
            .whereEqualTo("jobId", jobId)
            .whereEqualTo("reviewerId", reviewerId)
            .whereEqualTo("revieweeId", revieweeId)
            .limit(1)
            .get()
            .await()
        return !snap.isEmpty
    }

    private suspend fun updateUserRating(userId: String) {
        val reviewsSnap = db.collection("reviews").whereEqualTo("revieweeId", userId).get().await()
        val reviews = reviewsSnap.documents.mapNotNull { it.toObject(Review::class.java) }
        val avg = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 0.0
        val count = reviews.size
        db.collection("users").document(userId)
            .update(mapOf("ratingAvg" to avg, "ratingCount" to count))
            .await()
    }
}
