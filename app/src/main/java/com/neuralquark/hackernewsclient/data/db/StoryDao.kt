package com.neuralquark.hackernewsclient.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neuralquark.hackernewsclient.data.model.Story
import com.neuralquark.hackernewsclient.data.model.StoryCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    
    @Query("SELECT * FROM stories WHERE category = :category ORDER BY fetchedAt DESC, score DESC")
    fun getStoriesByCategory(category: StoryCategory): Flow<List<Story>>
    
    @Query("SELECT * FROM stories WHERE category = :category ORDER BY fetchedAt DESC, score DESC")
    suspend fun getStoriesByCategorySuspend(category: StoryCategory): List<Story>
    
    @Query("SELECT * FROM stories WHERE id = :id")
    suspend fun getStoryById(id: Long): Story?
    
    @Query("SELECT * FROM stories WHERE id = :id")
    fun getStoryByIdFlow(id: Long): Flow<Story?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<Story>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)
    
    @Query("DELETE FROM stories WHERE category = :category AND id NOT IN (:keepIds)")
    suspend fun deleteOldStories(category: StoryCategory, keepIds: List<Long>)
    
    @Query("SELECT id FROM stories WHERE category = :category")
    suspend fun getStoryIdsByCategory(category: StoryCategory): List<Long>
    
    @Query("UPDATE stories SET notified = 1 WHERE id = :storyId")
    suspend fun markAsNotified(storyId: Long)
    
    @Query("SELECT * FROM stories WHERE score >= :minScore AND notified = 0")
    suspend fun getHighScoreUnnotifiedStories(minScore: Int): List<Story>
    
    @Query("SELECT COUNT(*) FROM stories WHERE category = :category")
    suspend fun getStoryCount(category: StoryCategory): Int
}
