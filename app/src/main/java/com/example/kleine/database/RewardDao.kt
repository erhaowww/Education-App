package com.example.kleine.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RewardDao {

    @Insert
    suspend fun insert(reward: Reward): Long

    @Update
    suspend fun update(reward: Reward)

    @Query("SELECT * FROM reward WHERE rewardName = :name")
    suspend fun getRewardByName(name: String): Reward?

    @Query("SELECT * FROM reward WHERE isDeleted = 0")
    suspend fun getAllRewards(): List<Reward>

    @Query("SELECT COUNT(*) FROM reward WHERE rewardName = :rewardName")
    suspend fun countByName(rewardName: String): Int

    @Query("SELECT COUNT(*) FROM reward WHERE rewardName = :newRewardName AND rewardName != :currentRewardName")
    suspend fun countByNameExcludingCurrent(newRewardName: String, currentRewardName: String): Int

    @Query("SELECT * FROM reward WHERE isAdded = :isAdded")
    suspend fun getUnsyncedRewards(isAdded: Int): List<Reward>

    @Query("SELECT * FROM reward WHERE isUpdated = :isUpdated")
    suspend fun getModifiedRewards(isUpdated: Int): List<Reward>

    @Query("UPDATE reward SET redeemedCount = redeemedCount + 1 WHERE rewardName = :rewardName")
    suspend fun incrementRedeemedCount(rewardName: String)

    // Add other queries or operations as needed.
}
