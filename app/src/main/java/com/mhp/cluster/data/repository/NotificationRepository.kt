package com.mhp.cluster.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mhp.cluster.ui.screens.NotificationItem
import androidx.core.content.edit

class NotificationRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "notifications_prefs", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    companion object {
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
    
    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunchComplete() {
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
    
    fun getNotifications(): List<NotificationItem> {
        val json = sharedPreferences.getString(KEY_NOTIFICATIONS, null)
        return if (json != null) {
            val type = object : TypeToken<List<NotificationItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    fun saveNotifications(notifications: List<NotificationItem>) {
        val json = gson.toJson(notifications)
        sharedPreferences.edit() { putString(KEY_NOTIFICATIONS, json) }
    }
    
    fun clearNotifications() {
        sharedPreferences.edit() { remove(KEY_NOTIFICATIONS) }
    }
    
    fun getDefaultNotifications(): List<NotificationItem> {
        return listOf(
            NotificationItem("Car Unlocked", "Just now"),
            NotificationItem("Car Locked", "2 min ago"),
            NotificationItem("Fuel Running Low", "10 min ago"),
            NotificationItem("Engine Service Required", "1 day ago")
        )
    }
} 