package com.example.moneypad.data

import android.content.Context
import com.example.moneypad.data.dao.MoneyPadDao
import com.example.moneypad.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MoneyPadRepository(private val context: Context, private val dao: MoneyPadDao) {

    private val sharedPreferences = context.getSharedPreferences("moneypad_prefs", Context.MODE_PRIVATE)

    var currentUserId: String = sharedPreferences.getString("userId", "") ?: ""
    var currentUsername: String = sharedPreferences.getString("username", "") ?: ""

    private fun saveLoginState(userId: String, username: String) {
        sharedPreferences.edit()
            .putString("userId", userId)
            .putString("username", username)
            .apply()
    }

    private fun clearLoginState() {
        sharedPreferences.edit()
            .remove("userId")
            .remove("username")
            .apply()
    }

    suspend fun signup(username: String, email: String, password: String): Result<User> {
        if (dao.getUserByUsername(username) != null)
            return Result.failure(Exception("Username already taken"))
        if (dao.getUserByEmail(email) != null)
            return Result.failure(Exception("Email already registered"))
        val user = User(
            id = UUID.randomUUID().toString(),
            username = username,
            email = email,
            password = password
        )
        dao.insertUser(user)
        currentUserId = user.id
        currentUsername = user.username
        saveLoginState(currentUserId, currentUsername)
        return Result.success(user)
    }

    fun logout() {
        currentUserId = ""
        currentUsername = ""
        clearLoginState()
    }

    suspend fun login(identifier: String, password: String): Result<User> {
        val user = dao.login(identifier, password)
        return if (user != null) {
            currentUserId = user.id
            currentUsername = user.username
            saveLoginState(currentUserId, currentUsername)
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid username or password"))
        }
    }

    fun getUser(userId: String): Flow<User?> = dao.getUser(userId)

    suspend fun initUser() {}

    fun getAllStories(): Flow<List<Story>> = dao.getAllStories()

    fun getPublishedStoriesByAuthor(authorId: String): Flow<List<Story>> =
        dao.getPublishedStoriesByAuthor(authorId)

    fun getDraftStoriesByAuthor(authorId: String): Flow<List<Story>> =
        dao.getDraftStoriesByAuthor(authorId)

    suspend fun getStoryById(storyId: String): Story? = dao.getStoryById(storyId)

    suspend fun createStory(
        title: String, genres: String, overview: String, coverImageUrl: String?,
        isPublished: Boolean = false, isCompleted: Boolean = false, isMature: Boolean = false
    ) {
        dao.insertStory(
            Story(
                id = UUID.randomUUID().toString(),
                authorId = currentUserId, authorName = currentUsername,
                title = title, genres = genres, overview = overview,
                coverImageUrl = coverImageUrl,
                isPublished = isPublished, isCompleted = isCompleted, isMature = isMature
            )
        )
    }

    suspend fun createStoryAndReturnId(
        title: String, genres: String, overview: String, coverImageUrl: String?,
        isPublished: Boolean = false, isCompleted: Boolean = false, isMature: Boolean = false
    ): String {
        val id = UUID.randomUUID().toString()
        dao.insertStory(
            Story(
                id = id,
                authorId = currentUserId, authorName = currentUsername,
                title = title, genres = genres, overview = overview,
                coverImageUrl = coverImageUrl,
                isPublished = isPublished, isCompleted = isCompleted, isMature = isMature
            )
        )
        return id
    }

    suspend fun publishStory(storyId: String) = dao.publishStory(storyId)

    fun getStoriesByGenre(genre: String): Flow<List<Story>> = dao.getStoriesByGenre(genre)

    suspend fun addStoryPart(storyId: String, title: String, content: String, order: Int) {
        dao.insertStoryPart(
            StoryPart(
                id = UUID.randomUUID().toString(),
                storyId = storyId, title = title, content = content, order = order,
                publishedAt = System.currentTimeMillis()
            )
        )
    }

    fun getPartsForStory(storyId: String): Flow<List<StoryPart>> = dao.getPartsForStory(storyId)

    suspend fun recordRead(storyId: String) {
        val story = dao.getStoryById(storyId) ?: return
        dao.incrementReadCount(storyId)
        dao.updateBalance(story.authorId, 0.0001)
    }

    suspend fun withdraw(amount: Double, method: String, accountInfo: String): Boolean {
        if (amount < 0.10) return false
        dao.deductBalance(currentUserId, amount)
        dao.insertTransaction(
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = currentUserId, amount = amount,
                method = method, accountInfo = accountInfo
            )
        )
        return true
    }

    fun getTransactions(userId: String): Flow<List<Transaction>> =
        dao.getTransactionsForUser(userId)

    fun searchStories(query: String, genre: String = "All"): Flow<List<Story>> =
        dao.searchStories(query, genre)

    fun searchAuthors(query: String): Flow<List<User>> = dao.searchAuthors(query)

    suspend fun updateUserProfile(bio: String, profileImageUrl: String?, coverImageUrl: String?) {
        dao.updateUserProfile(currentUserId, bio, profileImageUrl, coverImageUrl)
    }

    suspend fun sendMessage(authorId: String, message: String, parentId: String? = null) {
        dao.insertConversation(
            Conversation(
                id = UUID.randomUUID().toString(),
                authorId = authorId, senderId = currentUserId,
                senderName = currentUsername, message = message, parentId = parentId
            )
        )
    }

    fun getConversations(authorId: String): Flow<List<Conversation>> =
        dao.getConversationsForAuthor(authorId)

    fun getReplies(parentId: String): Flow<List<Conversation>> = dao.getReplies(parentId)

    suspend fun followUser(followedId: String) {
        dao.insertFollow(Follow(currentUserId, followedId))
        dao.updateFollowing(currentUserId, 1)
        dao.updateFollowers(followedId, 1)
    }

    suspend fun unfollowUser(followedId: String) {
        dao.deleteFollow(currentUserId, followedId)
        dao.updateFollowing(currentUserId, -1)
        dao.updateFollowers(followedId, -1)
    }

    fun isFollowing(followedId: String): Flow<Boolean> =
        dao.isFollowing(currentUserId, followedId)

    // ── Reviews ──────────────────────────────────────────────────────────────
    suspend fun addReview(storyId: String, rating: Int, comment: String) {
        dao.insertReview(
            Review(
                id = UUID.randomUUID().toString(),
                storyId = storyId, userId = currentUserId,
                username = currentUsername, rating = rating, comment = comment
            )
        )
    }

    fun getReviewsForStory(storyId: String): Flow<List<Review>> =
        dao.getReviewsForStory(storyId)

    fun hasUserReviewed(storyId: String): Flow<Boolean> =
        dao.hasUserReviewed(storyId, currentUserId)
}