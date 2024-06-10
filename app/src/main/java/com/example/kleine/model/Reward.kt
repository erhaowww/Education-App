package com.example.kleine.model

data class Reward(
    var documentId: String = "",
    val imageUrl: String = "",
    val rewardName: String = "",
    val rewardDescription: String = "",
    val rewardPoints: Int = 0,
    val redeemLimit: Int = 0,
    val redeemedCount: Int = 0
)
