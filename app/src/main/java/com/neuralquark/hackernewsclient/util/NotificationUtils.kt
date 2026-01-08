package com.neuralquark.hackernewsclient.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.neuralquark.hackernewsclient.MainActivity
import com.neuralquark.hackernewsclient.R
import com.neuralquark.hackernewsclient.data.model.Story

/**
 * Utility class for handling notifications
 */
object NotificationUtils {
    
    const val CHANNEL_ID = "high_score_stories"
    
    /**
     * Send a notification for a high-score story
     */
    fun sendStoryNotification(context: Context, story: Story) {
        createNotificationChannel(context)
        
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("story_id", story.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            story.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔥 Trending on HN: ${story.score} points")
            .setContentText(story.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(story.title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context)
            .notify(story.id.toInt(), notification)
    }
    
    /**
     * Create the notification channel for high score stories
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "High Score Stories"
            val descriptionText = "Notifications for stories with high votes"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
