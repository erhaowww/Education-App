package com.example.kleine.viewmodel.user

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class UserViewModel : ViewModel() {
    fun fetchUserName(userId: String, callback: (String?) -> Unit) {
        if (userId.isBlank()) {
            callback(null)
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val userName = document.getString("firstName") + " " + document.getString("lastName")
                    callback(userName)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun fetchUserEmail(userId: String, callback: (String?) -> Unit) {
        if (userId.isBlank()) {
            callback(null)
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val email = document.getString("email")
                    callback(email)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun fetchUserImage(userId: String, callback: (String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val userImage = document.getString("imagePath")
                    callback(userImage)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }



}