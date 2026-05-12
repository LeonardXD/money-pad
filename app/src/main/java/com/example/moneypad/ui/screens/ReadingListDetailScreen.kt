package com.example.moneypad.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.moneypad.data.model.Story
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.ui.components.VerifiedIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListDetailScreen(
    listId: String,
    listName: String,
    onNavigateBack: () -> Unit,
    onNavigateToStoryDetail: (String) -> Unit,
    viewModel: StoryViewModel,
    isOwner: Boolean = true
) {
    val stories by remember(listId) { viewModel.getStoriesForReadingList(listId) }.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(listName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (stories.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("This reading list is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    items(stories) { story ->
                        ReadingListStoryItem(
                            story = story,
                            viewModel = viewModel,
                            onClick = { onNavigateToStoryDetail(story.id) },
                            onRemove = { viewModel.removeStoryFromReadingList(listId, story.id) },
                            isOwner = isOwner
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReadingListStoryItem(
    story: Story,
    viewModel: StoryViewModel,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    isOwner: Boolean
) {
    val author by remember(story.authorId) { viewModel.getUser(story.authorId) }.collectAsState(initial = null)

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "By ${story.authorName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (story.isAuthorVerified || story.authorId == MoneyPadRepository.OFFICIAL_USER_ID) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedIcon(modifier = Modifier.size(30.dp))
                    }
                }
            }

            if (isOwner) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from list",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}