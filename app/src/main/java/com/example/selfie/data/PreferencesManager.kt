package com.example.selfie.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "selfie_prefs",
        Context.MODE_PRIVATE
    )
    
    private val REMINDER_ENABLED_KEY = "reminder_enabled"
    private val REMINDER_HOUR_KEY = "reminder_hour"
    private val REMINDER_MINUTE_KEY = "reminder_minute"
    
    fun getReminderSettings(): ReminderSettings {
        return ReminderSettings(
            isEnabled = prefs.getBoolean(REMINDER_ENABLED_KEY, true),
            reminderHour = prefs.getInt(REMINDER_HOUR_KEY, 8),
            reminderMinute = prefs.getInt(REMINDER_MINUTE_KEY, 0)
        )
    }
    
    fun saveReminderSettings(settings: ReminderSettings) {
        prefs.edit().apply {
            putBoolean(REMINDER_ENABLED_KEY, settings.isEnabled)
            putInt(REMINDER_HOUR_KEY, settings.reminderHour)
            putInt(REMINDER_MINUTE_KEY, settings.reminderMinute)
            apply()
        }
    }
}

