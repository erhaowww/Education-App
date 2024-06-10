package com.example.kleine.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_history")
data class QuizHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val materialName: String,
    val setName: String,
    val score: String,
    val date: Long // You can store the date as a timestamp (Long)
)
