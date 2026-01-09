package com.neuralquark.hackernewsclient.di

import android.content.Context
import androidx.room.Room
import com.neuralquark.hackernewsclient.BuildConfig
import com.neuralquark.hackernewsclient.data.api.HackerNewsApi
import com.neuralquark.hackernewsclient.data.db.CommentDao
import com.neuralquark.hackernewsclient.data.db.HackerNewsDatabase
import com.neuralquark.hackernewsclient.data.db.StoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Use BODY level in debug builds for easier debugging, BASIC otherwise
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(HackerNewsApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideHackerNewsApi(retrofit: Retrofit): HackerNewsApi {
        return retrofit.create(HackerNewsApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HackerNewsDatabase {
        return Room.databaseBuilder(
            context,
            HackerNewsDatabase::class.java,
            "hackernews_db"
        )
            .addMigrations(HackerNewsDatabase.MIGRATION_1_2)
            // Note: fallbackToDestructiveMigration is used during initial development phase.
            // This means if a migration fails, user data will be deleted.
            // For production release, consider proper migration strategies instead.
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideStoryDao(database: HackerNewsDatabase): StoryDao {
        return database.storyDao()
    }
    
    @Provides
    fun provideCommentDao(database: HackerNewsDatabase): CommentDao {
        return database.commentDao()
    }
}
