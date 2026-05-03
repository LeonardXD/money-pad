package com.example.moneypad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.ui.components.VerifiedIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewScreen(
    storyId: String,
    onNavigateBack: () -> Unit,
    onNavigateToReadPart: (String, String) -> Unit,
    onNavigateToRelatedStory: (String) -> Unit,
    onNavigateToAuthorProfile: (String) -> Unit,
    viewModel: StoryViewModel
) {
    val story by viewModel.currentStory.collectAsState()
    val parts by viewModel.currentParts.collectAsState()
    val reviews by viewModel.reviews.collectAsState()
    val authorProfileImageUrl by viewModel.authorProfileImageUrl.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "Parts")

    var showMenu by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showAlbumSelectionDialog by remember { mutableStateOf(false) }
    val albums by viewModel.albums.collectAsState()

    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    val isLiked by viewModel.isStoryLiked(storyId).collectAsState(initial = false)
    val isInLibrary by viewModel.isStoryInLibrary(storyId).collectAsState(initial = false)
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LaunchedEffect(storyId) {
        viewModel.getStoryById(storyId)
    }

    if (showAlbumSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showAlbumSelectionDialog = false },
            title = { Text("Add to Album") },
            text = {
                if (albums.isEmpty()) {
                    Text("You don't have any albums yet. Create one in your Library.")
                } else {
                    LazyColumn {
                        items(albums) { album ->
                            var isInAlbum by remember { mutableStateOf(false) }
                            LaunchedEffect(album.id) {
                                isInAlbum = viewModel.isStoryInAlbum(album.id, storyId)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isInAlbum) {
                                            viewModel.removeStoryFromAlbum(album.id, storyId)
                                            isInAlbum = false
                                        } else {
                                            viewModel.addStoryToAlbum(album.id, storyId)
                                            isInAlbum = true
                                        }
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isInAlbum,
                                    onCheckedChange = null // Handled by row click
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(album.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlbumSelectionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = { showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Album") },
                            onClick = {
                                showMenu = false
                                showAlbumSelectionDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Make a review") },
                            onClick = {
                                showMenu = false
                                showReviewDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            story?.let {
                if (it.isPublished || it.authorId == viewModel.currentUserId) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (parts.isNotEmpty()) {
                                        onNavigateToReadPart(storyId, parts.first().id)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(25.dp)
                            ) {
                                Text(
                                    "Start reading",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { viewModel.toggleLike(storyId) },
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isInLibrary) {
                                        viewModel.removeStoryFromLibrary(storyId)
                                        android.widget.Toast.makeText(context, "Removed from library", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.downloadStoryToLibrary(storyId) { result ->
                                            result.onSuccess {
                                                android.widget.Toast.makeText(context, "Downloaded to library", android.widget.Toast.LENGTH_SHORT).show()
                                            }.onFailure { error ->
                                                android.widget.Toast.makeText(context, error.message ?: "Failed to download", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(
                                        if (isInLibrary) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = if (isInLibrary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        story?.let { currentStory ->
            if (!currentStory.isPublished && currentStory.authorId != viewModel.currentUserId) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("This story is no longer available.", color = Color.Gray)
                        TextButton(onClick = onNavigateBack) { Text("Go Back") }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Story Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            ) {
                                if (currentStory.coverImageUrl != null) {
                                    AsyncImage(
                                        model = currentStory.coverImageUrl,
                                        contentDescription = "Cover Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Book,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(48.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentStory.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onNavigateToAuthorProfile(currentStory.authorId) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (authorProfileImageUrl != null) {
                                            AsyncImage(
                                                model = authorProfileImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentStory.authorName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (currentStory.authorId == MoneyPadRepository.OFFICIAL_USER_ID) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerifiedIcon(modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = "Views",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${currentStory.readCount}",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.FavoriteBorder,
                                            contentDescription = "Likes",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${currentStory.likes}",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.ChatBubbleOutline,
                                            contentDescription = "Comments",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${currentStory.commentsCount}",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.DarkGray.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Advertisement",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.DarkGray.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            divider = {}
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold
                                            else FontWeight.Medium,
                                            fontSize = 16.sp
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (selectedTab == 0) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Book,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val wordCount = parts.sumOf {
                                        it.content.split(Regex("\\s+"))
                                            .count { w -> w.isNotBlank() }
                                    }
                                    val matureText = if (currentStory.isMature) "Mature" else "Everyone"
                                    val completeText = if (currentStory.isCompleted) "Complete" else "Ongoing"
                                    Text(
                                        text = "$matureText • $completeText • $wordCount words",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                var isExpanded by remember { mutableStateOf(false) }
                                Text(
                                    text = currentStory.overview,
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp,
                                    maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!isExpanded && currentStory.overview.length > 150) {
                                    Text(
                                        text = "Read more",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .clickable { isExpanded = true }
                                    )
                                }

                                Spacer(modifier = Modifier.height(32.dp))

                                Text(
                                    text = "Reviews",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                if (reviews.isEmpty()) {
                                    Text(
                                        "No reviews yet. Be the first to review!",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                } else {
                                    reviews.forEach { review ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 16.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = review.username,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Row {
                                                    for (i in 1..5) {
                                                        Icon(
                                                            imageVector = if (i <= review.rating)
                                                                Icons.Default.Star
                                                            else Icons.Default.StarBorder,
                                                            contentDescription = null,
                                                            tint = Color(0xFFFFD700),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = review.comment,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    } else {
                        if (parts.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No parts available yet.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(parts) { part ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToReadPart(storyId, part.id) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = part.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = dateFormat.format(Date(part.publishedAt)),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Make a Review") },
            text = {
                Column {
                    Text("Rating:", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { reviewRating = i }) {
                                Icon(
                                    imageVector = if (i <= reviewRating) Icons.Default.Star
                                    else Icons.Default.StarBorder,
                                    contentDescription = "Star $i",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reviewComment,
                        onValueChange = { reviewComment = it },
                        label = { Text("Comment") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (reviewComment.isNotBlank()) {
                        viewModel.addReview(storyId, reviewRating, reviewComment)
                        reviewComment = ""
                        reviewRating = 5
                        showReviewDialog = false
                    }
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) { Text("Cancel") }
            }
        )
    }
}
