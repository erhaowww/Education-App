package com.example.kleine.viewmodel.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PlayViewModelFactory(private val materialDocID: String?, private val randomSetID: String?) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayViewModel::class.java)) {
            return PlayViewModel(materialDocID, randomSetID) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
