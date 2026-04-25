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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.moneypad.data.model.Conversation
import com.example.moneypad.ui.components.StatItem
import com.example.moneypad.ui.theme.ThemeViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    themeViewModel: ThemeViewModel,
    onLogout: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    val storiesPublished by viewModel.storiesPublished.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("About", "Conversation")
    
    var showSettings by remember { mutableStateOf(false) }

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

    Column(modifier = Modifier.fillMaxSize()) {
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

            // Settings Icon (Gear)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
                
                DropdownMenu(
                    expanded = showSettings,
                    onDismissRequest = { showSettings = false }
                ) {
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isDarkTheme) "Light Mode" else "Dark Mode")
                                Spacer(modifier = Modifier.weight(1f))
                                Switch(
                                    checked = isDarkTheme,
                                    onCheckedChange = { 
                                        themeViewModel.toggleTheme()
                                    }
                                )
                            }
                        },
                        onClick = { themeViewModel.toggleTheme() }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Logout", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        onClick = {
                            showSettings = false
                            viewModel.logout()
                            onLogout()
                        }
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
                text = user?.username ?: "Loading...",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = user?.email ?: "Loading...",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Followers", value = (user?.followers ?: 0).toString())
                StatItem(label = "Following", value = (user?.following ?: 0).toString())
                StatItem(label = "Stories", value = storiesPublished.toString())
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
                0 -> AboutTab(user?.bio ?: "", onSaveBio = { newBio -> 
                    viewModel.updateProfile(newBio, user?.profileImageUrl, user?.coverImageUrl)
                })
                1 -> ConversationTabWithReplies(user?.id ?: "", conversations, viewModel)
            }
        }
    }
}

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
fun ConversationItemForProfile(
    conv: Conversation,
    userId: String,
    viewModel: ProfileViewModel
) {
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
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
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
                    modifier = Modifier.padding(start = 24.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    replies.forEach { reply ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
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
                        Icon(Icons.Default.Send, contentDescription = "Send Reply", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
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
