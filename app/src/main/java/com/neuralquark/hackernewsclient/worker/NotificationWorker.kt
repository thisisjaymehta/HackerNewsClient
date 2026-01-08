package com.neuralquark.hackernewsclient.worker

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
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neuralquark.hackernewsclient.MainActivity
import com.neuralquark.hackernewsclient.R
import com.neuralquark.hackernewsclient.data.model.Story
import com.neuralquark.hackernewsclient.data.repository.HackerNewsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker that sends scheduled notifications for high score stories
 * This is triggered when we're outside the notification window (9 AM - 8 PM)
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: HackerNewsRepository
) : CoroutineWorker(context, params) {
    
    companion object {
        const val CHANNEL_ID = "high_score_stories"
    }
    
    override suspend fun doWork(): Result {
        return try {
            val highScoreStories = repository.getHighScoreUnnotifiedStories(
                NewsSyncWorker.HIGH_SCORE_THRESHOLD
            )
            
            highScoreStories.forEach { story ->
                sendNotification(story)
                repository.markStoryAsNotified(story.id)
            }
            
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
    
    private fun sendNotification(story: Story) {
        createNotificationChannel()
        
        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("story_id", story.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            story.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔥 Trending on HN: ${story.score} points")
            .setContentText(story.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(story.title))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(applicationContext)
            .notify(story.id.toInt(), notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "High Score Stories"
            val descriptionText = "Notifications for stories with high votes"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = applicationContext.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
