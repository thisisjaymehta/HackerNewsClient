package com.neuralquark.hackernewsclient.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a comment on a Hacker News story
 */
@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = Story::class,
            parentColumns = ["id"],
            childColumns = ["storyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["storyId"]), Index(value = ["parent"])]
)
data class Comment(
    @PrimaryKey val id: Long,
    val storyId: Long,
    val parent: Long,
    val by: String?,
    val text: String?,
    val time: Long,
    val deleted: Boolean = false,
    val dead: Boolean = false,
    val kids: List<Long> = emptyList()
)
