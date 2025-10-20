package com.example.mahalleustasi.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.mahalleustasi.data.model.Post
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    init {
        fetchPosts()
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
}