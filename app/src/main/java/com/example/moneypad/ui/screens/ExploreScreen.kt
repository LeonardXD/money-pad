package com.example.moneypad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneypad.data.model.User
import com.example.moneypad.ui.components.CarouselStoryCard
import com.example.moneypad.ui.components.StoryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onNavigateToStoryDetail: (String) -> Unit,
    onNavigateToAuthorProfile: (String) -> Unit,
    viewModel: StoryViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }

    val genres = listOf(
        "All",
        "Romance",
        "Fantasy",
        "Mystery",
        "Sci-Fi",
        "Horror",
        "Action",
        "LGBTQIA+",
        "Werewolf",
        "New Adult",
        "Short Story",
        "Teen Fiction",
        "Historical Fiction",
        "Paranormal",
        "Humor",
        "Contemporary Lit",
        "Diverse Lit",
        "Thriller",
        "Adventure",
        "Fan Fiction",
        "Non-Fiction",
        "Poetry"
    )

    val stories by viewModel.search(searchQuery, selectedGenre).collectAsState(initial = emptyList())
    val authors by viewModel.searchAuthors(searchQuery).collectAsState(initial = emptyList())
    
    val continueReading by viewModel.continueReadingStories.collectAsState()
    val recommendedStories by viewModel.recommendedStories.collectAsState()
    val updatedStories by viewModel.updatedStories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Explore Money Pad") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search stories or authors...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Genre Tabs
            SecondaryScrollableTabRow(
                selectedTabIndex = genres.indexOf(selectedGenre),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                genres.forEach { genre ->
                    val selected = selectedGenre == genre
                    Tab(
                        selected = selected,
                        onClick = { selectedGenre = genre },
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                        text = {
                            Text(
                                genre,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.primary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (authors.isNotEmpty() && searchQuery.isNotBlank()) {
                    item {
                        Text("Authors", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    items(authors) { author ->
                        AuthorSearchItem(author = author, onClick = { onNavigateToAuthorProfile(author.id) })
                    }
                }

                if (searchQuery.isBlank()) {
                    if (continueReading.isNotEmpty()) {
                        item {
                            Text("Continue Reading", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(continueReading) { (story, progress) ->
                                    CarouselStoryCard(
                                        story = story, 
                                        modifier = Modifier.width(140.dp),
                                        progress = progress,
                                        onClick = { onNavigateToStoryDetail(story.id) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    
                    if (recommendedStories.isNotEmpty()) {
                        item {
                            Text("Stories from genres you like", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recommendedStories.take(10)) { story ->
                                    CarouselStoryCard(
                                        story = story, 
                                        modifier = Modifier.width(140.dp),
                                        onClick = { onNavigateToStoryDetail(story.id) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                val currentStoriesList = if (searchQuery.isNotBlank()) stories else updatedStories

                if (currentStoriesList.isNotEmpty()) {
                    item {
                        Text(if (searchQuery.isBlank()) "Updated Stories" else "Stories", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    val chunkedStories = currentStoriesList.chunked(2)
                    items(chunkedStories) { rowStories ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowStories.forEach { story ->
                                Box(modifier = Modifier.weight(1f)) {
                                    CarouselStoryCard(
                                        story = story, 
                                        onClick = { onNavigateToStoryDetail(story.id) }
                                    )
                                }
                            }
                            if (rowStories.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (currentStoriesList.isEmpty() && authors.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No stories available yet", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorSearchItem(author: User, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(author.username, fontWeight = FontWeight.Bold)
                Text("${author.followers} Followers", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}
