package com.example.kleine.viewmodel.admin

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kleine.database.Reward
import com.example.kleine.database.RewardDao
import com.example.kleine.resource.NetworkReceiver
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class AdminViewRewardViewModel(private val appContext: Context, private val rewardDao: RewardDao) : ViewModel() {
    val firebaseStorage = FirebaseStorage.getInstance()
    val rewards = MutableLiveData<List<Reward>>()
    val deleteResult = MutableLiveData<Boolean>()

    private var isNetworkAvailable: Boolean = false

    private val networkReceiver = NetworkReceiver(
        onNetworkAvailable = {
            isNetworkAvailable = true
            loadDataBasedOnConnection()
        },
        onNetworkUnavailable = {
            isNetworkAvailable = false
            loadDataBasedOnConnection()
        }
    )

    init {
        // Register your NetworkReceiver here
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        appContext.registerReceiver(networkReceiver, intentFilter)

        // Manually check network availability before initial load
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        isNetworkAvailable = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true

    }

     fun loadDataBasedOnConnection() {
        if (isNetworkAvailable) {
            syncLocalDataToFirestore() // Sync data from Room to Firestore, if needed
            fetchRewardsFromFirestore() // Fetch data from Firestore
        } else {
            fetchRewardsFromLocalDB() // Fetch data from Room when offline
        }
    }

    private fun syncLocalDataToFirestore() {
        viewModelScope.launch {
            syncNewRewardsToFirestore()
            updateFirestoreForModifiedRewards()
        }
    }


    private fun syncNewRewardsToFirestore() {
        viewModelScope.launch {
            val unsyncedRewards = rewardDao.getUnsyncedRewards(1) // Assuming isAdded = 1 denotes unsynced data
            val rewardsCollection = FirebaseFirestore.getInstance().collection("Rewards")

            for (reward in unsyncedRewards) {
                // If the reward has an image in ByteArray format
                if (reward.imageBytes != null) {
                    val imageRef = firebaseStorage.reference.child("rewards/${System.currentTimeMillis()}.jpg")

                    imageRef.putBytes(reward.imageBytes!!).addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            val newImageUrl = uri.toString()

                            // Create a new reward object without imageBytes but with new imageUrl
                            // Create a new data map to represent the reward for Firestore
                            val firestoreReward = hashMapOf(
                                "rewardName" to reward.rewardName,
                                "imageUrl" to newImageUrl,
                                "redeemLimit" to reward.redeemLimit,
                                "redeemedCount" to reward.redeemedCount,
                                "rewardDescription" to reward.rewardDescription,
                                "rewardPoints" to reward.rewardPoints,
                            )

                            rewardsCollection.add(firestoreReward).addOnSuccessListener {
                                // Once added successfully, mark the reward as synced in Room
                                reward.isAdded = 0
                                viewModelScope.launch {
                                    rewardDao.update(reward)
                                }
                            }
                        }
                    }.addOnFailureListener { exception ->
                        // Handle failure in image uploading
                        val errorMsg = "Error uploading image: ${exception.message}"
                        Log.e("AdminViewRewardVM", errorMsg)
                    }
                } else {
                    // If there's no image to upload, directly add the reward to Firestore
                    rewardsCollection.add(reward).addOnSuccessListener {
                        // Once added successfully, mark the reward as synced in Room
                        reward.isAdded = 0
                        viewModelScope.launch {
                            rewardDao.update(reward)
                        }
                    }
                }
            }
        }
    }

    private fun updateFirestoreForModifiedRewards() {
        viewModelScope.launch {
            val modifiedRewards = rewardDao.getModifiedRewards(1) // Assuming isUpdated = 1 denotes modified data
            val rewardsCollection = FirebaseFirestore.getInstance().collection("Rewards")

            for (reward in modifiedRewards) {
                if (reward.imageUrl == "changed" && reward.imageBytes != null) {
                    // Update the image on Firestore
                    val imageRef = firebaseStorage.reference.child("rewards/${System.currentTimeMillis()}.jpg")

                    imageRef.putBytes(reward.imageBytes!!).addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            val newImageUrl = uri.toString()

                            // Create a new data map to represent the reward for Firestore
                            val firestoreReward = hashMapOf(
                                "rewardName" to reward.rewardName,
                                "imageUrl" to newImageUrl,
                                "redeemLimit" to reward.redeemLimit,
                                "redeemedCount" to reward.redeemedCount,
                                "rewardDescription" to reward.rewardDescription,
                                "rewardPoints" to reward.rewardPoints,
                            )

                            updateFirestoreAndResetImageUrl(rewardsCollection, reward, firestoreReward as HashMap<String, Any?>)
                        }
                    }.addOnFailureListener { exception ->
                        val errorMsg = "Error uploading image: ${exception.message}"
                        Log.e("AdminViewRewardVM", errorMsg)
                    }
                } else {
                    // Don't update the image on Firestore, but other data can be updated
                    val firestoreReward = hashMapOf(
                        "rewardName" to reward.rewardName,
                        "redeemLimit" to reward.redeemLimit,
                        "redeemedCount" to reward.redeemedCount,
                        "rewardDescription" to reward.rewardDescription,
                        "rewardPoints" to reward.rewardPoints,
                    )

                    updateFirestoreAndResetImageUrl(rewardsCollection, reward, firestoreReward as HashMap<String, Any?>)
                }
            }
        }
    }

    private fun updateFirestoreAndResetImageUrl(rewardsCollection: CollectionReference, reward: Reward, firestoreReward: HashMap<String, Any?>) {
        rewardsCollection.whereEqualTo("rewardName", reward.rewardName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    document.reference.update(firestoreReward as Map<String, Any>).addOnSuccessListener {
                        // Once updated successfully, mark the reward as not modified and imageUrl to null in Room
                        reward.isUpdated = 0
                        reward.imageUrl = null
                        viewModelScope.launch {
                            rewardDao.update(reward)
                        }
                    }
                }
            }
    }

    private fun fetchRewardsFromFirestore() {
        val rewardsCollection = FirebaseFirestore.getInstance().collection("Rewards")
        rewardsCollection.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                return@addSnapshotListener
            }

            val rewardList = mutableListOf<Reward>()
            for (document in snapshot.documents) {
                val reward = document.toObject(Reward::class.java)
                if (reward != null) {
                    rewardList.add(reward)
                }
            }

            rewards.value = rewardList
        }
    }

    private fun fetchRewardsFromLocalDB() {
        viewModelScope.launch {
            val localRewards = rewardDao.getAllRewards()
            rewards.value = localRewards
        }
    }

    fun deleteReward(rewardName: String) {
        if(isNetworkAvailable){
            val rewardsCollection = FirebaseFirestore.getInstance().collection("Rewards")

            // Query to find the document that has the rewardName
            rewardsCollection.whereEqualTo("rewardName", rewardName)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    // Check if a document with that rewardName was found
                    if (!querySnapshot.isEmpty) {
                        // Get the first (and should be the only) document that matches the query
                        val document = querySnapshot.documents[0]

                        // Delete the found document
                        document.reference
                            .delete()
                            .addOnSuccessListener {
                                // Log success
                                deleteResult.value = true

                                // Mark reward as deleted in RoomDB
                                viewModelScope.launch {
                                    val existingReward = rewardDao.getRewardByName(rewardName)
                                    if(existingReward != null) {
                                        existingReward.isDeleted = 1
                                        rewardDao.update(existingReward)
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                // Handle failure
                                deleteResult.value = false
                            }
                    } else {
                        // No document with that rewardName found
                        deleteResult.value = false
                    }
                }
                .addOnFailureListener { e ->
                    // Handle failure
                    deleteResult.value = false
                }
        } else {
            deleteResult.value = false
        }

    }


    override fun onCleared() {
        super.onCleared()
        appContext.unregisterReceiver(networkReceiver)
    }
}
