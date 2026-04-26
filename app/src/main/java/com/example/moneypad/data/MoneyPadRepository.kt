package com.example.moneypad.data

import android.content.Context
import com.example.moneypad.data.dao.MoneyPadDao
import com.example.moneypad.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

private const val SESSION_DURATION_MS = 7L * 24 * 60 * 60 * 1000 // 7 days

class MoneyPadRepository(private val context: Context, private val dao: MoneyPadDao) {

    private val sharedPreferences = context.getSharedPreferences("moneypad_prefs", Context.MODE_PRIVATE)

    var currentUserId: String = sharedPreferences.getString("userId", "") ?: ""
    var currentUsername: String = sharedPreferences.getString("username", "") ?: ""

    /** Returns true if there is an active (< 7 days old) saved session */
    fun hasActiveSession(): Boolean {
        if (currentUserId.isEmpty()) return false
        val ts = sharedPreferences.getLong("loginTimestamp", 0L)
        return (System.currentTimeMillis() - ts) < SESSION_DURATION_MS
    }

    private fun saveLoginState(userId: String, username: String) {
        val now = System.currentTimeMillis()
        sharedPreferences.edit()
            .putString("userId", userId)
            .putString("username", username)
            .putLong("loginTimestamp", now)
            .apply()
    }

    private fun clearLoginState() {
        sharedPreferences.edit()
            .remove("userId")
            .remove("username")
            .remove("loginTimestamp")
            .apply()
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun signup(
        username: String,
        email: String,
        password: String,
        referrerUsername: String = ""
    ): Result<User> {
        if (dao.getUserByUsername(username) != null)
            return Result.failure(Exception("Username already taken"))
        if (dao.getUserByEmail(email) != null)
            return Result.failure(Exception("Email already registered"))

        // Validate referrer if provided
        val validatedReferrer = if (referrerUsername.isNotBlank()) {
            val referrer = dao.getUserByUsername(referrerUsername)
            if (referrer == null) return Result.failure(Exception("Referrer username not found"))
            referrerUsername
        } else ""

        val user = User(
            id = UUID.randomUUID().toString(),
            username = username,
            email = email,
            password = password,
            referredBy = validatedReferrer,
            loginTimestamp = System.currentTimeMillis()
        )
        dao.insertUser(user)

        // Credit referrer
        if (validatedReferrer.isNotBlank()) {
            dao.incrementReferralCount(validatedReferrer)
        }

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
            val now = System.currentTimeMillis()
            dao.updateLoginTimestamp(user.id, now)
            saveLoginState(currentUserId, currentUsername)
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid username or password"))
        }
    }

    fun getUser(userId: String): Flow<User?> = dao.getUser(userId)

    suspend fun initUser() {
        // Expire session if older than 7 days
        if (currentUserId.isNotEmpty() && !hasActiveSession()) {
            logout()
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    /**
     * Validates the new username isn't taken by someone else, then persists settings.
     */
    suspend fun updateSettings(
        username: String,
        birthday: String,
        gender: String,
        preferredGenres: String
    ): Result<Unit> {
        val existing = dao.getUserByUsername(username)
        if (existing != null && existing.id != currentUserId) {
            return Result.failure(Exception("Username already taken"))
        }
        dao.updateUserSettings(currentUserId, username, birthday, gender, preferredGenres)
        currentUsername = username
        sharedPreferences.edit().putString("username", username).apply()
        return Result.success(Unit)
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val stored = dao.getPassword(currentUserId)
        if (stored != currentPassword) return Result.failure(Exception("Current password is incorrect"))
        dao.updatePassword(currentUserId, newPassword)
        return Result.success(Unit)
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    suspend fun updateUserProfile(bio: String, profileImageUrl: String?, coverImageUrl: String?) {
        dao.updateUserProfile(currentUserId, bio, profileImageUrl, coverImageUrl)
    }

    // ── Social ────────────────────────────────────────────────────────────────

    fun getFollowers(userId: String): Flow<List<User>> = dao.getFollowers(userId)
    fun getFollowing(userId: String): Flow<List<User>> = dao.getFollowing(userId)

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

    // ── Stories ───────────────────────────────────────────────────────────────

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

    // ── Conversations ─────────────────────────────────────────────────────────

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

    // ── Reviews ───────────────────────────────────────────────────────────────

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