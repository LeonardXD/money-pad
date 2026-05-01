package com.example.moneypad.ui.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    var showBottomBar by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Explore.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Explore.route) {
                ExploreNavigation(storyViewModel, factory, onShowBottomBar = { showBottomBar = it })
            }
            composable(BottomNavItem.Library.route) {
                LibraryNavigation(storyViewModel, onShowBottomBar = { showBottomBar = it })
            }
            composable(BottomNavItem.Write.route) {
                WriteNavigation(storyViewModel, onShowBottomBar = { showBottomBar = it }, rootNavController = navController)
            }
            composable(BottomNavItem.Earnings.route) {
                androidx.compose.runtime.LaunchedEffect(Unit) { showBottomBar = true }
                EarningsScreen(viewModel(factory = factory))
            }
            composable(BottomNavItem.Profile.route) {
                androidx.compose.runtime.LaunchedEffect(Unit) { showBottomBar = true }
                ProfileNavigation(
                    factory = factory,
                    themeViewModel = themeViewModel,
                    onLogout = onLogout,
                    onShowBottomBar = { showBottomBar = it }
                )
            }
        }
    }
}

@Composable
fun ProfileNavigation(
    factory: ViewModelFactory,
    themeViewModel: ThemeViewModel,
    onLogout: () -> Unit,
    onShowBottomBar: (Boolean) -> Unit
) {
    val profileNavController = rememberNavController()
    val storyViewModel: StoryViewModel = viewModel(factory = factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = factory)

    NavHost(navController = profileNavController, startDestination = "my_profile") {
        composable("my_profile") {
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(true) }
            ProfileScreen(
                viewModel = profileViewModel,
                themeViewModel = themeViewModel,
                onLogout = onLogout,
                onNavigateToPublicProfile = { id -> profileNavController.navigate("author_profile/$id") }
            )
        }
        composable("author_profile/{authorId}") { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val authorId = backStackEntry.arguments?.getString("authorId") ?: ""
            PublicProfileScreen(
                authorId = authorId,
                onNavigateBack = { profileNavController.popBackStack() },
                onNavigateToStoryDetail = { id -> 
                    // For now, we don't have a story view screen in this nav host
                    // But we could add it if needed
                    profileNavController.navigate("story_view/$id")
                },
                storyViewModel = storyViewModel,
                profileViewModel = profileViewModel
            )
        }
        composable("story_view/{storyId}") { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            StoryViewScreen(
                storyId = storyId,
                onNavigateBack = { profileNavController.popBackStack() },
                onNavigateToReadPart = { sId, partId ->
                    profileNavController.navigate("read/$sId/$partId")
                },
                onNavigateToRelatedStory = { relatedId ->
                    profileNavController.navigate("story_view/$relatedId")
                },
                viewModel = storyViewModel
            )
        }
        composable("read/{storyId}/{partId}") { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            val partId = backStackEntry.arguments?.getString("partId") ?: ""
            ReadPartScreen(
                storyId = storyId,
                partId = partId,
                onNavigateBack = { profileNavController.popBackStack() },
                onNavigateToPart = { id -> profileNavController.navigate("read/$storyId/$id") },
                viewModel = storyViewModel
            )
        }
    }
}

@Composable
fun ExploreNavigation(storyViewModel: StoryViewModel, factory: ViewModelFactory, onShowBottomBar: (Boolean) -> Unit) {
    val exploreNavController = rememberNavController()

    NavHost(navController = exploreNavController, startDestination = "explore_list") {
        composable("explore_list") {
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(true) }
            ExploreScreen(
                onNavigateToStoryDetail = { id -> exploreNavController.navigate("story_view/$id") },
                onNavigateToAuthorProfile = { id -> exploreNavController.navigate("author_profile/$id") },
                viewModel = storyViewModel
            )
        }
        composable("story_view/{storyId}") { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
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
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            val partId = backStackEntry.arguments?.getString("partId") ?: ""
            ReadPartScreen(
                storyId = storyId,
                partId = partId,
                onNavigateBack = { exploreNavController.popBackStack() },
                onNavigateToPart = { id -> exploreNavController.navigate("read/$storyId/$id") },
                viewModel = storyViewModel
            )
        }
        composable("author_profile/{authorId}") { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
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
fun WriteNavigation(storyViewModel: StoryViewModel, onShowBottomBar: (Boolean) -> Unit, rootNavController: androidx.navigation.NavController) {
    val writeNavController = rememberNavController()

    NavHost(navController = writeNavController, startDestination = "list") {
        composable("list") {
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(true) }
            WriteScreen(
                onNavigateToCreateStory = { writeNavController.navigate("create") },
                onNavigateToStoryDetail = { id -> writeNavController.navigate("detail/$id") },
                viewModel = storyViewModel
            )
        }
        composable("create") {
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
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
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            StoryDetailScreen(
                storyId = storyId,
                onNavigateBack = { writeNavController.popBackStack() },
                onNavigateToWritePart = { id -> writeNavController.navigate("write_part/$id") },
                viewModel = storyViewModel
            )
        }
        composable(
            route = "write_part/{storyId}?partId={partId}",
            arguments = listOf(
                androidx.navigation.navArgument("storyId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("partId") { type = androidx.navigation.NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            val partId = backStackEntry.arguments?.getString("partId")
            WritePartScreen(
                storyId = storyId,
                partId = partId,
                onNavigateBack = { writeNavController.popBackStack() },
                onPartSaved = { writeNavController.popBackStack() },
                viewModel = storyViewModel,
                navController = writeNavController
            )
        }
        composable("read/{storyId}/{partId}") { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            val partId = backStackEntry.arguments?.getString("partId") ?: ""
            ReadPartScreen(
                storyId = storyId,
                partId = partId,
                onNavigateBack = { writeNavController.popBackStack() },
                onNavigateToPart = { id -> writeNavController.navigate("read/$storyId/$id") },
                viewModel = storyViewModel,
                isPreview = true
            )
        }
    }
}
