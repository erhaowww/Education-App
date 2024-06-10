package com.example.kleine.viewmodel.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SetsViewModelFactory(private val materialId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetsViewModel::class.java)) {
            return SetsViewModel(materialId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
