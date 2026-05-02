package com.example.moneypad.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.moneypad.utils.ImageUtils

import java.io.File

val STORY_GENRES = listOf(
    "Romance", "Fantasy", "Mystery", "Sci-Fi", "Horror",
    "Action", "LGBTQIA+", "Werewolf", "New Adult", "Short Story",
    "Teen Fiction", "Historical Fiction", "Paranormal", "Humor",
    "Contemporary Lit", "Diverse Lit", "Thriller", "Adventure",
    "Fan Fiction", "Non-Fiction", "Poetry"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    storyId: String? = null,
    onNavigateBack: () -> Unit,
    onStoryCreated: (storyId: String) -> Unit,
    viewModel: StoryViewModel
) {
    var title by remember { mutableStateOf("") }
    var selectedGenres by remember { mutableStateOf(setOf<String>()) }
    var overview by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("English") }
    var isCompleted by remember { mutableStateOf(false) }
    var isMature by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var genreDropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isEditMode = storyId != null
    val currentStory by viewModel.currentStory.collectAsState()

    LaunchedEffect(storyId) {
        if (storyId != null) {
            viewModel.getStoryById(storyId)
        }
    }

    LaunchedEffect(currentStory) {
        if (isEditMode && currentStory != null) {
            val story = currentStory!!
            title = story.title
            selectedGenres = story.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            overview = story.overview
            isCompleted = story.isCompleted
            isMature = story.isMature
            if (!story.coverImageUrl.isNullOrBlank()) {
                imageUri = Uri.parse(story.coverImageUrl)
            }
        }
    }

    val lastCreatedStoryId by viewModel.lastCreatedStoryId.collectAsState()

    // Navigate once the story ID is available
    LaunchedEffect(lastCreatedStoryId) {
        lastCreatedStoryId?.let { id ->
            viewModel.clearLastCreatedStoryId()
            onStoryCreated(id)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val localPath = ImageUtils.saveImageToInternalStorage(context, it)
            if (localPath != null) {
                imageUri = Uri.fromFile(File(localPath))
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Story" else "Story Setup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                // Book Cover Image Picker
                Box(
                    modifier = Modifier
                        .size(100.dp, 150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Cover",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Cover",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = {
                            Text(
                                "Story Title",
                                fontSize = 20.sp,
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
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Story Details",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = overview,
                onValueChange = { overview = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Genre dropdown
            ExposedDropdownMenuBox(
                expanded = genreDropdownExpanded,
                onExpandedChange = { genreDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = if (selectedGenres.isEmpty()) ""
                    else selectedGenres.joinToString(", "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Genre") },
                    placeholder = { Text("Select genres") },
                    trailingIcon = {
                        Icon(
                            if (genreDropdownExpanded) Icons.Default.ArrowDropUp
                            else Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = genreDropdownExpanded,
                    onDismissRequest = { genreDropdownExpanded = false }
                ) {
                    STORY_GENRES.forEach { genre ->
                        val isSelected = genre in selectedGenres
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null // handled by the row click
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(genre)
                                }
                            },
                            onClick = {
                                selectedGenres = if (isSelected) {
                                    selectedGenres - genre
                                } else {
                                    selectedGenres + genre
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = language,
                onValueChange = { language = it },
                label = { Text("Language") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isCompleted, onCheckedChange = { isCompleted = it })
                Text("Mark as Completed")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isMature, onCheckedChange = { isMature = it })
                Text("Mature Content (18+)")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && overview.isNotBlank()) {
                        if (isEditMode && storyId != null) {
                            viewModel.updateStory(
                                storyId,
                                title,
                                selectedGenres.joinToString(", "),
                                overview,
                                imageUri?.toString(),
                                isCompleted,
                                isMature
                            )
                        } else {
                            viewModel.addStory(
                                title,
                                selectedGenres.joinToString(", "),
                                overview,
                                imageUri?.toString(),
                                isPublished = false
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank() && overview.isNotBlank()
            ) {
                Text(
                    if (isEditMode) "Save Changes" else "Create Story",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}