package com.example.kleine.viewmodel.quiz

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kleine.database.QuizHistoryDao
import com.example.kleine.model.PassedQuiz
import com.example.kleine.resource.NetworkReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PassedQuizzesViewModel(
    private val quizHistoryDao: QuizHistoryDao,
    private val appContext: Application // Pass Application context to ViewModel
) : AndroidViewModel(appContext) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _passedQuizzes = MutableLiveData<List<PassedQuiz>>()
    val passedQuizzes: LiveData<List<PassedQuiz>> = _passedQuizzes

    private var isNetworkAvailable: Boolean = false

    private val networkReceiver = NetworkReceiver(
        onNetworkAvailable = {
            isNetworkAvailable = true
            loadPassedQuizzes() // Reload quizzes when network becomes available
        },
        onNetworkUnavailable = {
            isNetworkAvailable = false
            loadPassedQuizzesFromRoom() // Load quizzes from Room when network is unavailable
        }
    )

    init {
        // Register NetworkReceiver here
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        appContext.registerReceiver(networkReceiver, intentFilter)

        // Manually check network availability before initial load
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isNetworkAvailable = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true

        // Initial load
        loadPassedQuizzes()
    }

    private fun loadPassedQuizzes() {
        if (isNetworkAvailable) {
            val userId = auth.currentUser?.uid ?: return
            val userDocument = firestore.collection("users").document(userId)

            userDocument.collection("quizHistory").get().addOnSuccessListener { querySnapshot ->
                val deferreds = mutableListOf<Deferred<PassedQuiz?>>()

                for (document in querySnapshot.documents) {
                    val timestamp = document.getTimestamp("date")
                    val date = timestamp?.toDate()?.toString() ?: ""
                    val materialId = document.getString("materialId") ?: ""
                    val setId = document.getString("setId") ?: ""
                    val score = document.getString("score") ?: ""

                    val deferred = CoroutineScope(Dispatchers.IO).async {
                        try {
                            val materialSnapshot = firestore.collection("Materials").document(materialId).get().await()
                            val materialName = materialSnapshot.getString("name") ?: "Material Name Not Found"
                            val setSnapshot = firestore.collection("Materials").document(materialId).collection("Sets").document(setId).get().await()
                            val setName = setSnapshot.getString("setName") ?: "Set Name Not Found"

                            PassedQuiz(
                                userDocumentId = document.id,
                                materialName = materialName,
                                date = date,
                                setName = setName,
                                score = score
                            )
                        } catch (e: Exception) {
                            Log.e("PassedQuizzesViewModel", "Error fetching material or set name", e)
                            null
                        }
                    }
                    deferreds.add(deferred)
                }

                CoroutineScope(Dispatchers.Main).launch {
                    val results = deferreds.awaitAll()
                    val sortedResults = results.filterNotNull().sortedByDescending { it.date }
                    _passedQuizzes.value = sortedResults
                }
            }.addOnFailureListener { exception ->
                Log.e("PassedQuizzesViewModel", "Error fetching quiz history", exception)
            }

        } else {
            loadPassedQuizzesFromRoom()
        }

    }

    private fun loadPassedQuizzesFromRoom() {
        viewModelScope.launch {
            // Load quiz history from Room Database
            val userId = auth.currentUser?.uid ?: return@launch
            val historyList = quizHistoryDao.getAllQuizHistory(userId)

            // Map to your PassedQuiz model and set value to _passedQuizzes
            val passedQuizzesList = historyList.map { quizHistory ->
                // Convert Long type date to dd-MM-yyyy format
                val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val date = dateFormat.format(Date(quizHistory.date))

                PassedQuiz(
                    userDocumentId = quizHistory.id.toString(), // Assuming QuizHistory has an id field, adjust accordingly
                    materialName = quizHistory.materialName,
                    setName = quizHistory.setName,
                    score = quizHistory.score,
                    date = date
                )
            }
            _passedQuizzes.value = passedQuizzesList
        }
    }

    override fun onCleared() {
        super.onCleared()
        appContext.unregisterReceiver(networkReceiver)
    }


}



