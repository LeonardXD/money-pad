package com.example.moneypad.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moneypad.data.model.StoryPart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryDetailScreen(
    storyId: String,
    onNavigateBack: () -> Unit,
    onNavigateToWritePart: (String) -> Unit,
    viewModel: StoryViewModel
) {
    val story by viewModel.currentStory.collectAsState()
    val parts by viewModel.currentParts.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(storyId) {
        viewModel.getStoryById(storyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(story?.title ?: "Story Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToWritePart(storyId) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Chapter")
            }
        }
    ) { innerPadding ->
        story?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Story Dashboard",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Table of Contents (${parts.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!it.isPublished) {
                        Button(
                            onClick = { 
                                viewModel.publishStory(it.id)
                                android.widget.Toast.makeText(context, "Story published", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Publish Story")
                        }
                    } else {
                        Button(
                            onClick = { 
                                viewModel.unpublishStory(it.id)
                                android.widget.Toast.makeText(context, "Story unpublished", android.widget.Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Unpublish Story")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (parts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No chapters added yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(parts) { part ->
                            PartItem(
                                part = part, 
                                onEdit = { onNavigateToWritePart("${storyId}?partId=${part.id}") },
                                onDelete = { viewModel.deletePart(part.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartItem(part: StoryPart, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isPublished = true // Mock state

    Surface(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.DragHandle, contentDescription = "Reorder", tint = Color.Gray)
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = part.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (part.isPublished) "Published" else "Unpublished", fontSize = 10.sp, color = Color.Gray)
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
