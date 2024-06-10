package com.example.kleine.viewmodel.reward

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kleine.database.RewardHistoryDao
import com.example.kleine.resource.NetworkReceiver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class RewardHistoryViewModel(
    private val rewardHistoryDao: RewardHistoryDao,
    private val appContext: Application // Pass Application context to ViewModel
) : AndroidViewModel(appContext) {
    val rewardHistory = MutableLiveData<List<Map<String, Any>>>()
    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    private var isNetworkAvailable: Boolean = false

    private val networkReceiver = NetworkReceiver(
        onNetworkAvailable = {
            isNetworkAvailable = true
            loadRewardHistory() // Reload history when network becomes available
        },
        onNetworkUnavailable = {
            isNetworkAvailable = false
            loadRewardHistoryFromRoom() // Load history from Room when network is unavailable
        }
    )

    init {
        // Register your NetworkReceiver here
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        appContext.registerReceiver(networkReceiver, intentFilter)

        // Manually check network availability before initial load
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isNetworkAvailable = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true

        loadRewardHistory()
    }


    private fun loadRewardHistory() {
        userId?.let {
            if (isNetworkAvailable) {
                val rewardHistoryCollection = firestore.collection("users").document(it).collection("rewardHistory")
                rewardHistoryCollection
                    .orderBy("redeemedDate", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, exception ->
                    if (exception != null || snapshot == null) {
                        return@addSnapshotListener
                    }

                    val historyList = mutableListOf<Map<String, Any>>()
                    for (document in snapshot.documents) {
                        historyList.add(document.data ?: mapOf())
                    }
                    rewardHistory.value = historyList
                    // You can also save the retrieved data to Room here for offline access
                }
            } else {
                loadRewardHistoryFromRoom()
            }
        }
    }

    private fun loadRewardHistoryFromRoom() {
        viewModelScope.launch {
            userId?.let { id ->
                val historyList = rewardHistoryDao.getAllRewardHistory(id)
                rewardHistory.value = historyList.map { rewardHistory ->
                    mapOf(
                        "redeemedDate" to rewardHistory.redeemedDate,
                        "rewardName" to rewardHistory.rewardName,
                        "rewardDetails" to rewardHistory.rewardDetails
                    )
                }
            } ?: run {
                // Handle the case where userId is null
            }
        }

    }

    override fun onCleared() {
        super.onCleared()
        appContext.unregisterReceiver(networkReceiver)
    }
}

