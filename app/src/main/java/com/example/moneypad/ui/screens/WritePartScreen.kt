package com.example.moneypad.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritePartScreen(
    storyId: String,
    partId: String? = null,
    onNavigateBack: () -> Unit,
    onPartSaved: () -> Unit,
    viewModel: StoryViewModel,
    navController: androidx.navigation.NavController? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val parts by viewModel.currentParts.collectAsState()
    val partToEdit = parts.find { it.id == partId }

    var title by remember(partToEdit) { mutableStateOf(partToEdit?.title ?: "") }
    var content by remember(partToEdit) { mutableStateOf(TextFieldValue(partToEdit?.content ?: "")) }
    var isPublished by remember(partToEdit) { mutableStateOf(partToEdit?.isPublished ?: false) }
    var expanded by remember { mutableStateOf(false) }
    var showSaveDraftDialog by remember { mutableStateOf(false) }

    // Track last saved state to avoid showing "Save as Draft" dialog after publishing/saving
    var lastSavedTitle by remember(partToEdit) { mutableStateOf(partToEdit?.title ?: "") }
    var lastSavedContent by remember(partToEdit) { mutableStateOf(partToEdit?.content ?: "") }

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground

    val hasChanges = remember(title, content.text, lastSavedTitle, lastSavedContent) {
        title != lastSavedTitle || content.text != lastSavedContent
    }

    // Save as draft and go back
    fun saveDraftAndExit() {
        viewModel.savePartAsDraft(storyId, title, content.text, partId)
        onNavigateBack()
    }

    // Intercept the system back gesture/button
    BackHandler {
        if (hasChanges && (title.isNotBlank() || content.text.isNotBlank())) {
            showSaveDraftDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Save-draft confirmation dialog
    if (showSaveDraftDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDraftDialog = false },
            title = { Text("Save as Draft?") },
            text = { Text("Do you want to save your chapter as a draft before leaving?") },
            confirmButton = {
                Button(onClick = {
                    showSaveDraftDialog = false
                    saveDraftAndExit()
                }) {
                    Text("Save Draft")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDraftDialog = false
                    onNavigateBack()
                }) {
                    Text("Discard")
                }
            }
        )
    }

    fun applyFormat(tagStart: String, tagEnd: String = tagStart) {
        val text = content.text
        val selection = content.selection
        
        if (!selection.collapsed) {
            val before = text.substring(0, selection.start)
            val selected = text.substring(selection.start, selection.end)
            val after = text.substring(selection.end)
            val newText = "$before$tagStart$selected$tagEnd$after"
            content = content.copy(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(selection.end + tagStart.length + tagEnd.length)
            )
        } else {
            val before = text.substring(0, selection.start)
            val after = text.substring(selection.end)
            val newText = "$before$tagStart$tagEnd$after"
            content = content.copy(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(selection.start + tagStart.length)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges && (title.isNotBlank() || content.text.isNotBlank())) {
                            showSaveDraftDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.text.isNotBlank()) {
                                if (isPublished) {
                                    viewModel.updatePartStatus(storyId, partId ?: "", title, content.text, false)
                                    isPublished = false
                                    lastSavedTitle = title
                                    lastSavedContent = content.text
                                    android.widget.Toast.makeText(context, "Part unpublished", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addPartToStory(storyId, title, content.text, partId, true)
                                    isPublished = true
                                    lastSavedTitle = title
                                    lastSavedContent = content.text
                                    android.widget.Toast.makeText(context, "Part published", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(36.dp)
                    ) {
                        Text(if (isPublished) "Unpublish" else "Publish", fontSize = 14.sp)
                    }

                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save as Draft") },
                                onClick = {
                                    expanded = false
                                    saveDraftAndExit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View as Reader's POV") },
                                onClick = { 
                                    expanded = false 
                                    if (partId != null) {
                                        navController?.navigate("read/$storyId/$partId")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    if (partId != null) {
                                        viewModel.deletePart(partId)
                                        onNavigateBack()
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { applyFormat("<h1>", "</h1>") }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "Font Size")
                    }
                    IconButton(onClick = { applyFormat("<b>", "</b>") }) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                    }
                    IconButton(onClick = { applyFormat("<i>", "</i>") }) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                    }
                    IconButton(onClick = { applyFormat("<u>", "</u>") }) {
                        Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
                    }
                    IconButton(onClick = { applyFormat("<mark>", "</mark>") }) {
                        Icon(Icons.Default.Highlight, contentDescription = "Highlight")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Image, contentDescription = "Inline Image")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Like")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Header Media Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { /* Add Media */ },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Add Image",
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                        Icon(
                            Icons.Default.OndemandVideo,
                            contentDescription = "Add Video",
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap to add Header Media", color = Color.Gray, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chapter Title
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        "Chapter Title",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.LightGray
                ),
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Body Writing Area
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text(
                        "Tap here to start writing...",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = textColor
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
