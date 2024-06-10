package com.example.kleine.database

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SharedPreferencesHelper(context: Context) {

    private val SHARED_PREF_NAME = "user_sharePre"
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)

    fun saveUserId(userId: String) {
        val editor = sharedPreferences.edit()
        editor.putString("USER_ID", userId)
        editor.apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString("USER_ID", null)
    }

    fun printSharedPreferences() {
        val allEntries = sharedPreferences.all
        for ((key, value) in allEntries) {
            Log.d("SharedPreferencesData", "$key: $value")
        }
    }

}
