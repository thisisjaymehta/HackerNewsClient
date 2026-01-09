package com.neuralquark.hackernewsclient.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neuralquark.hackernewsclient.data.preferences.ThemeMode
import com.neuralquark.hackernewsclient.data.preferences.UserPreferencesData
import com.neuralquark.hackernewsclient.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    val userPreferences: StateFlow<UserPreferencesData> = userPreferencesRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesData()
        )
    
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationsEnabled(enabled)
        }
    }
    
    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(themeMode)
        }
    }
    
    fun setNotificationStartHour(hour: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationStartHour(hour)
        }
    }
    
    fun setNotificationEndHour(hour: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setNotificationEndHour(hour)
        }
    }
}
