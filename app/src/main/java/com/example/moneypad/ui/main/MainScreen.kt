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
import com.example.moneypad.ui.components.BannerAdView
import com.example.moneypad.ads.AdManager
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment

@Composable
fun MainScreen(factory: ViewModelFactory, themeViewModel: ThemeViewModel, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val storyViewModel: StoryViewModel = viewModel(factory = factory)
    var showBottomBar by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                if (showBottomBar) {
                    BannerAdView()
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
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Explore.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Explore.route) {
                    ExploreNavigation(storyViewModel, factory, onShowBottomBar = { showBottomBar = it }, rootNavController = navController)
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
                        onShowBottomBar = { showBottomBar = it },
                        rootNavController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryNavigation(storyViewModel: StoryViewModel, onShowBottomBar: (Boolean) -> Unit) {
    val libraryNavController = rememberNavController()

    NavHost(navController = libraryNavController, startDestination = "library_list") {
        composable("library_list") {
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(true) }
            LibraryScreen(
                onNavigateToStoryDetail = { id -> libraryNavController.navigate("story_view/$id") },
                viewModel = storyViewModel
            )
        }
        composable("story_view/{storyId}") { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            StoryViewScreen(
                storyId = storyId,
                onNavigateBack = { libraryNavController.popBackStack() },
                onNavigateToReadPart = { sId, partId ->
                    libraryNavController.navigate("read/$sId/$partId")
                },
                onNavigateToRelatedStory = { relatedId ->
                    libraryNavController.navigate("story_view/$relatedId")
                },
                onNavigateToAuthorProfile = { /* TODO */ },
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
                onNavigateBack = { libraryNavController.popBackStack() },
                onFinishReading = {
                    libraryNavController.popBackStack("story_view/$storyId", inclusive = false)
                },
                onNavigateToPart = { id -> libraryNavController.navigate("read/$storyId/$id") },
                viewModel = storyViewModel
            )
        }
    }
}

@Composable
fun ProfileNavigation(
    factory: ViewModelFactory,
    themeViewModel: ThemeViewModel,
    onLogout: () -> Unit,
    onShowBottomBar: (Boolean) -> Unit,
    rootNavController: androidx.navigation.NavController
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
                onNavigateToPublicProfile = { id -> profileNavController.navigate("author_profile/$id") },
                onNavigateToStoryDetail = { id -> profileNavController.navigate("story_view/$id") },
                onNavigateToReadingListDetail = { id, name -> profileNavController.navigate("reading_list_detail/$id/$name") }
            )
        }
        composable(
            "reading_list_detail/{listId}/{listName}?userId={userId}",
            arguments = listOf(
                androidx.navigation.navArgument("listId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("listName") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            val listName = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("listName") ?: "", "UTF-8")
            val userId = backStackEntry.arguments?.getString("userId")
            val isOwner = userId == null || userId == profileViewModel.currentUserId
            
            ReadingListDetailScreen(
                listId = listId,
                listName = listName,
                onNavigateBack = { profileNavController.popBackStack() },
                onNavigateToStoryDetail = { id -> profileNavController.navigate("story_view/$id") },
                viewModel = storyViewModel,
                isOwner = isOwner
            )
        }
        composable(
            "author_profile/{authorId}?initialTab={initialTab}",
            arguments = listOf(
                androidx.navigation.navArgument("authorId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("initialTab") { type = androidx.navigation.NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val authorId = backStackEntry.arguments?.getString("authorId") ?: ""
            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
            PublicProfileScreen(
                authorId = authorId,
                onNavigateBack = { profileNavController.popBackStack() },
                onNavigateToStoryDetail = { id -> 
                    profileNavController.navigate("story_view/$id")
                },
                onNavigateToAuthorProfile = { id -> 
                    if (id == profileViewModel.currentUserId) {
                        rootNavController.navigate(BottomNavItem.Profile.route) {
                            popUpTo(rootNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        profileNavController.navigate("author_profile/$id")
                    }
                },
                onNavigateToReadingListDetail = { id, name ->
                    profileNavController.navigate("reading_list_detail/$id/$name?userId=$authorId")
                },
                storyViewModel = storyViewModel,
                profileViewModel = profileViewModel,
                initialTab = initialTab
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
                onNavigateToAuthorProfile = { id ->
                    if (id == storyViewModel.currentUserId) {
                        rootNavController.navigate(BottomNavItem.Profile.route) {
                            popUpTo(rootNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        profileNavController.navigate("author_profile/$id")
                    }
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
                onFinishReading = {
                    profileNavController.popBackStack("story_view/$storyId", inclusive = false)
                },
                onNavigateToPart = { id -> profileNavController.navigate("read/$storyId/$id") },
                viewModel = storyViewModel
            )
        }
    }
}

@Composable
fun ExploreNavigation(
    storyViewModel: StoryViewModel, 
    factory: ViewModelFactory, 
    onShowBottomBar: (Boolean) -> Unit,
    rootNavController: androidx.navigation.NavController
) {
    val exploreNavController = rememberNavController()

    NavHost(navController = exploreNavController, startDestination = "explore_list") {
        composable("explore_list") {
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(true) }
            ExploreScreen(
                onNavigateToStoryDetail = { id -> exploreNavController.navigate("story_view/$id") },
                onNavigateToAuthorProfile = { id -> 
                    if (id == storyViewModel.currentUserId) {
                        rootNavController.navigate(BottomNavItem.Profile.route) {
                            popUpTo(rootNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        exploreNavController.navigate("author_profile/$id")
                    }
                },
                onNavigateToNotifications = { exploreNavController.navigate("notifications") },
                viewModel = storyViewModel
            )
        }
        composable("notifications") {
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            NotificationScreen(
                onBack = { exploreNavController.popBackStack() },
                onNavigateToStoryDetail = { id -> exploreNavController.navigate("story_view/$id") },
                onNavigateToAuthorProfile = { id -> 
                    if (id == storyViewModel.currentUserId) {
                        rootNavController.navigate(BottomNavItem.Profile.route) {
                            popUpTo(rootNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        exploreNavController.navigate("author_profile/$id")
                    }
                },
                onNavigateToConversation = { authorId ->
                    exploreNavController.navigate("author_profile/$authorId?initialTab=1")
                },
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
                onNavigateToAuthorProfile = { id -> 
                    if (id == storyViewModel.currentUserId) {
                        rootNavController.navigate(BottomNavItem.Profile.route) {
                            popUpTo(rootNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        exploreNavController.navigate("author_profile/$id")
                    }
                },
                viewModel = storyViewModel
            )
        }
        composable(
            "reading_list_detail/{listId}/{listName}?userId={userId}",
            arguments = listOf(
                androidx.navigation.navArgument("listId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("listName") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val listId = backStackEntry.arguments?.getString("listId") ?: ""
            val listName = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("listName") ?: "", "UTF-8")
            val userId = backStackEntry.arguments?.getString("userId")
            val isOwner = userId == null || userId == storyViewModel.currentUserId
            
            ReadingListDetailScreen(
                listId = listId,
                listName = listName,
                onNavigateBack = { exploreNavController.popBackStack() },
                onNavigateToStoryDetail = { id -> exploreNavController.navigate("story_view/$id") },
                viewModel = storyViewModel,
                isOwner = isOwner
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
                onFinishReading = {
                    exploreNavController.popBackStack("story_view/$storyId", inclusive = false)
                },
                onNavigateToPart = { id -> exploreNavController.navigate("read/$storyId/$id") },
                viewModel = storyViewModel
            )
        }
        composable(
            "author_profile/{authorId}?initialTab={initialTab}",
            arguments = listOf(
                androidx.navigation.navArgument("authorId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("initialTab") { type = androidx.navigation.NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val authorId = backStackEntry.arguments?.getString("authorId") ?: ""
            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
            PublicProfileScreen(
                authorId = authorId,
                onNavigateBack = { exploreNavController.popBackStack() },
                onNavigateToStoryDetail = { id -> exploreNavController.navigate("story_view/$id") },
                onNavigateToAuthorProfile = { id -> 
                    if (id == storyViewModel.currentUserId) {
                        rootNavController.navigate(BottomNavItem.Profile.route) {
                            popUpTo(rootNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        exploreNavController.navigate("author_profile/$id")
                    }
                },
                onNavigateToReadingListDetail = { id, name ->
                    exploreNavController.navigate("reading_list_detail/$id/$name?userId=$authorId")
                },
                storyViewModel = storyViewModel,
                profileViewModel = viewModel(factory = factory),
                initialTab = initialTab
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
                onNavigateToCreateStory = { writeNavController.navigate("setup") },
                onNavigateToStoryDetail = { id -> writeNavController.navigate("detail/$id") },
                viewModel = storyViewModel
            )
        }
        composable(
            route = "setup?storyId={storyId}",
            arguments = listOf(
                androidx.navigation.navArgument("storyId") { type = androidx.navigation.NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId")
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            CreateStoryScreen(
                storyId = storyId,
                onNavigateBack = { writeNavController.popBackStack() },
                onStoryCreated = { id ->
                    if (storyId == null) {
                        // New story created, go to write_part
                        storyViewModel.clearLastCreatedStoryId()
                        writeNavController.navigate("write_part/$id") {
                            popUpTo("setup") { inclusive = true }
                        }
                    } else {
                        // Existing story updated, just go back
                        writeNavController.popBackStack()
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
                onNavigateToWritePart = { id -> 
                    // If id is just storyId (not storyId?partId=...), it's a new part
                    if (!id.contains("?partId=")) {
                        storyViewModel.clearLastCreatedStoryId()
                    }
                    writeNavController.navigate("write_part/$id") 
                },
                onNavigateToEditStory = { id -> writeNavController.navigate("setup?storyId=$id") },
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
                onFinishReading = { writeNavController.popBackStack() },
                onNavigateToPart = { id -> writeNavController.navigate("read/$storyId/$id") },
                viewModel = storyViewModel,
                isPreview = true
            )
        }
    }
}
