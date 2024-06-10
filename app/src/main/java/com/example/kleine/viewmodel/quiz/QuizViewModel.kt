package com.example.kleine.viewmodel.quiz

import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class QuizViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    val materialNameLiveData = MutableLiveData<String>()
    val showToastLiveData = MutableLiveData<String>()
    val navigateToPlayFragmentLiveData = MutableLiveData<String>()

    fun fetchAndDisplayMaterialName(materialDocId: String) {
         viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch the material document
                val materialDocument = db.collection("Materials").document(materialDocId).get().await()

                // Extract the material name from the document
                val materialName = materialDocument.getString("name") ?: "N/A"

                // Update the materialNameTextView with the fetched material name
                materialNameLiveData.postValue(materialName)
            } catch (e: Exception) {
                // Handle any errors here
                withContext(Dispatchers.Main) {
                    showToastLiveData.postValue("Failed to fetch material name: $e")
                }
            }
        }
    }

    fun checkAvailableSetsAndNavigate(materialDocId: String, view: View) {
        // Launch coroutine in the GlobalScope
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch sets
                val sets = db.collection("Materials").document(materialDocId).collection("Sets").get().await()
                val availableSets = mutableListOf<String>()

                // Fetch the user's quiz history for the specific material
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                val quizHistory = if (userId != null) {
                    db.collection("users")
                        .document(userId)
                        .collection("quizHistory")
                        .whereEqualTo("materialId", materialDocId)
                        .get()
                        .await()
                        .documents
                        .map { it.getString("setId") }
                } else {
                    emptyList()
                }

                // Iterate over each set and check if it has sufficient questions and is not in the quiz history
                for (setDocument in sets.documents) {
                    val setId = setDocument.id
                    val questions = setDocument.reference.collection("Questions").get().await()
                    if (questions.size() >= 5 && setId !in quizHistory) {
                        availableSets.add(setId)
                    }
                }
                // Log the available sets
                Log.d("QuizViewModel", "Available Sets: $availableSets")
                // Navigate to PlayFragment if there are available sets, otherwise show a Toast
                withContext(Dispatchers.Main) {
                    if (availableSets.isNotEmpty()) {
                        val randomSetId = availableSets.random()
                        navigateToPlayFragmentLiveData.postValue(randomSetId)
                    } else {
                        showToastLiveData.postValue("No available sets with sufficient questions")
                    }
                }
            } catch (e: Exception) {
                // Handle any errors here
                withContext(Dispatchers.Main) {
                    showToastLiveData.postValue("Failed to fetch sets: $e")
                }
            }
        }
    }

    fun resetToastLiveData() {
        showToastLiveData.value = "null"
    }


}