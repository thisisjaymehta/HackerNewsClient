package com.neuralquark.hackernewsclient.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neuralquark.hackernewsclient.data.preferences.UserPreferencesRepository
import com.neuralquark.hackernewsclient.data.repository.HackerNewsRepository
import com.neuralquark.hackernewsclient.util.NotificationUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Worker that sends scheduled notifications for high score stories
 * This is triggered when we're outside the notification window (9 AM - 8 PM)
 */
@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: HackerNewsRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            // Check if notifications are enabled in settings
            val preferences = userPreferencesRepository.userPreferences.first()
            if (!preferences.notificationsEnabled) {
                return Result.success()
            }
            
            val highScoreStories = repository.getHighScoreUnnotifiedStories(
                NewsSyncWorker.HIGH_SCORE_THRESHOLD
            )
            
            highScoreStories.forEach { story ->
                NotificationUtils.sendStoryNotification(applicationContext, story)
                repository.markStoryAsNotified(story.id)
            }
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorker", "Failed to send notifications", e)
            Result.retry()
        }
    }
}
