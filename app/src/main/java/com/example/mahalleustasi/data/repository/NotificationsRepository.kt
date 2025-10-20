package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.data.model.AppNotification
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class NotificationsRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    suspend fun saveNotification(notification: AppNotification): String {
        val ref = db.collection("notifications").document()
        val data = notification.copy(id = ref.id)
        ref.set(data).await()
        return ref.id
    }

    suspend fun listMyNotifications(): List<AppNotification> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val snap = db.collection("notifications")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(AppNotification::class.java)?.copy(id = it.id) }
    }

    suspend fun markAsRead(notificationId: String) {
        db.collection("notifications").document(notificationId).update("read", true).await()
    }

    fun listenMyNotifications(onAdded: (AppNotification) -> Unit, onError: (Throwable) -> Unit = {}): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null
        val q = db.collection("notifications")
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
        return q.addSnapshotListener(EventListener { snapshots, e ->
            if (e != null) {
                onError(e)
                return@EventListener
            }
            snapshots?.documentChanges?.forEach { dc ->
                if (dc.type == DocumentChange.Type.ADDED) {
                    dc.document.toObject(AppNotification::class.java)?.copy(id = dc.document.id)?.let(onAdded)
                }
            }
        })
    }
}
