package com.example.moneypad.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.moneypad.data.model.Conversation
import com.example.moneypad.data.model.Story
import com.example.moneypad.data.model.User
import com.example.moneypad.ui.components.StatItem
import com.example.moneypad.ui.theme.ThemeViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    themeViewModel: ThemeViewModel,
    onLogout: () -> Unit,
    onNavigateToPublicProfile: (String) -> Unit
) {
    val user by viewModel.user.collectAsState()
    val storiesPublished by viewModel.storiesPublished.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val followers by viewModel.followers.collectAsState()
    val following by viewModel.following.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("About", "Stories", "Conversation")

    var showSettings by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }

    if (showSettingsScreen) {
        SettingsScreen(
            viewModel = viewModel,
            themeViewModel = themeViewModel,
            onNavigateBack = { showSettingsScreen = false },
            onLogout = onLogout
        )
        return
    }

    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfile(user?.bio ?: "", it.toString(), user?.coverImageUrl) }
    }

    val coverImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfile(user?.bio ?: "", user?.profileImageUrl, it.toString()) }
    }

    // Followers dialog
    if (showFollowersDialog) {
        UserListDialog(
            title = "Followers",
            users = followers,
            onDismiss = { showFollowersDialog = false },
            onNavigateToPublicProfile = { userId ->
                showFollowersDialog = false
                onNavigateToPublicProfile(userId)
            },
            isFollowersList = true,
            myFollowingList = following,
            onToggleFollow = { targetUser ->
                val isCurrentlyFollowing = following.any { it.id == targetUser.id }
                viewModel.toggleFollow(targetUser.id, isCurrentlyFollowing)
            }
        )
    }

    // Following dialog
    if (showFollowingDialog) {
        UserListDialog(
            title = "Following",
            users = following,
            onDismiss = { showFollowingDialog = false },
            onNavigateToPublicProfile = { userId ->
                showFollowingDialog = false
                onNavigateToPublicProfile(userId)
            },
            isFollowersList = false,
            myFollowingList = following,
            onToggleFollow = {}
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Profile header ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // Cover image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { coverImageLauncher.launch("image/*") }
            ) {
                if (user?.coverImageUrl != null) {
                    AsyncImage(
                        model = user?.coverImageUrl,
                        contentDescription = "Cover Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = "Add Cover",
                        modifier = Modifier.align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Settings gear
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { showSettingsScreen = true },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }

            // Profile image
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable { profileImageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (user?.profileImageUrl != null) {
                        AsyncImage(
                            model = user?.profileImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── User info ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = user?.username ?: "Loading...",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            // Email visible only to owner
            Text(
                text = user?.email ?: "",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row — followers/following are tappable
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Box(modifier = Modifier.clickable { showFollowersDialog = true }) {
                    StatItem(label = "Followers", value = (user?.followers ?: 0).toString())
                }
                Box(modifier = Modifier.clickable { showFollowingDialog = true }) {
                    StatItem(label = "Following", value = (user?.following ?: 0).toString())
                }
                Box(modifier = Modifier.clickable { selectedTab = 1 }) {
                    StatItem(label = "Stories", value = storiesPublished.toString())
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Tabs ───────────────────────────────────────────────────────────
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AboutTab(user?.bio ?: "", onSaveBio = { newBio ->
                    viewModel.updateProfile(newBio, user?.profileImageUrl, user?.coverImageUrl)
                })
                1 -> ProfileStoriesTab(viewModel = viewModel)
                2 -> ConversationTabWithReplies(user?.id ?: "", conversations, viewModel)
            }
        }
    }
}

// ── Followers / Following dialog ──────────────────────────────────────────────

@Composable
fun UserListDialog(
    title: String,
    users: List<User>,
    onDismiss: () -> Unit,
    onNavigateToPublicProfile: (String) -> Unit,
    isFollowersList: Boolean,
    myFollowingList: List<User>,
    onToggleFollow: (User) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                if (users.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No users yet", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users) { u ->
                            val isFollowing = myFollowingList.any { it.id == u.id }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToPublicProfile(u.id) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (u.profileImageUrl != null) {
                                        AsyncImage(
                                            model = u.profileImageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(u.username, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                
                                if (isFollowersList) {
                                    IconButton(onClick = { onToggleFollow(u) }) {
                                        Icon(
                                            imageVector = if (isFollowing) Icons.Default.Check else Icons.Default.PersonAdd,
                                            contentDescription = if (isFollowing) "Following" else "Follow",
                                            tint = if (isFollowing) Color.Green else MaterialTheme.colorScheme.primary
                                        )
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

// ── Stories grid (own profile) ────────────────────────────────────────────────

@Composable
fun ProfileStoriesTab(viewModel: ProfileViewModel) {
    val stories by viewModel.user.collectAsState()
    // We load published stories for the current user via a dedicated flow
    val storiesList by remember {
        derivedStateOf { emptyList<Story>() }
    }
    // Use a real flow — call viewModel helper
    val publishedStories by viewModel
        .getPublishedStoriesForCurrentUser()
        .collectAsState(initial = emptyList())

    if (publishedStories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No published stories yet.", color = Color.Gray)
        }
        return
    }

    val displayedStories = if (publishedStories.size > 6) publishedStories.take(6) else publishedStories
    val hasMore = publishedStories.size > 6

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val chunked = displayedStories.chunked(2)
        items(chunked) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { story ->
                    ProfileStoryItem(story = story, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (hasMore) {
            item {
                TextButton(
                    onClick = { /* future: navigate to full list */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View All Stories →", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ProfileStoryItem(story: Story, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (story.coverImageUrl != null) {
                    AsyncImage(
                        model = story.coverImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = story.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${story.readCount} reads",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// ── Keep existing tabs unchanged ──────────────────────────────────────────────

@Composable
fun ConversationTabWithReplies(
    userId: String,
    conversations: List<Conversation>,
    viewModel: ProfileViewModel
) {
    if (conversations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No conversations yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conversations) { conv ->
                ConversationItemForProfile(conv, userId, viewModel)
            }
        }
    }
}

@Composable
fun ConversationItemForProfile(conv: Conversation, userId: String, viewModel: ProfileViewModel) {
    val replies by viewModel.getReplies(conv.id).collectAsState(initial = emptyList())
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(conv.senderName, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showReplyInput = !showReplyInput }) {
                    Text("Reply", fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(conv.message)

            if (replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.padding(start = 24.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    replies.forEach { reply ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(reply.senderName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(reply.message, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            if (showReplyInput) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Write a reply...", fontSize = 12.sp) },
                        maxLines = 2,
                        shape = RoundedCornerShape(20.dp)
                    )
                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                viewModel.sendMessage(userId, replyText, conv.id)
                                replyText = ""
                                showReplyInput = false
                            }
                        },
                        enabled = replyText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutTab(bio: String, onSaveBio: (String) -> Unit) {
    var editedBio by remember(bio) { mutableStateOf(bio) }
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("About Me", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            IconButton(onClick = {
                if (isEditing) onSaveBio(editedBio)
                isEditing = !isEditing
            }) {
                Icon(if (isEditing) Icons.Default.Save else Icons.Default.Edit, contentDescription = null)
            }
        }

        if (isEditing) {
            OutlinedTextField(
                value = editedBio,
                onValueChange = { if (it.length <= 1000) editedBio = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                placeholder = { Text("Write something about yourself...") },
                label = { Text("${editedBio.length}/1000") }
            )
        } else {
            Text(
                text = if (bio.isBlank()) "No bio yet." else bio,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = if (bio.isBlank()) Color.Gray else Color.Unspecified
            )
        }
    }
}