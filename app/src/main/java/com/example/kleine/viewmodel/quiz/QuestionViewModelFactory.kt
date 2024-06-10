package com.example.kleine.viewmodel.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class QuestionViewModelFactory(
    private val setDocumentId: String,
    private val materialId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuestionViewModel::class.java)) {
            return QuestionViewModel(setDocumentId, materialId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
