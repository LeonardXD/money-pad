package com.example.moneypad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneypad.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    onNavigateToCreateStory: () -> Unit,
    onNavigateToStoryDetail: (String) -> Unit,
    viewModel: StoryViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Published", "Drafts")

    val publishedStories by viewModel.myPublishedStories.collectAsState()
    val draftStories by viewModel.myDraftStories.collectAsState()
    
    val currentStories = if (selectedTab == 0) publishedStories else draftStories

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("My Stories") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateStory,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Story")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SecondaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (currentStories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0) "No published stories yet." else "No drafts found.",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(currentStories) { story ->
                        if (selectedTab == 0) {
                            PublishedStoryCard(story = story, onClick = { onNavigateToStoryDetail(story.id) })
                        } else {
                            DraftStoryCard(
                                story = story,
                                onClick = { onNavigateToStoryDetail(story.id) },
                                onEdit = { onNavigateToStoryDetail(story.id) },
                                onDelete = { /* TODO: Implement delete in viewmodel */ }
                            )
                        }
                    }
                }
            }
        }
    }
}
