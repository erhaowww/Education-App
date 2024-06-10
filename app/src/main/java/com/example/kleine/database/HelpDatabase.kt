package com.example.kleine.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [QuizHistory::class, RewardHistory::class, Reward::class], version = 8, exportSchema = false)
abstract class HelpDatabase : RoomDatabase() {

    abstract fun quizHistoryDao(): QuizHistoryDao
    abstract fun rewardHistoryDao(): RewardHistoryDao

    abstract fun rewardDao(): RewardDao


    companion object {
        @Volatile
        private var INSTANCE: HelpDatabase? = null

        fun getDatabase(context: Context): HelpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HelpDatabase::class.java,
                    "help_database"
                )
                    .fallbackToDestructiveMigration() // Add this line for destructive migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
