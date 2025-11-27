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
    
    private val GOOGLE_DRIVE_CONNECTED_KEY = "google_drive_connected"
    private val GOOGLE_DRIVE_ACCOUNT_EMAIL_KEY = "google_drive_account_email"
    private val GOOGLE_DRIVE_AUTO_BACKUP_KEY = "google_drive_auto_backup"
    private val GOOGLE_DRIVE_LAST_BACKUP_KEY = "google_drive_last_backup"
    
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
    
    fun getGoogleDriveSettings(): GoogleDriveSettings {
        return GoogleDriveSettings(
            isConnected = prefs.getBoolean(GOOGLE_DRIVE_CONNECTED_KEY, false),
            accountEmail = prefs.getString(GOOGLE_DRIVE_ACCOUNT_EMAIL_KEY, "") ?: "",
            autoBackup = prefs.getBoolean(GOOGLE_DRIVE_AUTO_BACKUP_KEY, false),
            lastBackupTime = prefs.getLong(GOOGLE_DRIVE_LAST_BACKUP_KEY, 0L)
        )
    }
    
    fun saveGoogleDriveSettings(settings: GoogleDriveSettings) {
        prefs.edit().apply {
            putBoolean(GOOGLE_DRIVE_CONNECTED_KEY, settings.isConnected)
            putString(GOOGLE_DRIVE_ACCOUNT_EMAIL_KEY, settings.accountEmail)
            putBoolean(GOOGLE_DRIVE_AUTO_BACKUP_KEY, settings.autoBackup)
            putLong(GOOGLE_DRIVE_LAST_BACKUP_KEY, settings.lastBackupTime)
            apply()
        }
    }
}

