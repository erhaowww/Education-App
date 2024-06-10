package com.example.kleine.viewmodel.reward

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kleine.database.RewardHistoryDao

class RewardHistoryViewModelFactory(
    private val rewardHistoryDao: RewardHistoryDao,
    private val appContext: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RewardHistoryViewModel::class.java)) {
            return RewardHistoryViewModel(rewardHistoryDao, appContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

