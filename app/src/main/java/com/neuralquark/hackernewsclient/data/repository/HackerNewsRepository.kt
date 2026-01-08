package com.neuralquark.hackernewsclient.data.repository

import com.neuralquark.hackernewsclient.data.api.HackerNewsApi
import com.neuralquark.hackernewsclient.data.db.CommentDao
import com.neuralquark.hackernewsclient.data.db.StoryDao
import com.neuralquark.hackernewsclient.data.model.Comment
import com.neuralquark.hackernewsclient.data.model.Story
import com.neuralquark.hackernewsclient.data.model.StoryCategory
import com.neuralquark.hackernewsclient.data.model.toComment
import com.neuralquark.hackernewsclient.data.model.toStory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HackerNewsRepository @Inject constructor(
    private val api: HackerNewsApi,
    private val storyDao: StoryDao,
    private val commentDao: CommentDao
) {
    
    // Track new stories that arrived while user is viewing list
    private val _newStoriesAvailable = MutableStateFlow<Map<StoryCategory, Int>>(emptyMap())
    val newStoriesAvailable: StateFlow<Map<StoryCategory, Int>> = _newStoriesAvailable
    
    fun getStoriesByCategory(category: StoryCategory): Flow<List<Story>> {
        return storyDao.getStoriesByCategory(category)
    }
    
    suspend fun getStoryById(id: Long): Story? {
        return storyDao.getStoryById(id)
    }
    
    fun getStoryByIdFlow(id: Long): Flow<Story?> {
        return storyDao.getStoryByIdFlow(id)
    }
    
    fun getCommentsForStory(storyId: Long): Flow<List<Comment>> {
        return commentDao.getCommentsForStory(storyId)
    }
    
    /**
     * Refresh stories from the API for a specific category
     * Returns the count of new stories that were added
     */
    suspend fun refreshStories(
        category: StoryCategory,
        limit: Int = 50,
        offset: Int = 0
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val allStoryIds = when (category) {
                StoryCategory.TOP -> api.getTopStories()
                StoryCategory.NEW -> api.getNewStories()
                StoryCategory.BEST -> api.getBestStories()
                StoryCategory.ASK -> api.getAskStories()
                StoryCategory.SHOW -> api.getShowStories()
                StoryCategory.JOB -> api.getJobStories()
            }
            
            val storyIds = allStoryIds.drop(offset).take(limit)
            
            // Get existing story IDs
            val existingIds = storyDao.getStoryIdsByCategory(category).toSet()
            
            // Fetch story details in parallel, preserving order with index
            val stories = coroutineScope {
                storyIds.mapIndexed { index, id ->
                    async {
                        try {
                            api.getItem(id)?.toStory(category, offset + index)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
            
            // Count new stories
            val newStories = stories.filter { it.id !in existingIds }
            
            // Save to database
            storyDao.insertStories(stories)
            
            // Only clean up old stories on initial refresh (offset = 0)
            if (offset == 0) {
                storyDao.deleteOldStories(category, allStoryIds.take(limit * 2))
            }
            
            // Update new stories available count
            if (newStories.isNotEmpty() && offset == 0) {
                val currentMap = _newStoriesAvailable.value.toMutableMap()
                currentMap[category] = (currentMap[category] ?: 0) + newStories.size
                _newStoriesAvailable.value = currentMap
            }
            
            Result.success(newStories.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Load more stories (pagination)
     */
    suspend fun loadMoreStories(category: StoryCategory): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val currentCount = storyDao.getStoryCount(category)
            refreshStories(category, limit = 30, offset = currentCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun clearNewStoriesCount(category: StoryCategory) {
        val currentMap = _newStoriesAvailable.value.toMutableMap()
        currentMap.remove(category)
        _newStoriesAvailable.value = currentMap
    }
    
    /**
     * Fetch comments for a story
     */
    suspend fun refreshComments(storyId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val story = api.getItem(storyId) ?: return@withContext Result.failure(Exception("Story not found"))
            val commentIds = story.kids ?: return@withContext Result.success(Unit)
            
            // Recursively fetch comments
            val comments = mutableListOf<Comment>()
            fetchCommentsRecursively(storyId, commentIds, comments, maxDepth = 3)
            
            // Save to database
            commentDao.insertComments(comments)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun fetchCommentsRecursively(
        storyId: Long,
        commentIds: List<Long>,
        accumulator: MutableList<Comment>,
        maxDepth: Int,
        currentDepth: Int = 0
    ) {
        if (currentDepth >= maxDepth || commentIds.isEmpty()) return
        
        coroutineScope {
            commentIds.take(20).map { id -> // Limit to avoid too many requests
                async {
                    try {
                        val item = api.getItem(id)
                        val comment = item?.toComment(storyId)
                        if (comment != null) {
                            synchronized(accumulator) {
                                accumulator.add(comment)
                            }
                            // Recursively fetch child comments
                            if (item.kids != null && item.kids.isNotEmpty()) {
                                fetchCommentsRecursively(
                                    storyId,
                                    item.kids,
                                    accumulator,
                                    maxDepth,
                                    currentDepth + 1
                                )
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore individual comment failures
                    }
                }
            }.awaitAll()
        }
    }
    
    /**
     * Get high score stories that haven't been notified
     */
    suspend fun getHighScoreUnnotifiedStories(minScore: Int = 500): List<Story> {
        return storyDao.getHighScoreUnnotifiedStories(minScore)
    }
    
    suspend fun markStoryAsNotified(storyId: Long) {
        storyDao.markAsNotified(storyId)
    }
    
    suspend fun hasStoriesInCache(category: StoryCategory): Boolean {
        return storyDao.getStoryCount(category) > 0
    }
    
    /**
     * Get all stories for a category (for navigation between stories)
     */
    suspend fun getStoriesListByCategory(category: StoryCategory): List<Story> {
        return storyDao.getStoriesByCategorySuspend(category)
    }
}
