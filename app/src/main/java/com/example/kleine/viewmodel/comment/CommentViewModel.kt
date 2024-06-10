package com.example.kleine.viewmodel.comment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kleine.model.Comment
import com.example.kleine.model.CommentWithUserDetails
import com.example.kleine.viewmodel.user.UserViewModel
import com.firebase.ui.auth.AuthUI.TAG
import com.google.firebase.firestore.FirebaseFirestore

class CommentViewModel(private val userViewModel: UserViewModel) : ViewModel() {

    private val _commentsWithUserDetails = MutableLiveData<List<CommentWithUserDetails>>()
    val commentsWithUserDetails: LiveData<List<CommentWithUserDetails>> = _commentsWithUserDetails
    private val db = FirebaseFirestore.getInstance()
    private val commentsLiveData = MutableLiveData<List<Comment>>()
    class CommentViewModelFactory(private val userViewModel: UserViewModel) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CommentViewModel::class.java)) {
                return CommentViewModel(userViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
    fun fetchComments(materialId: String) {
        db.collection("Comments")
            .whereEqualTo("materialId", materialId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val commentsList = mutableListOf<CommentWithUserDetails>()
                value?.forEach { document ->
                    val comment = document.toObject(Comment::class.java)
                    comment.id = document.id
                    userViewModel.fetchUserName(comment.userId) { userName ->
                        userViewModel.fetchUserImage(comment.userId) { userImage ->
                            var partnerName: String? = null
                            var partnerImage: String? = null

                            if (comment.replyPartnerId != "") {
                                userViewModel.fetchUserName(comment.replyPartnerId.toString()) { pName ->
                                    partnerName = pName
                                    userViewModel.fetchUserImage(comment.replyPartnerId.toString()) { pImage ->
                                        partnerImage = pImage

                                        val commentWithUserDetails = CommentWithUserDetails(
                                            comment,
                                            userName,
                                            userImage,
                                            partnerName,
                                            partnerImage
                                        )
                                        commentsList.add(commentWithUserDetails)
                                        _commentsWithUserDetails.postValue(commentsList)
                                    }
                                }
                            } else {
                                val commentWithUserDetails = CommentWithUserDetails(
                                    comment,
                                    userName,
                                    userImage,
                                    null,
                                    null
                                )
                                commentsList.add(commentWithUserDetails)
                                _commentsWithUserDetails.postValue(commentsList)
                            }
                        }
                    }
                }
            }
    }

    fun fetchCommentToReply(commentId: String, callback: (CommentWithUserDetails) -> Unit) {
        if (commentId.isBlank()) {
            Log.e(TAG, "Error: Invalid commentId")
            return
        }

        db.collection("Comments").document(commentId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val comment = documentSnapshot.toObject(Comment::class.java) ?: return@addOnSuccessListener
                userViewModel.fetchUserName(comment.userId) { userName ->
                    userViewModel.fetchUserImage(comment.userId) { userImage ->
                        val commentWithUserDetails = CommentWithUserDetails(
                            comment,
                            userName,
                            userImage,
                            null,
                            null
                        )
                        callback(commentWithUserDetails)
                    }
                }
            }
    }

}
