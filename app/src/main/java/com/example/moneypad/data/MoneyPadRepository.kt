package com.example.moneypad.data

import android.content.Context
import com.example.moneypad.data.dao.MoneyPadDao
import com.example.moneypad.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID

private const val SESSION_DURATION_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
private const val MINIMUM_ONBOARDING_AGE = 16
private const val MAX_PREFERRED_GENRES = 5

private val DEFAULT_STORY_GENRES = listOf(
    "Romance", "Fantasy", "Mystery", "Sci-Fi", "Horror",
    "Action", "LGBTQIA+", "Werewolf", "New Adult", "Short Story",
    "Teen Fiction", "Historical Fiction", "Paranormal", "Humor",
    "Contemporary Lit", "Diverse Lit", "Thriller", "Adventure",
    "Fan Fiction", "Non-Fiction", "Poetry"
)

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

    suspend fun isUsernameTaken(username: String): Boolean {
        return dao.getUserByUsername(username) != null
    }

    suspend fun isEmailTaken(email: String): Boolean {
        return dao.getUserByEmail(email) != null
    }

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

    fun getCurrentUser(): Flow<User?> = dao.getUser(currentUserId)

    suspend fun initUser() {
        // Expire session if older than 7 days
        if (currentUserId.isNotEmpty() && !hasActiveSession()) {
            logout()
        }
    }

    suspend fun earnReaderCoins(amount: Int) {
        if (currentUserId.isNotEmpty()) {
            dao.updateReaderCoins(currentUserId, amount)
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

    fun getAvailableGenres(): Flow<List<String>> = dao.getAllStoryGenres().map { genreRows ->
        val discovered = genreRows
            .flatMap { row -> row.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        (discovered + DEFAULT_STORY_GENRES).distinct()
    }

    suspend fun saveOnboardingGender(gender: String): Result<Unit> {
        val normalizedGender = gender.trim()
        val validGenders = setOf("Male", "Female", "Others")
        if (normalizedGender !in validGenders) {
            return Result.failure(Exception("Choose a gender to continue"))
        }

        dao.updateOnboardingGender(currentUserId, normalizedGender)
        return Result.success(Unit)
    }

    suspend fun saveOnboardingBirthday(birthday: String): Result<Unit> {
        if (birthday.isBlank()) {
            return Result.failure(Exception("Choose your birthday to continue"))
        }

        val birthDate = try {
            LocalDate.parse(birthday, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: DateTimeParseException) {
            return Result.failure(Exception("Enter a valid birthday"))
        }

        val minimumBirthDate = LocalDate.now().minusYears(MINIMUM_ONBOARDING_AGE.toLong())
        if (birthDate.isAfter(minimumBirthDate)) {
            return Result.failure(Exception("You must be at least 16 years old"))
        }

        dao.updateOnboardingBirthday(currentUserId, birthday)
        return Result.success(Unit)
    }

    suspend fun saveOnboardingGenres(genres: List<String>): Result<Unit> {
        val selectedGenres = genres.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (selectedGenres.size > MAX_PREFERRED_GENRES) {
            return Result.failure(Exception("Choose up to 5 genres"))
        }

        dao.updatePreferredGenres(currentUserId, selectedGenres.joinToString(","))
        return Result.success(Unit)
    }

    suspend fun completeOnboarding(genres: List<String>): Result<Unit> {
        val genreResult = saveOnboardingGenres(genres)
        if (genreResult.isFailure) return genreResult

        val user = dao.getUserByUsername(currentUsername)
            ?: return Result.failure(Exception("User session expired"))
        if (user.gender.isBlank()) {
            dao.updateOnboardingStep(currentUserId, 1)
            return Result.failure(Exception("Choose a gender to continue"))
        }
        val birthdayResult = saveOnboardingBirthday(user.birthday)
        if (birthdayResult.isFailure) {
            dao.updateOnboardingStep(currentUserId, 2)
            return birthdayResult
        }

        dao.markOnboardingCompleted(currentUserId)
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
                isPublished = isPublished, isCompleted = isCompleted, isMature = isMature,
                lastUpdatedAt = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun publishStory(storyId: String) = dao.publishStory(storyId, System.currentTimeMillis())

    suspend fun unpublishStory(storyId: String) = dao.unpublishStory(storyId, System.currentTimeMillis())

    suspend fun deleteStory(storyId: String) {
        dao.deleteStory(storyId)
        dao.deleteStoryParts(storyId)
    }

    fun getStoriesByGenre(genre: String): Flow<List<Story>> = dao.getStoriesByGenre(genre)

    fun getUpdatedStories(): Flow<List<Story>> = dao.getUpdatedStories()

    fun getRecentlyReadStoryIds(): Flow<List<String>> = dao.getRecentlyReadStoryIds(currentUserId)

    fun getReadPartsCount(storyId: String): Flow<Int> = dao.getReadPartsCountForStory(currentUserId, storyId)

    suspend fun addStoryPart(storyId: String, title: String, content: String, order: Int, partId: String? = null, isPublished: Boolean = false, headerImageUrl: String? = null) {
        val now = System.currentTimeMillis()
        dao.insertStoryPart(
            StoryPart(
                id = partId ?: UUID.randomUUID().toString(),
                storyId = storyId, title = title, content = content, order = order,
                publishedAt = now,
                isPublished = isPublished,
                headerImageUrl = headerImageUrl
            )
        )
        if (isPublished) {
            dao.publishStory(storyId, now)
        }
    }

    suspend fun updateStoryPart(partId: String, storyId: String, title: String, content: String, order: Int, isPublished: Boolean, headerImageUrl: String? = null) {
        val now = System.currentTimeMillis()
        dao.insertStoryPart(
            StoryPart(
                id = partId,
                storyId = storyId,
                title = title,
                content = content,
                order = order,
                publishedAt = now,
                isPublished = isPublished,
                headerImageUrl = headerImageUrl
            )
        )
        if (isPublished) {
            dao.publishStory(storyId, now)
        } else {
            val publishedCount = dao.getPublishedPartCount(storyId)
            if (publishedCount == 0) {
                dao.unpublishStory(storyId, now)
            }
        }
    }

    suspend fun deleteStoryPart(partId: String) {
        val part = dao.getStoryPartById(partId)
        if (part != null) {
            dao.deleteStoryPart(partId)
            val publishedCount = dao.getPublishedPartCount(part.storyId)
            if (publishedCount == 0) {
                dao.unpublishStory(part.storyId, System.currentTimeMillis())
            }
        }
    }

    fun getPartsForStory(storyId: String): Flow<List<StoryPart>> = dao.getPartsForStory(storyId)

    fun getPublishedPartsForStory(storyId: String): Flow<List<StoryPart>> = dao.getPublishedPartsForStory(storyId)

    suspend fun recordRead(storyId: String) {
        val story = dao.getStoryById(storyId) ?: return
        
        // Only track views and credit income if reader is NOT the author
        if (currentUserId != story.authorId) {
            val lastRead = dao.getLastReadTimestamp(currentUserId, storyId)
            val now = System.currentTimeMillis()
            
            if (lastRead == null) {
                // First time reading this story - Unique View
                dao.incrementUniqueViews(storyId)
                dao.incrementReadCount(storyId)
                dao.updateAuthorIncome(story.authorId, 0.001)
            } else if (now - lastRead > 30 * 60 * 1000) {
                // Came back after at least 30 minutes - Repeated View
                dao.incrementRepeatedViews(storyId)
                dao.incrementReadCount(storyId)
                dao.updateAuthorIncome(story.authorId, 0.001)
            }
            // If they read another part within 30 minutes, we don't increment anything
            // to avoid over-counting during a single session.
        }
    }

    suspend fun recordPartRead(storyId: String, partId: String) {
        dao.incrementPartReadCount(partId)
        dao.insertUserReadPart(
            com.example.moneypad.data.model.UserReadPart(
                userId = currentUserId,
                partId = partId,
                storyId = storyId,
                readAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun withdraw(amount: Double, method: String, accountInfo: String, source: String = ""): Boolean {
        if (amount <= 0) return false
        
        if (source == "AUTHOR") {
            dao.deductAuthorIncome(currentUserId, amount)
        } else if (source == "READER") {
            // For readers, amount is in PHP, coins = amount * 100
            val coinsToDeduct = (amount * 100).toInt()
            dao.deductReaderCoins(currentUserId, coinsToDeduct)
        } else {
            dao.deductBalance(currentUserId, amount)
        }
        
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
        dao.searchStoriesExcludingAuthor(query, genre, currentUserId)

    fun searchAuthors(query: String): Flow<List<User>> = dao.searchAuthorsExcludingSelf(query, currentUserId)

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

    // ── Likes ────────────────────────────────────────────────────────────────

    fun isStoryLikedByUser(storyId: String): Flow<Boolean> =
        dao.isStoryLikedByUser(currentUserId, storyId)

    suspend fun toggleStoryLike(storyId: String) {
        val alreadyLiked = dao.isStoryLikedByUser(currentUserId, storyId).firstOrNull() ?: false
        if (alreadyLiked) {
            dao.deleteStoryLike(currentUserId, storyId)
        } else {
            dao.insertStoryLike(UserStoryLike(currentUserId, storyId))
        }
        dao.updateStoryLikesCount(storyId)
    }

    // ── Part Annotations ──────────────────────────────────────────────────────

    suspend fun addPartAnnotation(
        partId: String,
        selectedText: String,
        startIndex: Int,
        endIndex: Int,
        type: String,
        content: String? = null
    ) {
        dao.insertPartAnnotation(
            PartAnnotation(
                id = UUID.randomUUID().toString(),
                partId = partId,
                userId = currentUserId,
                username = currentUsername,
                selectedText = selectedText,
                startIndex = startIndex,
                endIndex = endIndex,
                type = type,
                content = content
            )
        )
    }

    fun getAnnotationsForPart(partId: String): Flow<List<PartAnnotation>> =
        dao.getAnnotationsForPart(partId)

    // ── Library ───────────────────────────────────────────────────────────────

    suspend fun addStoryToLibrary(storyId: String): Result<Unit> {
        val count = dao.getLibraryStoryCount(currentUserId).firstOrNull() ?: 0
        if (count >= 15) {
            return Result.failure(Exception("Library is full. Please remove a story before downloading again."))
        }
        dao.insertLibraryStory(LibraryStory(currentUserId, storyId))
        return Result.success(Unit)
    }

    suspend fun removeStoryFromLibrary(storyId: String) {
        dao.deleteLibraryStory(currentUserId, storyId)
    }

    fun getLibraryStories(): Flow<List<Story>> = dao.getLibraryStories(currentUserId)

    fun isStoryInLibrary(storyId: String): Flow<Boolean> = dao.isStoryInLibrary(currentUserId, storyId)
}
