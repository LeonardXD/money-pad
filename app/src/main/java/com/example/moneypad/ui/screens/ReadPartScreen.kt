package com.example.moneypad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadPartScreen(
    storyId: String,
    partId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPart: (String) -> Unit,
    viewModel: StoryViewModel
) {
    val story by viewModel.currentStory.collectAsState()
    val parts by viewModel.currentParts.collectAsState()

    LaunchedEffect(storyId, partId) {
        viewModel.getStoryById(storyId)
        viewModel.recordRead(storyId)
        viewModel.recordPartRead(storyId, partId)
    }

    val part = if (story?.id == storyId) parts.find { it.id == partId } else null
    val currentIndex = parts.indexOf(part)
    val prevPart = if (currentIndex > 0) parts[currentIndex - 1] else null
    val nextPart = if (currentIndex in 0 until parts.size - 1) parts[currentIndex + 1] else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { }, // Title removed from TopAppBar
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        story?.let { currentStory ->
            if (!currentStory.isPublished && currentStory.authorId != viewModel.currentUserId) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("This content is no longer available.", color = Color.Gray)
                        TextButton(onClick = onNavigateBack) { Text("Go Back") }
                    }
                }
            } else {
                part?.let {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = it.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = "Views", modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "123", fontSize = 12.sp, color = Color.Gray)
                            
                            Spacer(modifier = Modifier.width(16.dp))

                            Icon(Icons.Default.Favorite, contentDescription = "Likes", modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "45", fontSize = 12.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.width(16.dp))

                            Icon(Icons.Default.Comment, contentDescription = "Comments", modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "8", fontSize = 12.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = it.content,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        HorizontalDivider()
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "End of ${it.title}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (prevPart != null) {
                                Button(onClick = { onNavigateToPart(prevPart.id) }) {
                                    Text("Previous")
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f)) // pushes Next to the right if no Prev
                            }
                            if (nextPart != null) {
                                Button(onClick = { onNavigateToPart(nextPart.id) }) {
                                    Text("Next")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
