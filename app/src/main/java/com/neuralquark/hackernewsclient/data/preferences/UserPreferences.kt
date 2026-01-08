package com.neuralquark.hackernewsclient.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    MATERIAL_YOU
}

data class UserPreferencesData(
    val notificationsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private object PreferencesKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
    
    val userPreferences: Flow<UserPreferencesData> = context.dataStore.data.map { preferences ->
        UserPreferencesData(
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            themeMode = try {
                ThemeMode.valueOf(preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            } catch (_: Exception) {
                ThemeMode.SYSTEM
            }
        )
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }
}
