package com.pedometers.motiontracker

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun isFirstLaunch() =
        sharedPreferences.getBoolean("first_launch", true)

    fun setFirstLaunch() {
        sharedPreferences.edit().putBoolean("first_launch", false).apply()
    }

    fun saveUUID(uuid: String) {
        sharedPreferences.edit().putString("uuid", uuid).apply()
    }

    fun getUUID(): String? {
        return sharedPreferences.getString("uuid", null)
    }
}