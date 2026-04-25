package com.example.moneypad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadPartScreen(
    storyId: String,
    partId: String,
    onNavigateBack: () -> Unit,
    viewModel: StoryViewModel
) {
    val story by viewModel.currentStory.collectAsState()
    val parts by viewModel.currentParts.collectAsState()

    LaunchedEffect(storyId) {
        viewModel.getStoryById(storyId)
        viewModel.recordRead(storyId)
    }

    val part = if (story?.id == storyId) parts.find { it.id == partId } else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(part?.title ?: "Reading") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        part?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = it.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = it.content,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "End of ${it.title}",
                    fontSize = 14.sp,
                    color = androidx.compose.ui.graphics.Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
