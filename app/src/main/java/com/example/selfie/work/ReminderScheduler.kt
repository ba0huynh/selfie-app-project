package com.example.selfie.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.os.Build
import com.example.selfie.data.PreferencesManager
import java.util.Calendar

class ReminderScheduler {
    
    companion object {
        private const val REMINDER_ACTION = "com.example.selfie.REMINDER"
        
        fun scheduleDailyReminder(context: Context) {
            val prefs = PreferencesManager(context)
            val settings = prefs.getReminderSettings()
            
            if (!settings.isEnabled) return
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, settings.reminderHour)
                set(Calendar.MINUTE, settings.reminderMinute)
                set(Calendar.SECOND, 0)
                
                // If the time has already passed today, schedule for tomorrow
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            // Use setExactAndAllowWhileIdle for better reliability on modern Android
            // Note: setRepeating is deprecated and may not work reliably
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    // Fallback to setExact if exact alarms are not allowed
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
        
        fun cancelReminder(context: Context) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Schedule the notification checking work
        val workRequest = androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>()
            .build()
        
        androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
    }
}

