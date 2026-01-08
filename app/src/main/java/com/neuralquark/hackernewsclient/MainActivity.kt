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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.neuralquark.hackernewsclient.ui.navigation.HackerNewsNavGraph
import com.neuralquark.hackernewsclient.ui.navigation.Screen
import com.neuralquark.hackernewsclient.ui.theme.HackerNewsClientTheme
import com.neuralquark.hackernewsclient.worker.NewsSyncWorker
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean ->
        // Permission result handled
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
            HackerNewsClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var initialStoryHandled by remember { mutableStateOf(false) }
                    
                    HackerNewsNavGraph(
                        navController = navController,
                        initialStoryId = if (!initialStoryHandled) storyIdFromNotification else null
                    )
                    
                    // Handle navigation to story from notification
                    LaunchedEffect(storyIdFromNotification) {
                        if (storyIdFromNotification != null && !initialStoryHandled) {
                            navController.navigate(Screen.StoryDetail.createRoute(storyIdFromNotification)) {
                                launchSingleTop = true
                            }
                            initialStoryHandled = true
                        }
                    }
                }
            }
        }
        
        // Trigger immediate sync when app opens
        NewsSyncWorker.enqueueOnce(this)
    }
}