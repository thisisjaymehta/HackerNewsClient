package com.neuralquark.hackernewsclient.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neuralquark.hackernewsclient.data.model.StoryCategory

class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStoryCategory(category: StoryCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toStoryCategory(value: String): StoryCategory {
        return StoryCategory.valueOf(value)
    }
    
    @TypeConverter
    fun fromLongList(list: List<Long>): String {
        return gson.toJson(list)
    }
    
    @TypeConverter
    fun toLongList(value: String): List<Long> {
        val listType = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
