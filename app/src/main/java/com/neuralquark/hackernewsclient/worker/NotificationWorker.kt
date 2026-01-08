package com.neuralquark.hackernewsclient.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neuralquark.hackernewsclient.data.repository.HackerNewsRepository
import com.neuralquark.hackernewsclient.util.NotificationUtils
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
    
    override suspend fun doWork(): Result {
        return try {
            val highScoreStories = repository.getHighScoreUnnotifiedStories(
                NewsSyncWorker.HIGH_SCORE_THRESHOLD
            )
            
            highScoreStories.forEach { story ->
                NotificationUtils.sendStoryNotification(applicationContext, story)
                repository.markStoryAsNotified(story.id)
            }
            
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
