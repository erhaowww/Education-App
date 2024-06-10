package com.example.kleine.viewmodel.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore

class QuestionViewModel(private val setDocumentId: String, private val materialId: String) : ViewModel() {

    private val _questions = MutableLiveData<List<String>>()
    val questions: LiveData<List<String>>
        get() = _questions

    private val _operationStatus = MutableLiveData<String>()
    val operationStatus: LiveData<String>
        get() = _operationStatus

    private val _questionIdMap = MutableLiveData<MutableMap<String, String>>()
    val questionIdMap: LiveData<MutableMap<String, String>>
        get() = _questionIdMap

    private val db = FirebaseFirestore.getInstance()

    init {
        _questionIdMap.value = mutableMapOf()
        fetchQuestions(materialId, setDocumentId) // Replace with actual IDs
    }

     fun fetchQuestions(materialId: String, setId: String) {
        db.collection("Materials")
            .document(materialId)
            .collection("Sets")
            .document(setId)
            .collection("Questions")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val questionsList = mutableListOf<String>()
                val questionMap = mutableMapOf<String, String>()
                for (document in querySnapshot.documents) {
                    val questionText = document["questionText"] as String
                    questionsList.add(questionText)
                    questionMap[questionText] = document.id
                }
                _questions.value = questionsList
                _questionIdMap.value = questionMap
            }
            .addOnFailureListener { e ->
                // Handle error here
            }
    }

    fun addNewQuestion(materialId: String, setId: String, newQuestion: Map<String, Any>) {
        val questionText = newQuestion["questionText"] as String

        // Check if the question already exists in the list
        if (_questions.value?.contains(questionText) == true) {
            _operationStatus.value = "DuplicateQuestion"
            return
        }

        db.collection("Materials")
            .document(materialId)
            .collection("Sets")
            .document(setId)
            .collection("Questions")
            .add(newQuestion)
            .addOnSuccessListener { _ ->
                // Refresh the list of questions
                fetchQuestions(materialId, setId)
                _operationStatus.value = "Success"
            }
            .addOnFailureListener { e ->
                // Handle error here
                _operationStatus.value = "Failure"
            }
    }

    fun updateQuestion(materialDocId: String, setDocumentId: String, questionId: String, updatedData: Map<String, Any>) {
        val updatedQuestionText = updatedData["questionText"] as? String ?: return

        // Check if the updated question text is the same as any existing question in the set (excluding the question being updated)
        if (_questions.value?.any { it == updatedQuestionText && _questionIdMap.value?.get(it) != questionId } == true) {
            _operationStatus.value = "DuplicateQuestion"
            return
        }

        db.collection("Materials")
            .document(materialDocId)
            .collection("Sets")
            .document(setDocumentId)
            .collection("Questions")
            .document(questionId)
            .update(updatedData)
            .addOnSuccessListener {
                _operationStatus.value = "UpdateSuccess"
            }
            .addOnFailureListener {
                _operationStatus.value = "UpdateFailure"
            }
    }

    fun fetchSingleQuestion(materialDocId: String, setDocumentId: String, questionId: String): LiveData<Map<String, Any>> {
        val questionData = MutableLiveData<Map<String, Any>>()

        db.collection("Materials")
            .document(materialDocId)
            .collection("Sets")
            .document(setDocumentId)
            .collection("Questions")
            .document(questionId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val questionMap = mutableMapOf<String, Any>()
                questionMap["questionText"] = documentSnapshot["questionText"] as String
                questionMap["optionA"] = documentSnapshot["optionA"] as String
                questionMap["optionB"] = documentSnapshot["optionB"] as String
                questionMap["optionC"] = documentSnapshot["optionC"] as String
                questionMap["optionD"] = documentSnapshot["optionD"] as String
                questionMap["correctAnswer"] = documentSnapshot["correctAnswer"] as String

                questionData.value = questionMap
            }
            .addOnFailureListener {
                // Handle failure
            }

        return questionData
    }

    fun deleteQuestion(materialDocId: String, setDocumentId: String, questionId: String) {
        db.collection("Materials")
            .document(materialDocId)
            .collection("Sets")
            .document(setDocumentId)
            .collection("Questions")
            .document(questionId)
            .delete()
            .addOnSuccessListener {
                _operationStatus.value = "DeleteSuccess"
                fetchQuestions(materialDocId, setDocumentId)  // Fetch updated list of questions
            }
            .addOnFailureListener {
                _operationStatus.value = "DeleteFailure"
            }
    }

    fun resetShowToastMsg() {
        _operationStatus.value = ""
    }
}