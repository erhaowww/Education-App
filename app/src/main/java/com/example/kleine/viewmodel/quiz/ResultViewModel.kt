package com.example.kleine.viewmodel.quiz

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kleine.database.HelpDatabase
import com.example.kleine.database.QuizHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResultViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun updateUserPoints(earnedPoints: Int) {
        val userId = auth.currentUser?.uid
//        val userId = "GyK8GLYxHkUKQ3BRowU6uPNbKRm1"
        if (userId != null) {
            val userDocument = firestore.collection("users").document(userId)

            // Retrieve the current points of the user
            userDocument.get().addOnSuccessListener { snapshot ->
                val currentPoints = snapshot.getLong("points") ?: 0
                val updatedPoints = currentPoints + earnedPoints

                // Update the points in Firestore
                userDocument.update("points", updatedPoints)
                    .addOnSuccessListener {
                        // Handle success - e.g., show a Toast
                    }
                    .addOnFailureListener { e ->
                        // Handle failure - e.g., show a Toast or Log error
                    }
            }
        }
    }

//    fun storeQuizHistory(score: Int, totalQuestions: Int, materialId: String, setId: String) {
//        val userId = auth.currentUser?.uid
//        if (userId != null) {
//            val userDocument = firestore.collection("users").document(userId)
//
//            // Formatting the score as "x / totalQuestions"
//            val formattedScore = "$score / $totalQuestions"
//
//            // Creating a map to store quiz data
//            val quizData = hashMapOf(
//                "score" to formattedScore,
//                "date" to FieldValue.serverTimestamp(),
//                "materialId" to materialId,
//                "setId" to setId
//            )
//
//            // Adding quiz data to the quizHistory sub-collection
//            userDocument.collection("quizHistory")
//                .add(quizData)
//                .addOnSuccessListener {
//                    // Handle success - e.g., show a Toast or Log message
//                }
//                .addOnFailureListener { e ->
//                    // Handle failure - e.g., show a Toast or Log error
//                }
//        }
//    }

    fun storeQuizHistory(score: Int, totalQuestions: Int, materialId: String, setId: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userDocument = firestore.collection("users").document(userId)

            // Retrieve material name and set name from Firestore based on materialId and setId
            firestore.collection("Materials").document(materialId).get()
                .addOnSuccessListener { materialDocument ->
                    val materialName = materialDocument.getString("name") ?: ""

                    // Fetch set name
                    firestore.collection("Materials").document(materialId)
                        .collection("Sets").document(setId).get()
                        .addOnSuccessListener { setDocument ->
                            val setName = setDocument.getString("setName") ?: ""

                            // Formatting the score as "x / totalQuestions"
                            val formattedScore = "$score / $totalQuestions"

                            // Creating a map to store quiz data
                            val quizData = hashMapOf(
                                "score" to formattedScore,
                                "date" to FieldValue.serverTimestamp(),
                                "materialId" to materialId,
                                "setId" to setId,
                            )

                            // Adding quiz data to the quizHistory sub-collection in Firestore
                            userDocument.collection("quizHistory")
                                .add(quizData)
                                .addOnSuccessListener {
                                    // Handle success - e.g., show a Toast or Log message
                                }
                                .addOnFailureListener { e ->
                                    // Handle failure - e.g., show a Toast or Log error
                                }

                            // Save quiz data to Room database
                            val quizHistory = QuizHistory(
                                userId = userId,
                                materialName = materialName,
                                setName = setName,
                                score = formattedScore,
                                date = System.currentTimeMillis() // Current timestamp
                            )
                            insertQuizHistoryToRoom(quizHistory)
                        }
                }
        }
    }

    private fun insertQuizHistoryToRoom(quizHistory: QuizHistory) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = HelpDatabase.getDatabase(getApplication()).quizHistoryDao()
            dao.insertQuizHistory(quizHistory)
        }
    }


}
