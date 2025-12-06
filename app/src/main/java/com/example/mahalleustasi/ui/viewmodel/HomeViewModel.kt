package com.example.mahalleustasi.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahalleustasi.data.model.Post
import com.example.mahalleustasi.data.model.Job
import com.example.mahalleustasi.data.repository.JobsRepository
import com.example.mahalleustasi.data.repository.OffersRepositoryContract
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val jobsRepository: JobsRepository,
    private val offersRepository: OffersRepositoryContract
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Kullanıcının sahip olduğu ve atandığı işler
    private val _ownedJobs = MutableStateFlow<List<Job>>(emptyList())
    val ownedJobs: StateFlow<List<Job>> = _ownedJobs.asStateFlow()

    private val _assignedJobs = MutableStateFlow<List<Job>>(emptyList())
    val assignedJobs: StateFlow<List<Job>> = _assignedJobs.asStateFlow()

    private val _myOffersCount = MutableStateFlow(0)
    val myOffersCount: StateFlow<Int> = _myOffersCount.asStateFlow()

    init {
        fetchPosts()
        refreshUserJobs()
    }

    private fun fetchPosts() {
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HomeViewModel", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val postList = snapshot.documents.mapNotNull {
                        it.toObject(Post::class.java)?.copy(id = it.id)
                    }
                    _posts.value = postList
                }
            }
    }

    fun addPost(title: String, description: String) {
        val userId = auth.currentUser?.uid ?: return

        val newPost = hashMapOf(
            "title" to title,
            "description" to description,
            "userId" to userId,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("posts")
            .add(newPost)
            .addOnSuccessListener {
                Log.d("HomeViewModel", "Post added successfully")
            }
            .addOnFailureListener { e ->
                Log.e("HomeViewModel", "Error adding post", e)
            }
    }

    fun refreshUserJobs() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val owned = runCatching { jobsRepository.listOwnedJobs(ownerId = userId, limit = 100) }
                .getOrElse { emptyList() }
            val assigned = runCatching { jobsRepository.listAssignedJobs(proId = userId, limit = 100) }
                .getOrElse { emptyList() }
            _ownedJobs.value = owned
            _assignedJobs.value = assigned

            // Teklif sayım
            val offers = runCatching { offersRepository.listOffersByPro(userId) }.getOrElse { emptyList() }
            _myOffersCount.value = offers.count { it.status == "pending" }
        }
    }
}
