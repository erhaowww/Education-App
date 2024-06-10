package com.example.kleine.viewmodel.reward

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RedeemRewardViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RedeemRewardViewModel::class.java)) {
            return RedeemRewardViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
