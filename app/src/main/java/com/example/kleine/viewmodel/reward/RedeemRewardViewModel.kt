package com.example.kleine.viewmodel.reward

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.kleine.database.HelpDatabase
import com.example.kleine.database.RewardHistory
import com.example.kleine.model.Reward
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RedeemRewardViewModel(application: Application) : AndroidViewModel(application) {
    val rewards = MutableLiveData<List<Reward>>()
    val userPoints = MutableLiveData<Int>()
    val redemptionSuccessful = MutableLiveData<Boolean>()
    val noEnoughPoints = MutableLiveData<Boolean>()

    private val firestore = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadRewards()
        loadUserPoints()
    }

     fun loadRewards() {
        val rewardsCollection = firestore.collection("Rewards")

        rewardsCollection.addSnapshotListener { snapshot, exception ->
            if (exception != null || snapshot == null) {
                // Handle the error
                return@addSnapshotListener
            }

            val rewardList = mutableListOf<Reward>()
            for (document in snapshot.documents) {
                val reward = document.toObject(Reward::class.java)
                if (reward != null && reward.redeemedCount < reward.redeemLimit) {
                    reward.documentId = document.id // Set the documentId field
                    rewardList.add(reward)
                }
            }

            rewards.value = rewardList
        }
    }

    private fun loadUserPoints() {
        userId?.let {
            val userDocument = firestore.collection("users").document(it)

            userDocument.addSnapshotListener { snapshot, exception ->
                if (exception != null || snapshot == null) {
                    // Handle the error
                    return@addSnapshotListener
                }

                val points = snapshot.getLong("points")?.toInt() ?: 0
                userPoints.value = points
            }
        }
    }

    fun redeemReward(selectedReward: Reward) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocument = firestore.collection("users").document(userId)

        firestore.runTransaction { transaction ->
            // 1. First perform all read operations
            val userSnapshot = transaction.get(userDocument)
            val points = userSnapshot.getLong("points")?.toInt() ?: 0

            if (points < selectedReward.rewardPoints) {
                // Not enough points, return or throw an exception
                throw FirebaseFirestoreException("Not enough points", FirebaseFirestoreException.Code.ABORTED)
            }

            val rewardDocument = firestore.collection("Rewards").document(selectedReward.documentId)
            val redeemedCount = transaction.get(rewardDocument).getLong("redeemedCount")?.toInt() ?: 0

            // 2. Then perform all write operations
            transaction.update(userDocument, "points", points - selectedReward.rewardPoints)
            transaction.update(rewardDocument, "redeemedCount", redeemedCount + 1)

            // Insert reward history into Room
            val rewardHistory = RewardHistory(
                userDocId = userId,
                redeemedDate = System.currentTimeMillis(), // Current timestamp
                rewardName = selectedReward.rewardName,
                rewardDetails = selectedReward.rewardDescription
            )
            insertRewardHistory(rewardHistory)

            val historyData = mapOf(
                "rewardName" to selectedReward.rewardName,
                "redeemedDate" to FieldValue.serverTimestamp(),
                "rewardDetails" to selectedReward.rewardDescription
            )
            transaction.set(userDocument.collection("rewardHistory").document(), historyData)
        }.addOnSuccessListener {
            // Handle success
            redemptionSuccessful.value = true

            // Update redeemed count in Room database
            updateRedeemedCountInRoom(selectedReward.rewardName)
        }.addOnFailureListener { exception ->
            // Handle failure
            Log.e("RedeemReward", "Redemption Failed", exception)
            if (exception is FirebaseFirestoreException && exception.code == FirebaseFirestoreException.Code.ABORTED) {
                noEnoughPoints.value = true
            }
        }
    }

    private fun updateRedeemedCountInRoom(rewardName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = HelpDatabase.getDatabase(getApplication()).rewardDao()
            dao.incrementRedeemedCount(rewardName)
        }
    }


    private fun insertRewardHistory(rewardHistory: RewardHistory) {
        viewModelScope.launch(Dispatchers.IO) {
            val dao = HelpDatabase.getDatabase(getApplication()).rewardHistoryDao()
            dao.insertRewardHistory(rewardHistory)
        }
    }

}
