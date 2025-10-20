package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.data.model.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class UsersRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return getUser(uid)
    }

    suspend fun getUser(uid: String): User? {
        val snap = db.collection("users").document(uid).get().await()
        return snap.toObject(User::class.java)?.copy(uid = uid)
    }

    suspend fun upsertUser(user: User) {
        val uid = user.uid.ifEmpty { auth.currentUser?.uid ?: return }
        db.collection("users").document(uid).set(user.copy(uid = uid)).await()
    }

    suspend fun updateFcmToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("fcmToken", token).await()
    }

    suspend fun updatePhotoUrl(photoUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("photoUrl", photoUrl).await()
    }
}
