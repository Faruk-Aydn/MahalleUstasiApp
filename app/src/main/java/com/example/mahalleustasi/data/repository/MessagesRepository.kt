package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.data.model.Message
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

class MessagesRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage

    private fun chatMessagesRef(chatId: String) =
        db.collection("messages").document(chatId).collection("chatMessages")

    suspend fun sendTextMessage(chatId: String, text: String, receiverId: String? = null): String {
        val senderId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
        val docRef = chatMessagesRef(chatId).document()
        val msg = Message(
            id = docRef.id,
            chatId = chatId,
            senderId = senderId,
            receiverId = receiverId,
            text = text
        )
        docRef.set(msg).await()
        return docRef.id
    }

    suspend fun uploadChatImage(chatId: String, bytes: ByteArray, fileName: String): String {
        val ref = storage.reference.child("chat_images/$chatId/$fileName")
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun sendImageMessage(chatId: String, imageUrl: String, receiverId: String? = null): String {
        val senderId = auth.currentUser?.uid ?: throw IllegalStateException("Not logged in")
        val docRef = chatMessagesRef(chatId).document()
        val msg = Message(
            id = docRef.id,
            chatId = chatId,
            senderId = senderId,
            receiverId = receiverId,
            imageUrl = imageUrl
        )
        docRef.set(msg).await()
        return docRef.id
    }

    fun listenChatMessages(
        chatId: String,
        onUpdate: (List<Message>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration {
        return chatMessagesRef(chatId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError(err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { it.toObject(Message::class.java) } ?: emptyList()
                onUpdate(list)
            }
    }

    suspend fun markAsRead(chatId: String, messageId: String) {
        val uid = auth.currentUser?.uid ?: return
        chatMessagesRef(chatId)
            .document(messageId)
            .update("readBy", com.google.firebase.firestore.FieldValue.arrayUnion(uid))
            .await()
    }
}
