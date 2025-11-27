package com.example.selfie.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.selfie.MainActivity
import com.example.selfie.R
import com.example.selfie.data.PhotoRepository
import java.util.Calendar
import java.util.Date

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val repo = PhotoRepository(applicationContext)
        val today = Date()
        
        // Check if there's already a photo taken today
        // This check happens at the scheduled reminder time, so:
        // - If user took a photo before scheduled time and kept it → no notification
        // - If user took a photo before scheduled time but deleted it → notification will be sent
        // - If user never took a photo today → notification will be sent
        val photosToday = repo.getPhotosForDate(today)
        
        if (photosToday.isEmpty()) {
            // Send notification to remind user to take a photo today
            sendNotification(applicationContext)
        }
        
        // Schedule next reminder for tomorrow
        scheduleNextReminder(applicationContext)
        
        return Result.success()
    }
    
    private fun sendNotification(context: Context) {
        // Create notification channel for Android 8.0+
        createNotificationChannel(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = "OPEN_CAMERA"
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Nhắc nhở chụp ảnh")
            .setContentText("Đã đến lúc chụp ảnh selfie hôm nay!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun scheduleNextReminder(context: Context) {
        ReminderScheduler.scheduleDailyReminder(context)
    }
    
    companion object {
        private const val CHANNEL_ID = "reminder_channel"
        private const val CHANNEL_NAME = "Nhắc nhở chụp ảnh"
        private const val CHANNEL_DESCRIPTION = "Thông báo nhắc nhở chụp ảnh selfie hàng ngày"
        private const val NOTIFICATION_ID = 1
        
        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESCRIPTION
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}

