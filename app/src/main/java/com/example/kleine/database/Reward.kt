package com.example.kleine.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward")
data class Reward(
    @PrimaryKey
    val rewardName: String = "",
    var imageBytes: ByteArray? = null,
    val redeemLimit: Int = 0,
    val redeemedCount: Int = 0,
    val rewardDescription: String = "",
    val rewardPoints: Int = 0,
    var isAdded: Int = 0,
    var isUpdated: Int = 0,
    var isDeleted: Int = 0,
    var imageUrl: String? = null
)
