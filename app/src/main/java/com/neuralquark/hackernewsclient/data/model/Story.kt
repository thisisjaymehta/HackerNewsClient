package com.neuralquark.hackernewsclient.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a story from Hacker News
 */
@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: Long,
    val title: String,
    val url: String?,
    val by: String,
    val score: Int,
    val time: Long,
    val descendants: Int, // comment count
    val type: String,
    val text: String?, // for Ask HN, etc.
    val category: StoryCategory,
    val fetchedAt: Long = System.currentTimeMillis(),
    val notified: Boolean = false
)

enum class StoryCategory {
    TOP,
    NEW,
    BEST,
    ASK,
    SHOW,
    JOB
}
