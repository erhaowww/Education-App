package com.example.kleine.viewmodel.admin

import android.content.Context
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class AdminAddRewardViewModel(private val appContext: Context, private val rewardDao: RewardDao) : ViewModel() {

    val firebaseStorage = FirebaseStorage.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val uploadSuccess = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String>()

    private var isNetworkAvailable: Boolean = false

    private val networkReceiver = NetworkReceiver(
        onNetworkAvailable = {
            isNetworkAvailable = true
        },
        onNetworkUnavailable = {
            isNetworkAvailable = false
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

    fun uriToByteArray(context: Context, imageUri: Uri): ByteArray? {
        return context.contentResolver.openInputStream(imageUri)?.use {
            it.readBytes()
        }
    }


    fun saveReward(imageUri: Uri?, rewardName: String, rewardDescription: String, rewardPoints: Int, redeemLimit: Int) {
        if (imageUri != null) {
            val byteArray = uriToByteArray(appContext, imageUri)
            if (isNetworkAvailable && byteArray != null) {
                val imageRef =
                    firebaseStorage.reference.child("rewards/${System.currentTimeMillis()}.jpg")

                imageRef.putBytes(byteArray).addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        saveRewardToFirestore(
                            imageUrl,
                            rewardName,
                            rewardDescription,
                            rewardPoints,
                            redeemLimit
                        )

                        // Save to local Room database when Firestore save is successful
                        saveRewardToLocalDB(byteArray, rewardName, rewardDescription, rewardPoints, redeemLimit, isAdded = 0)
                    }
                }.addOnFailureListener { exception ->
                    val errorMsg = "Error uploading image: ${exception.message}"
                    Log.e("AdminAddRewardVM", errorMsg)
                    errorMessage.value = "Error uploading image."
                }
            } else {
                // Save to Room with isAdded = 1
                saveRewardToLocalDB(byteArray ?: ByteArray(0), rewardName, rewardDescription, rewardPoints, redeemLimit, isAdded = 1)
            }
        } else {
            errorMessage.value = "Please select an image."
        }
    }

    private fun saveRewardToLocalDB(imageBytes: ByteArray, rewardName: String, rewardDescription: String, rewardPoints: Int, redeemLimit: Int, isAdded: Int) {
        val reward = Reward(
            rewardName = rewardName,
            imageBytes = imageBytes,
            redeemLimit = redeemLimit,
            rewardDescription = rewardDescription,
            rewardPoints = rewardPoints,
            isAdded = isAdded
        )

        viewModelScope.launch {
            rewardDao.insert(reward)
        }
        uploadSuccess.value = true
    }


    private fun saveRewardToFirestore(imageUrl: String, rewardName: String, rewardDescription: String, rewardPoints: Int, redeemLimit: Int) {
        val reward = hashMapOf(
            "imageUrl" to imageUrl,
            "rewardName" to rewardName,
            "rewardDescription" to rewardDescription,
            "rewardPoints" to rewardPoints,
            "redeemLimit" to redeemLimit,
            "redeemedCount" to 0
        )

        firestore.collection("Rewards").add(reward)
            .addOnSuccessListener {
                uploadSuccess.value = true
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Error adding document: ${e.message}"
            }
    }

    fun checkRewardNameExists(rewardName: String, callback: (Boolean) -> Unit) {
        if(isNetworkAvailable){
            firestore.collection("Rewards")
                .whereEqualTo("rewardName", rewardName)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        callback(false) // Reward name doesn't exist
                    } else {
                        callback(true) // Reward name already exists
                    }
                }
                .addOnFailureListener {
                    errorMessage.value = "Error checking reward name."
                }
        } else {
            // Check in Room database if no network
            viewModelScope.launch {
                val count = rewardDao.countByName(rewardName)
                callback(count > 0) // If count > 0, reward name exists in Room, else it doesn't
            }
        }

    }

    override fun onCleared() {
        super.onCleared()
        appContext.unregisterReceiver(networkReceiver)
    }


}