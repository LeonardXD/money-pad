package com.example.moneypad.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.moneypad.R
import com.example.moneypad.data.model.Story
import com.example.moneypad.data.model.User

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BadgeShape(private val points: Int = 12, private val innerRadiusScale: Float = 0.92f) : Shape {
    override fun createOutline(size: Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: Density): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.width / 2f
        val innerRadius = radius * innerRadiusScale
        
        val angleStep = (2 * PI / points).toFloat()
        val halfAngleStep = angleStep / 2f
        
        for (i in 0 until points) {
            val outerAngle = i * angleStep
            val innerAngle = outerAngle + halfAngleStep
            
            val ox = centerX + radius * cos(outerAngle)
            val oy = centerY + radius * sin(outerAngle)
            
            if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)
            
            val ix = centerX + innerRadius * cos(innerAngle)
            val iy = centerY + innerRadius * sin(innerAngle)
            path.lineTo(ix, iy)
        }
        path.close()
        return Outline.Generic(path)
    }
}

@Composable
fun ClickableMessageText(
    message: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    color: Color = Color.Unspecified,
    onUserClick: (String) -> Unit
) {
    val annotatedString = buildAnnotatedString {
        val words = message.split(" ")
        words.forEachIndexed { index, word ->
            if (word.startsWith("@") && word.length > 1) {
                // Handle potential punctuation at the end of username
                val cleanWord = word.substring(1)
                val username = cleanWord.takeWhile { it.isLetterOrDigit() || it == '_' }
                val suffix = cleanWord.substring(username.length)
                
                pushStringAnnotation(tag = "USER", annotation = username)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                    append("@$username")
                }
                pop()
                if (suffix.isNotEmpty()) {
                    append(suffix)
                }
            } else {
                append(word)
            }
            if (index < words.size - 1) append(" ")
        }
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = LocalTextStyle.current.copy(fontSize = fontSize, color = color),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "USER", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onUserClick(annotation.item)
                }
        }
    )
}

@Composable
fun VerifiedIcon(modifier: Modifier = Modifier, size: Dp = 14.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xFF4CAF50), shape = BadgeShape())
            .padding(size * 0.15f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Verified",
            tint = Color.White,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun StoryCard(story: Story, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp, 100.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                    Icon(
                        Icons.Default.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text(
                    text = story.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "By ${story.authorName}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (story.isAuthorVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedIcon(size = 20.dp)
                    }
                }
                if (story.genres.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Genre: ${story.genres}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ReadingListItem(
    list: com.example.moneypad.data.model.ReadingList,
    user: User?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Reading List?") },
            text = { Text("Are you sure you want to delete '${list.name}'? The stories inside won't be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "By ${user?.username ?: "User"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (user?.isVerified == true) {
                        Spacer(modifier = Modifier.width(4.dp))
                        VerifiedIcon(size = 20.dp)
                    }
                }
            }
            if (onDelete != null) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Reading List",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun PublishedStoryCard(story: Story, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(70.dp, 100.dp)
                            .clip(RoundedCornerShape(8.dp))
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
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(story.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(story.overview, fontSize = 14.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Likes", modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${story.likes}", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${story.commentsCount}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                Column(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(
                        onClick = { /* TODO: Share functionality */ }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = onDelete
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Views", "${story.readCount}")
                StatItem("Unique", "${story.uniqueViews}")
                StatItem("Repeated", "${story.repeatedViews}")
            }
        }
    }
}

@Composable
fun DraftStoryCard(
    story: Story,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(70.dp, 100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (story.coverImageUrl != null) {
                        AsyncImage(model = story.coverImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(story.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Draft - Not published", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                            Text("Edit", fontSize = 12.sp)
                        }
                        Button(onClick = onDelete, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Continue Writing")
            }
        }
    }
}

@Composable
fun CarouselStoryCard(
    story: Story,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (story.coverImageUrl != null) {
                    AsyncImage(
                        model = story.coverImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Dark Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 200f
                            )
                        )
                )

                // Category Label
                val firstGenre = story.genres.split(",").firstOrNull()?.trim()?.lowercase() ?: "general"
                Text(
                    text = firstGenre,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = story.authorName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (story.isAuthorVerified) {
                Spacer(modifier = Modifier.width(4.dp))
                VerifiedIcon(size = 20.dp)
            }
        }
        
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Visibility,
                    contentDescription = "Views",
                    modifier = Modifier.size(14.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${story.readCount}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
