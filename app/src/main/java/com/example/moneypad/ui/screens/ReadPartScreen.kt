package com.example.moneypad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.geometry.Rect

import androidx.compose.ui.platform.LocalView
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem

import com.example.moneypad.utils.HtmlConverter.parseHtmlToAnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadPartScreen(
    storyId: String,
    partId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPart: (String) -> Unit,
    viewModel: StoryViewModel,
    isPreview: Boolean = false
) {
    val story by viewModel.currentStory.collectAsState()
    val parts by viewModel.currentParts.collectAsState()
    val annotations by viewModel.partAnnotations.collectAsState()

    var showCommentDialog by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    // Use TextFieldValue to track selection reliably
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    
    val showAnnotationMenu = remember(textFieldValue.selection) {
        !textFieldValue.selection.collapsed && textFieldValue.text.isNotEmpty()
    }
    val selectedText = remember(textFieldValue.selection) {
        if (!textFieldValue.selection.collapsed && textFieldValue.text.isNotEmpty()) {
            try {
                textFieldValue.text.substring(textFieldValue.selection.start, textFieldValue.selection.end)
            } catch (e: Exception) { "" }
        } else ""
    }

    // suppression of Compose-side menu
    val emptyToolbar = remember {
        object : TextToolbar {
            override val status: TextToolbarStatus = TextToolbarStatus.Hidden
            override fun hide() {}
            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {}
        }
    }

    LaunchedEffect(storyId, partId) {
        viewModel.getStoryById(storyId)
        if (!isPreview) {
            viewModel.recordRead(storyId)
            viewModel.recordPartRead(storyId, partId)
        }
        viewModel.getAnnotationsForPart(partId)
    }
    
    LaunchedEffect(parts) {
        val part = parts.find { it.id == partId }
        if (part != null) {
            val parsedContent = part.content.parseHtmlToAnnotatedString()
            if (textFieldValue.annotatedString != parsedContent) {
                textFieldValue = TextFieldValue(parsedContent)
            }
        }
    }

    val part = if (story?.id == storyId) parts.find { it.id == partId } else null
    val currentIndex = parts.indexOf(part)
    val prevPart = if (currentIndex > 0) parts[currentIndex - 1] else null
    val nextPart = if (currentIndex in 0 until parts.size - 1) parts[currentIndex + 1] else null

    val scrollState = rememberScrollState()

    // Earnings Feature
    var earnedCoins by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var lastScrollTime by remember { mutableLongStateOf(0L) }

    if (!isPreview) {
        LaunchedEffect(scrollState.value) {
            lastScrollTime = System.currentTimeMillis()
        }

        LaunchedEffect(Unit) {
            while (true) {
                if (System.currentTimeMillis() - lastScrollTime < 1000L) {
                    // 15 seconds to fill. 15s * 10 ticks/s = 150 ticks.
                    progress += (1f / 150f)
                    if (progress >= 1f) {
                        earnedCoins += 1
                        viewModel.earnReaderCoins(1)
                        progress = 0f
                    }
                }
                kotlinx.coroutines.delay(100)
            }
        }
    }

    CompositionLocalProvider(LocalTextToolbar provides emptyToolbar) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
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
                    part?.let { currentPart ->
                        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .verticalScroll(scrollState)
                            ) {
                                Text(
                                    text = currentPart.title,
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
                                    Text(text = "${currentPart.readCount}", fontSize = 12.sp, color = Color.Gray)

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Icon(Icons.Default.Favorite, contentDescription = "Likes", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "${annotations.count { it.type == "LIKE" }}", fontSize = 12.sp, color = Color.Gray)

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Icon(Icons.Default.Comment, contentDescription = "Comments", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "${annotations.count { it.type == "COMMENT" }}", fontSize = 12.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                BasicTextField(
                                    value = textFieldValue,
                                    onValueChange = { textFieldValue = it },
                                    readOnly = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 17.sp,
                                        lineHeight = 28.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                                HorizontalDivider()
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "End of ${currentPart.title}",
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
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                    if (nextPart != null) {
                                        Button(onClick = { onNavigateToPart(nextPart.id) }) {
                                            Text("Next")
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))

                                if (annotations.isNotEmpty()) {
                                    Text("Reactions & Comments", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    annotations.forEach { ann ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "\"${ann.selectedText}\"",
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 2,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        if (ann.type == "LIKE") Icons.Default.Favorite else Icons.Default.Comment,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = if (ann.type == "LIKE") Color.Red else MaterialTheme.colorScheme.secondary
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(ann.username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        if (ann.type == "COMMENT") {
                                                            Text(ann.content ?: "", fontSize = 14.sp)
                                                        } else {
                                                            Text("reacted with a heart", fontSize = 14.sp, color = Color.Gray)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(120.dp))
                            }
                            
                            // Earnings UI Overlay
                            if (!isPreview) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(end = 16.dp, top = 32.dp),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(50.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxSize(),
                                            color = Color(0xFFFFD700),
                                            strokeWidth = 4.dp,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        Text(
                                            text = "+$earnedCoins",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Selection Action Menu
                            if (showAnnotationMenu) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 32.dp)
                                        .fillMaxWidth(0.95f),
                                    shape = RoundedCornerShape(32.dp),
                                    color = MaterialTheme.colorScheme.inverseSurface,
                                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 12.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 12.dp)
                                        ) {
                                            Text(
                                                "\"$selectedText\"",
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FilledIconButton(
                                                onClick = {
                                                    viewModel.addPartAnnotation(partId, selectedText, textFieldValue.selection.start, textFieldValue.selection.end, "LIKE")
                                                    textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.selection.end))
                                                },
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = Color.Red,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(Icons.Default.Favorite, "Heart", modifier = Modifier.size(20.dp))
                                            }
                                            
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            FilledIconButton(
                                                onClick = { showCommentDialog = true },
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(Icons.Default.Comment, "Comment", modifier = Modifier.size(20.dp))
                                            }
                                            
                                            Spacer(modifier = Modifier.width(4.dp))
                                            
                                            IconButton(onClick = { 
                                                textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.selection.end))
                                            }) {
                                                Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.6f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCommentDialog) {
        AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text("Comment on selection") },
            text = {
                Column {
                    Text("\"$selectedText\"", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("What are your thoughts?") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (commentText.isNotBlank()) {
                        viewModel.addPartAnnotation(partId, selectedText, textFieldValue.selection.start, textFieldValue.selection.end, "COMMENT", commentText)
                        showCommentDialog = false
                        commentText = ""
                        textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.selection.end))
                    }
                }) { Text("Post") }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDialog = false }) { Text("Cancel") }
            }
        )
    }
}
