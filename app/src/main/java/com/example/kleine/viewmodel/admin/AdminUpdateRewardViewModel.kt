package com.example.kleine.viewmodel.admin

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kleine.database.Reward
import com.example.kleine.database.RewardDao
import com.example.kleine.resource.NetworkReceiver
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class AdminUpdateRewardViewModel(private val appContext: Context, private val rewardDao: RewardDao) : ViewModel(){
    val rewardName = MutableLiveData<String>()
    val rewardDescription = MutableLiveData<String>()
    val rewardPoints = MutableLiveData<String>()
    val redeemLimit = MutableLiveData<String>()
    val imageUrl = MutableLiveData<String>()
    val imageBytes = MutableLiveData<ByteArray?>()

    private val db = FirebaseFirestore.getInstance()
    val firebaseStorage = FirebaseStorage.getInstance()
    val updateResult = MutableLiveData<Boolean>()

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

    fun loadRewardDetails(rewardName: String?) {
        if (isNetworkAvailable) {
            loadRewardFromFirebase(rewardName)
        } else {
            loadRewardFromRoom(rewardName)
        }
    }

    private fun loadRewardFromFirebase(rewardName: String?) {
        rewardName?.let {
            db.collection("Rewards")
                .whereEqualTo("rewardName", it)
                .limit(1)  // Limiting to one document since reward names should be unique
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.documents.isNotEmpty()) {
                        val document = documents.documents[0]
                        this.rewardName.value = document.getString("rewardName")
                        rewardDescription.value = document.getString("rewardDescription")
                        rewardPoints.value = document.getLong("rewardPoints")?.toString()
                        redeemLimit.value = document.getLong("redeemLimit")?.toString()
                        imageUrl.value = document.getString("imageUrl")
                    } else {
                        // Handle the case where no document with the given reward name exists
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle any errors here
                }
        }
    }

    private fun loadRewardFromRoom(rewardName: String?) {
        viewModelScope.launch {
            rewardName?.let { name ->
                val reward = rewardDao.getRewardByName(name)
                reward?.let {
                    this@AdminUpdateRewardViewModel.rewardName.value = it.rewardName
                    rewardDescription.value = it.rewardDescription
                    rewardPoints.value = it.rewardPoints.toString()
                    redeemLimit.value = it.redeemLimit.toString()
                    imageBytes.value = it.imageBytes
                }
            }
        }
    }

    fun updateRewardDetailsWithImage(rewardName: String?, selectedImageUri: Uri?) {
        if (isNetworkAvailable) {
            // If connected, handle the update in Firestore and then in Room
            handleFirebaseUpdate(rewardName, selectedImageUri)
        } else {
            // If not connected, handle the update only in Room with appropriate flags
            handleLocalUpdate(rewardName, selectedImageUri)
        }
    }

    private fun handleFirebaseUpdate(rewardName: String?, selectedImageUri: Uri?) {
        if (selectedImageUri != null) {
            val byteArray = uriToByteArray(appContext, selectedImageUri)

            if (byteArray != null) {
                val storageRef = firebaseStorage.reference.child("rewards/${System.currentTimeMillis()}.jpg")
                storageRef.putBytes(byteArray).addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        imageUrl.value = uri.toString()
                        updateRewardDetails(rewardName)
                        // Save updated data to Room DB with isUpdated = 0 since it's updated in Firestore
                        saveRewardToLocalDB(byteArray, rewardName, isAdded = 0, isUpdated = 0)
                    }
                }
            }
        } else {
            updateRewardDetails(rewardName)
            // Save updated data to Room DB without an image and with isUpdated = 0 since it's updated in Firestore
            saveRewardToLocalDB(null, rewardName, isAdded = 0, isUpdated = 0)
        }
    }

    private fun handleLocalUpdate(rewardName: String?, selectedImageUri: Uri?) {
        val byteArray = selectedImageUri?.let { uriToByteArray(appContext, it) }

        viewModelScope.launch {
            rewardName?.let { name ->  // Use 'let' to handle the nullable rewardName
                val currentReward = rewardDao.getRewardByName(name)
                if (currentReward?.isAdded == 1) {
                    // If the reward was added offline and not yet synced, don't mark it as updated.
                    saveRewardToLocalDB(byteArray, name, isAdded = 1, isUpdated = 0)
                } else {
                    // If the reward was not added offline, mark it as updated.
                    saveRewardToLocalDB(byteArray, name, isAdded = 0, isUpdated = 1)
                }
            }
        }
    }


    private fun saveRewardToLocalDB(imageBytes: ByteArray?, rewardName: String?, isAdded: Int, isUpdated: Int) {
        viewModelScope.launch {
            rewardName?.let { name -> // Use 'let' to handle the nullable rewardName
                val existingReward = rewardDao.getRewardByName(name)
                val imageToSave = imageBytes ?: existingReward?.imageBytes
                val imageUrlToSave = if (!isNetworkAvailable && imageBytes != null) "changed" else existingReward?.imageUrl

                val reward = Reward(
                    rewardName = name,
                    imageBytes = imageToSave,
                    imageUrl = imageUrlToSave,
                    rewardDescription = rewardDescription.value ?: "",  // Provide default value in case of null
                    redeemLimit = redeemLimit.value?.toInt() ?: 0,
                    rewardPoints = rewardPoints.value?.toInt() ?: 0,
                    isAdded = isAdded,
                    isUpdated = isUpdated,
                )

                rewardDao.update(reward)
                updateResult.postValue(true)

            }
        }
    }



    fun uriToByteArray(context: Context, imageUri: Uri): ByteArray? {
        return context.contentResolver.openInputStream(imageUri)?.use {
            it.readBytes()
        }
    }

    private fun updateRewardDetails(currentRewardName: String?) {
        currentRewardName?.let {
            val updatedData = hashMapOf(
                "rewardName" to rewardName.value,
                "rewardDescription" to rewardDescription.value,
                "rewardPoints" to rewardPoints.value?.toInt(),
                "redeemLimit" to redeemLimit.value?.toInt(),
                "imageUrl" to imageUrl.value
            )

            db.collection("Rewards")
                .whereEqualTo("rewardName", it)
                .limit(1)  // Limiting to one document since reward names should be unique
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.documents.isNotEmpty()) {
                        val document = documents.documents[0]
                        document.reference.update(updatedData as Map<String, Any>)
                            .addOnSuccessListener {
                                updateResult.postValue(true)
                            }
                            .addOnFailureListener {
                                updateResult.postValue(false)
                            }
                    } else {
                        // Handle the case where no document with the given reward name exists
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle any errors here
                }
        }
    }


    fun checkRewardNameExists(currentRewardName: String?, newRewardName: String, callback: (Boolean) -> Unit) {
        if(isNetworkAvailable){
            db.collection("Rewards")
                .get()
                .addOnSuccessListener { documents ->
                    val existingNames = documents.mapNotNull { it.getString("rewardName") }.filter { it != currentRewardName }
                    callback(newRewardName in existingNames)
                }
                .addOnFailureListener {
                    updateResult.postValue(false)
                }
        } else {
            // If no connection, check using Room DB
            viewModelScope.launch {
                val count = if (currentRewardName != null) {
                    rewardDao.countByNameExcludingCurrent(newRewardName, currentRewardName)
                } else {
                    rewardDao.countByName(newRewardName)
                }
                callback(count > 0) // If count > 0, the reward name exists in Room DB, else it doesn't
            }
        }

    }

    override fun onCleared() {
        super.onCleared()
        appContext.unregisterReceiver(networkReceiver)
    }

}