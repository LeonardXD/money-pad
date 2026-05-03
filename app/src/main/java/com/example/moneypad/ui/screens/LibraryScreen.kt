package com.example.moneypad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.moneypad.data.model.Story
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun LibraryNavigation(storyViewModel: StoryViewModel, onShowBottomBar: (Boolean) -> Unit) {
    val libraryNavController = rememberNavController()

    NavHost(navController = libraryNavController, startDestination = "library_list") {
        composable("library_list") {
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(true) }
            LibraryScreen(
                viewModel = storyViewModel,
                onNavigateToStoryDetail = { id -> libraryNavController.navigate("story_view/$id") },
                onNavigateToAlbumDetail = { albumId, albumName -> 
                    libraryNavController.navigate("album_detail/$albumId/$albumName")
                }
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
                onNavigateToAuthorProfile = { id -> 
                    // From library, we don't have easy access to rootNavController 
                    // unless we pass it down. But we can at least try to go to public profile.
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
                onNavigateBack = { libraryNavController.popBackStack() },
                onNavigateToPart = { id -> libraryNavController.navigate("read/$storyId/$id") },
                viewModel = storyViewModel
            )
        }
        composable("album_detail/{albumId}/{albumName}") { backStackEntry ->
            androidx.compose.runtime.LaunchedEffect(Unit) { onShowBottomBar(false) }
            val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
            val albumName = backStackEntry.arguments?.getString("albumName") ?: ""
            AlbumDetailScreen(
                albumId = albumId,
                albumName = albumName,
                onNavigateBack = { libraryNavController.popBackStack() },
                onNavigateToStoryDetail = { id -> libraryNavController.navigate("story_view/$id") },
                viewModel = storyViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: StoryViewModel,
    onNavigateToStoryDetail: (String) -> Unit,
    onNavigateToAlbumDetail: (String, String) -> Unit
) {
    val libraryStories by viewModel.libraryStories.collectAsState()
    val albums by viewModel.albums.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Offline", "Albums")

    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    if (showCreateAlbumDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAlbumDialog = false },
            title = { Text("New Album") },
            text = {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    label = { Text("Album Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newAlbumName.isNotBlank()) {
                            viewModel.createAlbum(newAlbumName)
                            newAlbumName = ""
                            showCreateAlbumDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAlbumDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Library", fontWeight = FontWeight.Bold) }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = { showCreateAlbumDialog = true }) {
                    Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Create Album")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                Text(
                    text = "${libraryStories.size} / 15 stories downloaded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (libraryStories.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Your library is empty. Download stories to read them offline.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(libraryStories) { story ->
                            LibraryStoryItem(
                                story = story,
                                onClick = { onNavigateToStoryDetail(story.id) },
                                onRemove = { viewModel.removeStoryFromLibrary(story.id) }
                            )
                        }
                    }
                }
            } else {
                if (albums.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No albums created yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(albums) { album ->
                            AlbumItem(
                                album = album,
                                onClick = { onNavigateToAlbumDetail(album.id, album.name) },
                                onDelete = { viewModel.deleteAlbum(album.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumItem(
    album: com.example.moneypad.data.model.Album,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Album?") },
            text = { Text("Are you sure you want to delete '${album.name}'? The stories inside won't be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (album.description.isNotBlank()) {
                    Text(
                        text = album.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Album",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}
@Composable
fun LibraryStoryItem(
    story: Story,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove from Library?") },
            text = { Text("Are you sure you want to remove '${story.title}' from your library?") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove()
                    showRemoveDialog = false
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image
            val placeholderCover = "https://picsum.photos/seed/${story.id}/200/300"
            AsyncImage(
                model = story.coverImageUrl?.ifBlank { placeholderCover } ?: placeholderCover,
                contentDescription = "Cover for ${story.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp, 90.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Story Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "By ${story.authorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Remove Button
            IconButton(onClick = { showRemoveDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove story",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
