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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuralquark.hackernewsclient.MainActivity
import com.neuralquark.hackernewsclient.R
import com.neuralquark.hackernewsclient.data.model.Story
import com.neuralquark.hackernewsclient.data.model.StoryCategory
import com.neuralquark.hackernewsclient.data.repository.HackerNewsRepository
import com.neuralquark.hackernewsclient.util.TimeUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class NewsSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: HackerNewsRepository
) : CoroutineWorker(context, params) {
    
    companion object {
        const val WORK_NAME = "news_sync_worker"
        const val NOTIFICATION_WORK_NAME = "notification_worker"
        const val CHANNEL_ID = "high_score_stories"
        const val NOTIFICATION_ID = 1001
        const val HIGH_SCORE_THRESHOLD = 500
        
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<NewsSyncWorker>(
                15, TimeUnit.MINUTES
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
        
        fun enqueueOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<NewsSyncWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_once",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            // Sync all categories
            StoryCategory.entries.forEach { category ->
                repository.refreshStories(category, limit = 30)
            }
            
            // Check for high score stories and send notifications
            checkAndNotifyHighScoreStories()
            
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
    
    private suspend fun checkAndNotifyHighScoreStories() {
        val highScoreStories = repository.getHighScoreUnnotifiedStories(HIGH_SCORE_THRESHOLD)
        
        if (highScoreStories.isEmpty()) return
        
        // Check if we're in notification window
        if (TimeUtils.isWithinNotificationHours()) {
            // Send notification immediately
            highScoreStories.forEach { story ->
                sendNotification(story)
                repository.markStoryAsNotified(story.id)
            }
        } else {
            // Schedule notification for 9 AM
            scheduleNotificationForMorning()
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
    
    private fun scheduleNotificationForMorning() {
        val delay = TimeUtils.getDelayUntilNotificationWindow()
        
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            NOTIFICATION_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
