package com.example.moneypad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
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
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.data.model.Conversation
import com.example.moneypad.data.model.User
import com.example.moneypad.ui.components.StatItem
import com.example.moneypad.ui.components.StoryCard
import com.example.moneypad.ui.components.VerifiedIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    authorId: String,
    onNavigateBack: () -> Unit,
    onNavigateToStoryDetail: (String) -> Unit,
    onNavigateToAuthorProfile: (String) -> Unit,
    storyViewModel: StoryViewModel,
    profileViewModel: ProfileViewModel
) {
    val author by profileViewModel.getUser(authorId).collectAsState(initial = null)
    val authorStories by storyViewModel.getStoriesByAuthor(authorId).collectAsState(initial = emptyList())
    val conversations by profileViewModel.getConversations(authorId).collectAsState(initial = emptyList())
    val isFollowing by profileViewModel.isFollowing(authorId).collectAsState(initial = false)
    val authorFollowers by profileViewModel.getFollowers(authorId).collectAsState(initial = emptyList())
    val authorFollowing by profileViewModel.getFollowing(authorId).collectAsState(initial = emptyList())
    val myFollowing by profileViewModel.following.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("About", "Conversation", "Stories")
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }

    if (showFollowersDialog) {
        UserListDialog(
            title = "Followers",
            users = authorFollowers,
            onDismiss = { showFollowersDialog = false },
            onNavigateToPublicProfile = { userId ->
                showFollowersDialog = false
                onNavigateToAuthorProfile(userId)
            },
            isFollowersList = true,
            myFollowingList = myFollowing,
            onToggleFollow = { targetUser ->
                val isCurrentlyFollowing = myFollowing.any { it.id == targetUser.id }
                profileViewModel.toggleFollow(targetUser.id, isCurrentlyFollowing)
            },
            currentUserId = profileViewModel.currentUserId
        )
    }

    if (showFollowingDialog) {
        UserListDialog(
            title = "Following",
            users = authorFollowing,
            onDismiss = { showFollowingDialog = false },
            onNavigateToPublicProfile = { userId ->
                showFollowingDialog = false
                onNavigateToAuthorProfile(userId)
            },
            isFollowersList = false,
            myFollowingList = myFollowing,
            onToggleFollow = {},
            currentUserId = profileViewModel.currentUserId
        )
    }

    var messageText by remember { mutableStateOf("") }

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
        author?.let { user ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                // Top Profile Header with Images
                item {
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
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // User Info
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.username,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (user.id == MoneyPadRepository.OFFICIAL_USER_ID) {
                                Spacer(modifier = Modifier.width(6.dp))
                                VerifiedIcon()
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (user.id != MoneyPadRepository.OFFICIAL_USER_ID || !isFollowing) {
                            Button(
                                onClick = { profileViewModel.toggleFollow(authorId, isFollowing) },
                                colors = if (isFollowing) ButtonDefaults.outlinedButtonColors() else ButtonDefaults.buttonColors(),
                                border = if (isFollowing) ButtonDefaults.outlinedButtonBorder else null
                            ) {
                                Text(if (isFollowing) "Unfollow" else "Follow")
                            }
                        } else {
                            // Official account already followed, cannot unfollow
                            Button(
                                onClick = { },
                                enabled = false,
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    disabledContentColor = Color.White
                                )
                            ) {
                                Text("Following")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Box(modifier = Modifier.clickable { showFollowersDialog = true }) {
                                StatItem(label = "Followers", value = user.followers.toString())
                            }
                            Box(modifier = Modifier.clickable { showFollowingDialog = true }) {
                                StatItem(label = "Following", value = user.following.toString())
                            }
                            Box(modifier = Modifier.clickable { selectedTab = 2 }) {
                                StatItem(label = "Stories", value = authorStories.size.toString())
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Tabs
                item {
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }

                // Tab Content
                when (selectedTab) {
                    0 -> item {
                        AboutTabReadOnly(user.bio)
                    }
                    1 -> {
                        // Post Message Area
                        item {
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
                        }

                        if (conversations.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No conversations yet.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(conversations) { conv ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    ConversationItem(conv, authorId, profileViewModel)
                                }
                            }
                        }
                    }
                    2 -> {
                        if (authorStories.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No stories published yet.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(authorStories) { story ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    StoryCard(story = story, onClick = { onNavigateToStoryDetail(story.id) })
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun AboutTabReadOnly(bio: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
fun ConversationItem(
    conv: Conversation,
    authorId: String,
    profileViewModel: ProfileViewModel
) {
    val replies by profileViewModel.getReplies(conv.id).collectAsState(initial = emptyList())
    var showReplyInput by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoViewRequester),
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
                    if (conv.senderProfileImageUrl != null) {
                        AsyncImage(
                            model = conv.senderProfileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    }
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
                                if (reply.senderProfileImageUrl != null) {
                                    AsyncImage(
                                        model = reply.senderProfileImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
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
                        modifier = Modifier.weight(1f).focusRequester(focusRequester),
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
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    scope.launch {
                        bringIntoViewRequester.bringIntoView()
                    }
                }
            }
        }
    }
}
