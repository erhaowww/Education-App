package com.example.kleine.viewmodel.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.kleine.database.RewardDao

class AdminUpdateRewardViewModelFactory(
    private val appContext: Context,
    private val rewardDao: RewardDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminUpdateRewardViewModel::class.java)) {
            return AdminUpdateRewardViewModel(appContext, rewardDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}