package com.neuralquark.hackernewsclient.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.neuralquark.hackernewsclient.data.model.Comment
import com.neuralquark.hackernewsclient.data.model.Story

@Database(
    entities = [Story::class, Comment::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HackerNewsDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao
    abstract fun commentDao(): CommentDao
    
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add orderIndex column with default value 0
                db.execSQL("ALTER TABLE stories ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
