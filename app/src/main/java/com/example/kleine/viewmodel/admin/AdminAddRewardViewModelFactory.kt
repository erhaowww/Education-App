package com.example.kleine.viewmodel.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kleine.database.RewardDao

class AdminAddRewardViewModelFactory(
    private val appContext: Context,
    private val rewardDao: RewardDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminAddRewardViewModel::class.java)) {
            return AdminAddRewardViewModel(appContext, rewardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
