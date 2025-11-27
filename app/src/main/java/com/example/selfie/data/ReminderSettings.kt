package com.example.selfie.data

data class ReminderSettings(
    val isEnabled: Boolean = true,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0
)

