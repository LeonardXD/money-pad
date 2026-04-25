package com.example.moneypad.ui.screens

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
import coil.compose.AsyncImage
import com.example.moneypad.data.model.Conversation
import com.example.moneypad.data.model.User
import com.example.moneypad.ui.components.StatItem
import com.example.moneypad.ui.components.StoryCard
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    authorId: String,
    onNavigateBack: () -> Unit,
    onNavigateToStoryDetail: (String) -> Unit,
    storyViewModel: StoryViewModel,
    profileViewModel: ProfileViewModel
) {
    val author by profileViewModel.getUser(authorId).collectAsState(initial = null)
    val authorStories by storyViewModel.getStoriesByAuthor(authorId).collectAsState(initial = emptyList())
    val conversations by profileViewModel.getConversations(authorId).collectAsState(initial = emptyList())
    val isFollowing by profileViewModel.isFollowing(authorId).collectAsState(initial = false)

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("About", "Conversation", "Stories")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(author?.username ?: "Author Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
        ) {
            author?.let { user ->
                // Top Profile Header with Images
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Cover Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        if (user.coverImageUrl != null) {
                            AsyncImage(
                                model = user.coverImageUrl,
                                contentDescription = "Cover Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    // Profile Image
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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.profileImageUrl != null) {
                                AsyncImage(
                                    model = user.profileImageUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // User Info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = user.username,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { profileViewModel.toggleFollow(authorId, isFollowing) },
                        colors = if (isFollowing) ButtonDefaults.outlinedButtonColors() else ButtonDefaults.buttonColors(),
                        border = if (isFollowing) ButtonDefaults.outlinedButtonBorder else null
                    ) {
                        Text(if (isFollowing) "Unfollow" else "Follow")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Followers", value = user.followers.toString())
                        StatItem(label = "Following", value = user.following.toString())
                        StatItem(label = "Stories", value = authorStories.size.toString())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> AboutTabReadOnly(user.bio)
                        1 -> ConversationTabWithPosting(
                            authorId = authorId,
                            conversations = conversations,
                            profileViewModel = profileViewModel
                        )
                        2 -> StoriesTab(authorStories, onNavigateToStoryDetail)
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun AboutTabReadOnly(bio: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("About", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (bio.isBlank()) "No bio yet." else bio,
            color = if (bio.isBlank()) Color.Gray else Color.Unspecified
        )
    }
}

@Composable
fun StoriesTab(stories: List<com.example.moneypad.data.model.Story>, onNavigateToStoryDetail: (String) -> Unit) {
    if (stories.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No stories published yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(stories) { story ->
                StoryCard(story = story, onClick = { onNavigateToStoryDetail(story.id) })
            }
        }
    }
}

@Composable
fun ConversationTabWithPosting(
    authorId: String,
    conversations: List<Conversation>,
    profileViewModel: ProfileViewModel
) {
    var messageText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Post Message Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Post a message to the wall...") },
                maxLines = 3,
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        profileViewModel.sendMessage(authorId, messageText)
                        messageText = ""
                    }
                },
                enabled = messageText.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (conversations.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No conversations yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(conversations) { conv ->
                    ConversationItem(conv, authorId, profileViewModel)
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conv: Conversation,
    authorId: String,
    profileViewModel: ProfileViewModel
) {
    val replies by profileViewModel.getReplies(conv.id).collectAsState(initial = emptyList())
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
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
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

            // Replies
            if (replies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    replies.forEach { reply ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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
                                profileViewModel.sendMessage(authorId, replyText, conv.id)
                                replyText = ""
                                showReplyInput = false
                            }
                        },
                        enabled = replyText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Reply", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
