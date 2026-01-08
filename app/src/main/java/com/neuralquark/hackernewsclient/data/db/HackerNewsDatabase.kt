package com.neuralquark.hackernewsclient.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.neuralquark.hackernewsclient.data.model.Comment
import com.neuralquark.hackernewsclient.data.model.Story

@Database(
    entities = [Story::class, Comment::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HackerNewsDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun commentDao(): CommentDao
}
