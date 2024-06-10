package com.example.kleine.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QuizHistoryDao {

    @Insert
    suspend fun insertQuizHistory(quizHistory: QuizHistory)

    @Query("SELECT * FROM quiz_history WHERE userId = :userDocId ORDER BY date DESC")
    suspend fun getAllQuizHistory(userDocId: String): List<QuizHistory>
}
