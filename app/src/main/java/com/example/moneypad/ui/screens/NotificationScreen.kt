package com.example.moneypad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import coil.compose.AsyncImage
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.data.model.Notification
import com.example.moneypad.ui.components.VerifiedIcon
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    onNavigateToStoryDetail: (String) -> Unit,
    onNavigateToAuthorProfile: (String) -> Unit,
    onNavigateToConversation: (String) -> Unit,
    viewModel: StoryViewModel
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                        Text("Mark all as read")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No notifications yet", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(notifications) { notification ->
                    NotificationItem(
                        notification = notification,
                        onClick = {
                            viewModel.markNotificationAsRead(notification.id)
                            when (notification.type) {
                                "FOLLOW" -> onNavigateToAuthorProfile(notification.actorId)
                                "NEW_STORY", "NEW_PART", "LIKE", "READ", "REVIEW" -> notification.storyId?.let { onNavigateToStoryDetail(it) }
                                "CONVERSATION", "REPLY" -> notification.storyId?.let { onNavigateToConversation(it) }
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(notification.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (notification.isRead) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Actor Profile Image
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (notification.actorProfileImageUrl != null) {
                AsyncImage(
                    model = notification.actorProfileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val text = when (notification.type) {
                "WELCOME" -> {
                    "Welcome to **Money Pad**! We're glad you're here. Start exploring and enjoy reading!"
                }
                "FOLLOW" -> {
                    "**${notification.actorName}** followed you."
                }
                "NEW_STORY" -> {
                    "**${notification.actorName}** published a new story: **${notification.storyTitle}**"
                }
                "NEW_PART" -> {
                    "**${notification.actorName}** published a new part for **${notification.storyTitle}**: **${notification.partTitle}**"
                }
                "CONVERSATION" -> {
                    if (notification.userId == notification.storyId) {
                        "**${notification.actorName}** posted on your wall."
                    } else {
                        "**${notification.actorName}** posted on their wall."
                    }
                }
                "REPLY" -> {
                    if (notification.userId == notification.storyId) {
                        "**${notification.actorName}** replied to a post on your wall."
                    } else {
                        "**${notification.actorName}** replied to a conversation."
                    }
                }
                else -> ""
            }

            NotificationText(
                text = text,
                isVerified = notification.isActorVerified
            )

            if (!notification.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.content,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateString,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun NotificationText(text: String, isVerified: Boolean = false) {
    val parts = text.split("**")
    val annotatedString = buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold))
                append(part)
                pop()
                // Append verified icon if the actor (usually the first bold part) is verified
                if (isVerified && index == 1) {
                    append(" ")
                    appendInlineContent("verified", "[verified]")
                }
            } else {
                append(part)
            }
        }
    }

    val inlineContent = mapOf(
        "verified" to InlineTextContent(
            Placeholder(14.sp, 14.sp, PlaceholderVerticalAlign.Center)
        ) {
            VerifiedIcon(modifier = Modifier.size(36.dp))
        }
    )

    Text(
        text = annotatedString,
        inlineContent = inlineContent,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
}
