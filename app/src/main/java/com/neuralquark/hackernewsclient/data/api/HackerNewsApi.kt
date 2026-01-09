package com.neuralquark.hackernewsclient.data.api

import com.neuralquark.hackernewsclient.data.model.HNItemResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for the Hacker News Firebase API
 */
interface HackerNewsApi {
    
    companion object {
        const val BASE_URL = "https://hacker-news.firebaseio.com/v0/"
    }
    
    @GET("topstories.json")
    suspend fun getTopStories(): List<Long>
    
    @GET("newstories.json")
    suspend fun getNewStories(): List<Long>
    
    @GET("beststories.json")
    suspend fun getBestStories(): List<Long>
    
    @GET("askstories.json")
    suspend fun getAskStories(): List<Long>
    
    @GET("showstories.json")
    suspend fun getShowStories(): List<Long>
    
    @GET("jobstories.json")
    suspend fun getJobStories(): List<Long>
    
    @GET("item/{id}.json")
    suspend fun getItem(@Path("id") id: Long): HNItemResponse?
}
