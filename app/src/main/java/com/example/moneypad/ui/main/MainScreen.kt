package com.example.moneypad.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.moneypad.ui.ViewModelFactory
import com.example.moneypad.ui.screens.*
import com.example.moneypad.ui.theme.ThemeViewModel

@Composable
fun MainScreen(factory: ViewModelFactory, themeViewModel: ThemeViewModel, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val storyViewModel: StoryViewModel = viewModel(factory = factory)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Explore.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Explore.route) {
                ExploreNavigation(storyViewModel, factory)
            }
            composable(BottomNavItem.Write.route) {
                WriteNavigation(storyViewModel)
            }
            composable(BottomNavItem.Earnings.route) {
                EarningsScreen(viewModel(factory = factory))
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel(factory = factory),
                    themeViewModel = themeViewModel,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun ExploreNavigation(storyViewModel: StoryViewModel, factory: ViewModelFactory) {
    val exploreNavController = rememberNavController()

    NavHost(navController = exploreNavController, startDestination = "explore_list") {
        composable("explore_list") {
            ExploreScreen(
                onNavigateToStoryDetail = { id -> exploreNavController.navigate("story_view/$id") },
                onNavigateToAuthorProfile = { id -> exploreNavController.navigate("author_profile/$id") },
                viewModel = storyViewModel
            )
        }
        composable("story_view/{storyId}") { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            StoryViewScreen(
                storyId = storyId,
                onNavigateBack = { exploreNavController.popBackStack() },
                onNavigateToReadPart = { sId, partId ->
                    exploreNavController.navigate("read/$sId/$partId")
                },
                onNavigateToRelatedStory = { relatedId ->
                    exploreNavController.navigate("story_view/$relatedId")
                },
                viewModel = storyViewModel
            )
        }
        composable("read/{storyId}/{partId}") { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            val partId = backStackEntry.arguments?.getString("partId") ?: ""
            ReadPartScreen(
                storyId = storyId,
                partId = partId,
                onNavigateBack = { exploreNavController.popBackStack() },
                viewModel = storyViewModel
            )
        }
        composable("author_profile/{authorId}") { backStackEntry ->
            val authorId = backStackEntry.arguments?.getString("authorId") ?: ""
            PublicProfileScreen(
                authorId = authorId,
                onNavigateBack = { exploreNavController.popBackStack() },
                onNavigateToStoryDetail = { id -> exploreNavController.navigate("story_view/$id") },
                storyViewModel = storyViewModel,
                profileViewModel = viewModel(factory = factory)
            )
        }
    }
}

@Composable
fun WriteNavigation(storyViewModel: StoryViewModel) {
    val writeNavController = rememberNavController()

    NavHost(navController = writeNavController, startDestination = "list") {
        composable("list") {
            WriteScreen(
                onNavigateToCreateStory = { writeNavController.navigate("create") },
                onNavigateToStoryDetail = { id -> writeNavController.navigate("detail/$id") },
                viewModel = storyViewModel
            )
        }
        composable("create") {
            CreateStoryScreen(
                onNavigateBack = { writeNavController.popBackStack() },
                // After creating a story, jump straight to the write-part screen
                onStoryCreated = { storyId ->
                    // Pop "create" off the stack so back from WritePartScreen goes to "list"
                    writeNavController.navigate("write_part/$storyId") {
                        popUpTo("create") { inclusive = true }
                    }
                },
                viewModel = storyViewModel
            )
        }
        composable("detail/{storyId}") { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            StoryDetailScreen(
                storyId = storyId,
                onNavigateBack = { writeNavController.popBackStack() },
                onNavigateToWritePart = { id -> writeNavController.navigate("write_part/$id") },
                viewModel = storyViewModel
            )
        }
        composable("write_part/{storyId}") { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            WritePartScreen(
                storyId = storyId,
                onNavigateBack = { writeNavController.popBackStack() },
                onPartSaved = { writeNavController.popBackStack() },
                viewModel = storyViewModel
            )
        }
    }
}