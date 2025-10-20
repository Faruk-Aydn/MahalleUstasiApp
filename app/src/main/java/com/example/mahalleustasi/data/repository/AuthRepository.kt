package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val usersRepository: UsersRepository
) {
    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun registerWithEmail(email: String, password: String, name: String): String {
        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = res.user?.uid ?: throw IllegalStateException("Registration failed")
        // users/{uid} dokümanı aç
        usersRepository.upsertUser(User(uid = uid, name = name))
        return uid
    }

    suspend fun loginWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    fun currentUser(): FirebaseUser? = auth.currentUser

    fun signOut() = auth.signOut()

    suspend fun loginWithGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val res = auth.signInWithCredential(credential).await()
        val uid = res.user?.uid ?: return
        // Eğer users/{uid} yoksa oluştur
        val existing = usersRepository.getUser(uid)
        if (existing == null) {
            usersRepository.upsertUser(User(uid = uid, name = res.user?.displayName ?: ""))
        }
    }
}
