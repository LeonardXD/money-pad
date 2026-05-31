package com.example.moneypad.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val password: String = "",
    val bio: String? = null,
    val followers: Int = 0,
    val following: Int = 0,
    val profileImageUrl: String? = null,
    val coverImageUrl: String? = null,
    val balance: Double = 0.0,
    val authorIncome: Double = 0.0,
    val readerCoins: Double = 0.0,
    val totalReaderCoins: Double = 0.0,      // lifetime reader coins earned
    // New fields
    val birthday: String = "",          // "YYYY-MM-DD"
    val gender: String = "",            // "Male" | "Female" | "Non-binary" | "Prefer not to say"
    val preferredGenres: String = "",   // comma-separated, max 5
    val referredBy: String = "",        // username of referrer
    val referralCount: Int = 0,         // how many users this user referred
    val signupTimestamp: Long = System.currentTimeMillis(),
    val isReferralRewardClaimed: Boolean = false,
    val loginTimestamp: Long = 0L,      // epoch ms of last login, used for 7-day session check
    val onboardingStep: Int = 1,
    val onboardingCompleted: Boolean = false,
    val isVerified: Boolean = false,
    val adFreeUntil: Long = 0L,
    val isAdFreePermanently: Boolean = false
)

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey val id: String,
    val authorId: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val senderProfileImageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val parentId: String? = null,
    val isSenderVerified: Boolean = false,
    val likes: Int = 0,
    val isLiked: Boolean = false
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
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val isAuthorVerified: Boolean = false
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
    val readCount: Int = 0,
    val headerImageUrl: String? = null
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Double,
    val method: String,
    val accountInfo: String,
    val source: String = "",           // "AUTHOR", "READER", or empty
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending"
)

@Entity(tableName = "reviews")
data class Review(
    @PrimaryKey val id: String,
    val storyId: String,
    val userId: String,
    val username: String,
    val userProfileImageUrl: String? = null,
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isUserVerified: Boolean = false
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
    val timestamp: Long = System.currentTimeMillis(),
    val isUserVerified: Boolean = false
)

@Entity(tableName = "library_stories", primaryKeys = ["userId", "storyId"])
data class LibraryStory(
    val userId: String,
    val storyId: String,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_lists")
data class ReadingList(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_list_stories", primaryKeys = ["listId", "storyId"])
data class ReadingListStory(
    val listId: String,
    val storyId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey val id: String,
    val userId: String, // Recipient
    val type: String, // "FOLLOW", "NEW_STORY", "NEW_PART"
    val actorId: String, // Who triggered it
    val actorName: String,
    val actorProfileImageUrl: String? = null,
    val storyId: String? = null,
    val storyTitle: String? = null,
    val partId: String? = null,
    val partTitle: String? = null,
    val content: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isActorVerified: Boolean = false
)
