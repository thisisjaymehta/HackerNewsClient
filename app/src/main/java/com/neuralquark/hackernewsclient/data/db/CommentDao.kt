package com.neuralquark.hackernewsclient.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.neuralquark.hackernewsclient.data.model.Comment
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {
    
    @Query("SELECT * FROM comments WHERE storyId = :storyId ORDER BY time ASC")
    fun getCommentsForStory(storyId: Long): Flow<List<Comment>>
    
    @Query("SELECT * FROM comments WHERE storyId = :storyId ORDER BY time ASC")
    suspend fun getCommentsForStorySuspend(storyId: Long): List<Comment>
    
    @Query("SELECT * FROM comments WHERE id = :id")
    suspend fun getCommentById(id: Long): Comment?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<Comment>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)
    
    @Query("DELETE FROM comments WHERE storyId = :storyId")
    suspend fun deleteCommentsForStory(storyId: Long)
    
    @Query("SELECT id FROM comments WHERE storyId = :storyId")
    suspend fun getCommentIdsForStory(storyId: Long): List<Long>
}
