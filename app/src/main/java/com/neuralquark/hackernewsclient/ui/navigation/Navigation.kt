package com.neuralquark.hackernewsclient.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.neuralquark.hackernewsclient.ui.screens.detail.StoryDetailScreen
import com.neuralquark.hackernewsclient.ui.screens.list.NewsListScreen
import com.neuralquark.hackernewsclient.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object NewsList : Screen("news_list")
    data object StoryDetail : Screen("story_detail/{storyId}") {
        fun createRoute(storyId: Long) = "story_detail/$storyId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun HackerNewsNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.NewsList.route,
    initialStoryId: Long? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.NewsList.route) {
            NewsListScreen(
                onStoryClick = { storyId ->
                    navController.navigate(Screen.StoryDetail.createRoute(storyId))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(
            route = Screen.StoryDetail.route,
            arguments = listOf(
                navArgument("storyId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getLong("storyId") ?: return@composable
            StoryDetailScreen(
                storyId = storyId,
                onBackClick = { navController.popBackStack() },
                onNavigateToStory = { newStoryId ->
                    navController.navigate(Screen.StoryDetail.createRoute(newStoryId)) {
                        popUpTo(Screen.StoryDetail.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
    
    // Handle deep link from notification
    initialStoryId?.let { storyId ->
        navController.navigate(Screen.StoryDetail.createRoute(storyId)) {
            launchSingleTop = true
        }
    }
}
