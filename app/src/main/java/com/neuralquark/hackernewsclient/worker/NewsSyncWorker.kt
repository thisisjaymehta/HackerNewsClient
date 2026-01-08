package com.neuralquark.hackernewsclient.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neuralquark.hackernewsclient.data.model.StoryCategory
import com.neuralquark.hackernewsclient.data.repository.HackerNewsRepository
import com.neuralquark.hackernewsclient.util.NotificationUtils
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
                NotificationUtils.sendStoryNotification(applicationContext, story)
                repository.markStoryAsNotified(story.id)
            }
        } else {
            // Schedule notification for 9 AM
            scheduleNotificationForMorning()
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
