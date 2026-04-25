package com.example.moneypad.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val password: String = "",
    val bio: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val profileImageUrl: String? = null,
    val coverImageUrl: String? = null,
    val balance: Double = 0.0,
    val authorIncome: Double = 0.0,
    val readerCoins: Int = 0
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val authorId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val parentId: String? = null // For replies
)

@Entity(tableName = "follows", primaryKeys = ["followerId", "followedId"])
data class Follow(
    val followerId: String,
    val followedId: String
)

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey val id: String,
    val authorId: String,
    val authorName: String,
    val title: String,
    val overview: String,
    val genres: String = "", // Comma-separated genres
    val coverImageUrl: String? = null,
    val readCount: Int = 0,
    val isPublished: Boolean = false,
    val likes: Int = 0,
    val commentsCount: Int = 0,
    val uniqueViews: Int = 0,
    val repeatedViews: Int = 0
)

@Entity(tableName = "story_parts")
data class StoryPart(
    @PrimaryKey val id: String,
    val storyId: String,
    val title: String,
    val content: String,
    val order: Int
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val method: String, // "PayPal" or "GCash"
    val accountInfo: String, // Email or Number
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending"
)
