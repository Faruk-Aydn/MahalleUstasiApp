package com.example.mahalleustasi.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {
    private fun requireUid(): String = auth.currentUser?.uid
        ?: throw IllegalStateException("Kullanıcı oturum açmamış")

    private fun defaultFileName(): String = "${System.currentTimeMillis()}.jpg"

    suspend fun uploadProfileImage(
        fileUri: Uri,
        fileName: String = defaultFileName()
    ): String {
        val uid = requireUid()
        val ref = storage.reference.child("profileImages/$uid/$fileName")
        val metadata = storageMetadata { contentType = "image/jpeg" }
        ref.putFile(fileUri, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadJobImage(
        jobId: String,
        fileUri: Uri,
        fileName: String = defaultFileName()
    ): String {
        val ref = storage.reference.child("jobImages/$jobId/$fileName")
        val metadata = storageMetadata { contentType = "image/jpeg" }
        ref.putFile(fileUri, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteByUrl(downloadUrl: String) {
        val ref = storage.getReferenceFromUrl(downloadUrl)
        ref.delete().await()
    }
}
