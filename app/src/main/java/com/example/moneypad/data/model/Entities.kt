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
    val readerCoins: Int = 0,
    // New fields
    val birthday: String = "",          // "YYYY-MM-DD"
    val gender: String = "",            // "Male" | "Female" | "Non-binary" | "Prefer not to say"
    val preferredGenres: String = "",   // comma-separated, max 5
    val referredBy: String = "",        // username of referrer
    val referralCount: Int = 0,         // how many users this user referred
    val loginTimestamp: Long = 0L,      // epoch ms of last login, used for 7-day session check
    val onboardingStep: Int = 1,
    val onboardingCompleted: Boolean = false
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val authorId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val parentId: String? = null
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
    val genres: String = "",
    val coverImageUrl: String? = null,
    val readCount: Int = 0,
    val isPublished: Boolean = false,
    val isCompleted: Boolean = false,
    val isMature: Boolean = false,
    val likes: Int = 0,
    val commentsCount: Int = 0,
    val uniqueViews: Int = 0,
    val repeatedViews: Int = 0,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_read_parts", primaryKeys = ["userId", "partId"])
data class UserReadPart(
    val userId: String,
    val partId: String,
    val storyId: String,
    val readAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "story_parts")
data class StoryPart(
    @PrimaryKey val id: String,
    val storyId: String,
    val title: String,
    val content: String,
    val order: Int,
    val publishedAt: Long = System.currentTimeMillis(),
    val isPublished: Boolean = false,
    val readCount: Int = 0
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val method: String,
    val accountInfo: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending"
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey val id: String,
    val storyId: String,
    val userId: String,
    val username: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_story_likes", primaryKeys = ["userId", "storyId"])
data class UserStoryLike(
    val userId: String,
    val storyId: String
)

@Entity(tableName = "part_annotations")
data class PartAnnotation(
    @PrimaryKey val id: String,
    val partId: String,
    val userId: String,
    val username: String,
    val selectedText: String,
    val startIndex: Int,
    val endIndex: Int,
    val type: String, // "LIKE" or "COMMENT"
    val content: String? = null, // Used for comments
    val timestamp: Long = System.currentTimeMillis()
)
