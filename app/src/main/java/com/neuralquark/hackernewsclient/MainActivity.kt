package com.neuralquark.hackernewsclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.neuralquark.hackernewsclient.data.preferences.UserPreferencesRepository
import com.neuralquark.hackernewsclient.ui.navigation.HackerNewsNavGraph
import com.neuralquark.hackernewsclient.ui.theme.HackerNewsClientTheme
import com.neuralquark.hackernewsclient.worker.NewsSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result: if denied, notifications won't work but app continues normally
        // Users can enable notifications later via system settings if needed
        if (!isGranted) {
            android.util.Log.d("MainActivity", "Notification permission denied. Notifications will be disabled.")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Get story ID from notification intent
        val storyIdFromNotification = intent.getLongExtra("story_id", -1L)
            .takeIf { it != -1L }
        
        setContent {
            val userPreferences by userPreferencesRepository.userPreferences.collectAsState(
                initial = com.neuralquark.hackernewsclient.data.preferences.UserPreferencesData()
            )
            
            HackerNewsClientTheme(themeMode = userPreferences.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    HackerNewsNavGraph(
                        navController = navController,
                        initialStoryId = storyIdFromNotification
                    )
                }
            }
        }
        
        // Trigger immediate sync when app opens
        NewsSyncWorker.enqueueOnce(this)
    }
}