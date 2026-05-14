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

    companion object {
        const val OFFICIAL_USER_ID = "moneypad_official_id"
        const val OFFICIAL_USERNAME = "moneypad"
        const val OFFICIAL_EMAIL = "moneypad@moneypad.com"
        const val OFFICIAL_PASSWORD = "@Moneypad3014"
        const val MIN_PUBLISHED_PARTS_TO_PUBLISH_STORY = 1
    }

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

    private suspend fun ensureOfficialUserExists() {
        val existing = dao.getUser(OFFICIAL_USER_ID).firstOrNull()
        if (existing == null) {
            val officialUser = User(
                id = OFFICIAL_USER_ID,
                username = OFFICIAL_USERNAME,
                email = OFFICIAL_EMAIL,
                password = OFFICIAL_PASSWORD,
                bio = "Official Money Pad Account",
                onboardingCompleted = true,
                onboardingStep = 3
            )
            dao.insertUser(officialUser)
        }
    }

    // ── Notifications ────────────────────────────────────────────────────────

    fun getNotifications(): Flow<List<Notification>> = dao.getNotificationsForUser(currentUserId)

    fun getUnreadNotificationCount(): Flow<Int> = dao.getUnreadNotificationCount(currentUserId)

    suspend fun markNotificationAsRead(notificationId: String) = dao.markNotificationAsRead(notificationId)

    suspend fun markAllNotificationsAsRead() = dao.markAllNotificationsAsRead(currentUserId)

    private suspend fun notifyFollowers(type: String, story: Story? = null, part: StoryPart? = null) {
        val followers = dao.getFollowers(currentUserId).firstOrNull() ?: emptyList()
        val currentUser = dao.getUser(currentUserId).firstOrNull()
        followers.forEach { follower ->
            dao.insertNotification(
                Notification(
                    id = UUID.randomUUID().toString(),
                    userId = follower.id,
                    type = type,
                    actorId = currentUserId,
                    actorName = currentUsername,
                    actorProfileImageUrl = currentUser?.profileImageUrl,
                    storyId = story?.id,
                    storyTitle = story?.title,
                    partId = part?.id,
                    partTitle = part?.title,
                    isActorVerified = currentUser?.isVerified == true || currentUserId == OFFICIAL_USER_ID
                )
            )
        }
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
            signupTimestamp = System.currentTimeMillis(),
            loginTimestamp = System.currentTimeMillis()
        )
        dao.insertUser(user)

        // Welcome Notification
        dao.insertNotification(
            Notification(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                type = "WELCOME",
                actorId = "SYSTEM",
                actorName = "Money Pad Team",
                isActorVerified = true
            )
        )

        // Previous Announcements Notification for New User
        val announcements = dao.getConversationsForAuthor(OFFICIAL_USER_ID).firstOrNull() ?: emptyList()
        announcements.forEach { announcement ->
            dao.insertNotification(
                Notification(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    type = "CONVERSATION",
                    actorId = OFFICIAL_USER_ID,
                    actorName = OFFICIAL_USERNAME,
                    actorProfileImageUrl = null, // Can fetch if needed, but official logo is default
                    storyId = OFFICIAL_USER_ID,
                    partId = announcement.id,
                    content = announcement.message,
                    timestamp = announcement.timestamp,
                    isActorVerified = true
                )
            )
        }

        // Credit referrer
        if (validatedReferrer.isNotBlank()) {
            dao.incrementReferralCount(validatedReferrer)
        }

        currentUserId = user.id
        currentUsername = user.username
        saveLoginState(currentUserId, currentUsername)

        // Auto-follow official account
        followUser(OFFICIAL_USER_ID)

        return Result.success(user)
    }

    suspend fun updateReferrer(referrerUsername: String): Result<Unit> {
        val referrer = dao.getUserByUsername(referrerUsername)
        if (referrer == null) return Result.failure(Exception("Referrer username not found"))
        if (referrerUsername == currentUsername) return Result.failure(Exception("You cannot refer yourself"))
        
        dao.updateReferrer(currentUserId, referrerUsername)
        dao.incrementReferralCount(referrerUsername)
        return Result.success(Unit)
    }

    suspend fun claimReferralReward() {
        if (currentUserId.isNotEmpty()) {
            dao.updateReaderCoins(currentUserId, 10)
            dao.markReferralRewardClaimed(currentUserId)
        }
    }

    fun logout() {
        currentUserId = ""
        currentUsername = ""
        clearLoginState()
    }

    suspend fun login(identifier: String, password: String): Result<User> {
        // Handle hardcoded official user login
        if ((identifier == OFFICIAL_USERNAME || identifier == OFFICIAL_EMAIL) && password == OFFICIAL_PASSWORD) {
            ensureOfficialUserExists()
            val user = dao.getUser(OFFICIAL_USER_ID).firstOrNull()
            return if (user != null) {
                currentUserId = user.id
                currentUsername = user.username
                val now = System.currentTimeMillis()
                dao.updateLoginTimestamp(user.id, now)
                saveLoginState(currentUserId, currentUsername)
                Result.success(user)
            } else {
                Result.failure(Exception("Critical Error: Official user not found"))
            }
        }

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
        // Ensure official user exists
        ensureOfficialUserExists()

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
        
        // Sync user info across other tables
        val user = dao.getUser(currentUserId).firstOrNull() ?: return Result.success(Unit)
        dao.updateConversationsSyncInfo(currentUserId, username, user.profileImageUrl, user.isVerified)
        dao.updateNotificationsSyncInfo(currentUserId, username, user.profileImageUrl, user.isVerified)
        dao.updateReviewsSyncInfo(currentUserId, username, user.isVerified)
        dao.updatePartAnnotationsSyncInfo(currentUserId, username, user.isVerified)
        dao.updateStoriesSyncInfo(currentUserId, username, user.isVerified)
        
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
        
        // Sync user info across other tables
        val user = dao.getUser(currentUserId).firstOrNull() ?: return
        dao.updateConversationsSyncInfo(currentUserId, user.username, profileImageUrl, user.isVerified)
        dao.updateNotificationsSyncInfo(currentUserId, user.username, profileImageUrl, user.isVerified)
        dao.updateReviewsSyncInfo(currentUserId, user.username, user.isVerified)
        dao.updatePartAnnotationsSyncInfo(currentUserId, user.username, user.isVerified)
        dao.updateStoriesSyncInfo(currentUserId, user.username, user.isVerified)
    }

    // ── Social ────────────────────────────────────────────────────────────────

    fun getFollowers(userId: String): Flow<List<User>> = dao.getFollowers(userId)
    fun getFollowing(userId: String): Flow<List<User>> = dao.getFollowing(userId)

    suspend fun followUser(followedId: String) {
        dao.insertFollow(Follow(currentUserId, followedId))
        dao.updateFollowing(currentUserId, 1)
        dao.updateFollowers(followedId, 1)

        // Notify the followed user
        val currentUser = dao.getUser(currentUserId).firstOrNull()
        dao.insertNotification(
            Notification(
                id = UUID.randomUUID().toString(),
                userId = followedId,
                type = "FOLLOW",
                actorId = currentUserId,
                actorName = currentUsername,
                actorProfileImageUrl = currentUser?.profileImageUrl
            )
        )
    }

    suspend fun unfollowUser(followedId: String) {
        if (followedId == OFFICIAL_USER_ID) return // Cannot unfollow official account
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

    fun getStoryByIdFlow(storyId: String): Flow<Story?> = dao.getStoryByIdFlow(storyId)

    suspend fun createStory(
        title: String, genres: String, overview: String, coverImageUrl: String?,
        isPublished: Boolean = false, isCompleted: Boolean = false, isMature: Boolean = false
    ) {
        val storyId = UUID.randomUUID().toString()
        val currentUser = dao.getUser(currentUserId).firstOrNull()
        val story = Story(
            id = storyId,
            authorId = currentUserId, authorName = currentUsername,
            title = title, genres = genres, overview = overview,
            coverImageUrl = coverImageUrl,
            isPublished = isPublished, isCompleted = isCompleted, isMature = isMature,
            isAuthorVerified = currentUser?.isVerified == true || currentUserId == OFFICIAL_USER_ID
        )
        dao.insertStory(story)
        if (isPublished) {
            notifyFollowers("NEW_STORY", story = story)
        }
    }

    suspend fun createStoryAndReturnId(
        title: String, genres: String, overview: String, coverImageUrl: String?,
        isPublished: Boolean = false, isCompleted: Boolean = false, isMature: Boolean = false
    ): String {
        val id = UUID.randomUUID().toString()
        val currentUser = dao.getUser(currentUserId).firstOrNull()
        val story = Story(
            id = id,
            authorId = currentUserId, authorName = currentUsername,
            title = title, genres = genres, overview = overview,
            coverImageUrl = coverImageUrl,
            isPublished = isPublished, isCompleted = isCompleted, isMature = isMature,
            lastUpdatedAt = System.currentTimeMillis(),
            isAuthorVerified = currentUser?.isVerified == true || currentUserId == OFFICIAL_USER_ID
        )
        dao.insertStory(story)
        if (isPublished) {
            notifyFollowers("NEW_STORY", story = story)
        }
        return id
    }

    suspend fun publishStory(storyId: String): Boolean {
        val story = dao.getStoryById(storyId)
        if (story == null || story.isPublished || !canPublishStory(storyId)) {
            return false
        }
        dao.publishStory(storyId, System.currentTimeMillis())
        notifyFollowers("NEW_STORY", story = story)
        return true
    }

    suspend fun unpublishStory(storyId: String) = dao.unpublishStory(storyId, System.currentTimeMillis())

    suspend fun updateStory(story: Story) {
        val oldStory = dao.getStoryById(story.id)
        val currentUser = dao.getUser(currentUserId).firstOrNull()
        val updatedStory = story.copy(
            isAuthorVerified = currentUser?.isVerified == true || currentUserId == OFFICIAL_USER_ID
        )
        dao.insertStory(updatedStory)
        if (updatedStory.isPublished && (oldStory == null || !oldStory.isPublished)) {
            notifyFollowers("NEW_STORY", story = updatedStory)
        }
    }

    suspend fun deleteStory(storyId: String) {
        dao.deleteStory(storyId)
        dao.deleteStoryParts(storyId)
    }

    fun getStoriesByGenre(genre: String): Flow<List<Story>> = dao.getStoriesByGenre(genre)

    fun getUpdatedStories(): Flow<List<Story>> = dao.getUpdatedStories()

    fun getRecentlyReadStoryIds(): Flow<List<String>> = dao.getRecentlyReadStoryIds(currentUserId)

    fun getReadPartsCount(storyId: String): Flow<Int> = dao.getReadPartsCountForStory(currentUserId, storyId)

    suspend fun addStoryPart(storyId: String, title: String, content: String, order: Int, partId: String? = null, isPublished: Boolean = false, headerImageUrl: String? = null): String {
        val now = System.currentTimeMillis()
        val id = partId ?: UUID.randomUUID().toString()
        val oldPart = dao.getStoryPartById(id)
        val part = StoryPart(
            id = id,
            storyId = storyId, title = title, content = content, order = oldPart?.order ?: order,
            publishedAt = getPublishedAtForSave(oldPart, isPublished, now),
            isPublished = isPublished,
            readCount = oldPart?.readCount ?: 0,
            headerImageUrl = headerImageUrl
        )
        dao.insertStoryPart(part)

        if (isPublished) {
            val story = dao.getStoryById(storyId)
            if (story != null) {
                if (!story.isPublished && canPublishStory(storyId)) {
                    dao.publishStory(storyId, now)
                    notifyFollowers("NEW_STORY", story = story)
                } else {
                    dao.updateStoryLastUpdated(storyId, now)
                    if (story.isPublished) {
                        notifyFollowers("NEW_PART", story = story, part = part)
                    }
                }
            }
        } else {
            unpublishStoryIfBelowMinimum(storyId, now)
        }
        return id
    }

    suspend fun updateStoryPart(partId: String, storyId: String, title: String, content: String, order: Int, isPublished: Boolean, headerImageUrl: String? = null) {
        val now = System.currentTimeMillis()
        val oldPart = dao.getStoryPartById(partId)
        val part = StoryPart(
            id = partId,
            storyId = storyId,
            title = title,
            content = content,
            order = oldPart?.order ?: order,
            publishedAt = getPublishedAtForSave(oldPart, isPublished, now),
            isPublished = isPublished,
            readCount = oldPart?.readCount ?: 0,
            headerImageUrl = headerImageUrl
        )
        dao.insertStoryPart(part)

        if (isPublished) {
            val story = dao.getStoryById(storyId)
            if (story != null) {
                if (!story.isPublished && canPublishStory(storyId)) {
                    dao.publishStory(storyId, now)
                    notifyFollowers("NEW_STORY", story = story)
                } else if (oldPart == null || !oldPart.isPublished) {
                    dao.updateStoryLastUpdated(storyId, now)
                    if (story.isPublished) {
                        notifyFollowers("NEW_PART", story = story, part = part)
                    }
                }
            }
        } else {
            unpublishStoryIfBelowMinimum(storyId, now)
        }
    }

    suspend fun deleteStoryPart(partId: String) {
        val part = dao.getStoryPartById(partId)
        if (part != null) {
            dao.deleteStoryPart(partId)
            unpublishStoryIfBelowMinimum(part.storyId, System.currentTimeMillis())
        }
    }

    private suspend fun canPublishStory(storyId: String): Boolean =
        dao.getPublishedPartCount(storyId) >= MIN_PUBLISHED_PARTS_TO_PUBLISH_STORY

    private fun getPublishedAtForSave(oldPart: StoryPart?, isPublished: Boolean, now: Long): Long =
        if (isPublished && oldPart?.isPublished == true) oldPart.publishedAt else now

    private suspend fun unpublishStoryIfBelowMinimum(storyId: String, timestamp: Long) {
        if (!canPublishStory(storyId)) {
            dao.unpublishStory(storyId, timestamp)
        }
    }

    fun getPartsForStory(storyId: String): Flow<List<StoryPart>> = dao.getPartsForStory(storyId)

    fun getPublishedPartsForStory(storyId: String): Flow<List<StoryPart>> = dao.getPublishedPartsForStory(storyId)

    suspend fun recordRead(storyId: String) {
        val story = dao.getStoryById(storyId) ?: return

        val lastRead = dao.getLastReadTimestamp(currentUserId, storyId)
        val now = System.currentTimeMillis()
        val isAuthor = currentUserId == story.authorId

        // For authors, we always allow views to increment for testing/responsiveness
        // For others, we use the 30-minute session rule
        if (isAuthor || lastRead == null || (now - lastRead > 30 * 60 * 1000)) {
            if (lastRead == null) {
                dao.incrementUniqueViews(storyId)
            } else {
                dao.incrementRepeatedViews(storyId)
            }
            dao.incrementReadCount(storyId)

            // Only track income if reader is NOT the author
            if (!isAuthor && lastRead == null) {
                // Fetch author's verification status
                val author = dao.getUser(story.authorId).firstOrNull()
                val rate = if (author?.isVerified == true) 0.0005 else 0.0003
                dao.updateAuthorIncome(story.authorId, rate)
            }
        }
    }

    suspend fun countQualifyingStories(userId: String): Int = dao.countQualifyingStories(userId)

    suspend fun verifyUser(userId: String) {
        dao.verifyUser(userId)
        
        // Sync user info across other tables
        val user = dao.getUser(userId).firstOrNull() ?: return
        dao.updateConversationsSyncInfo(userId, user.username, user.profileImageUrl, true)
        dao.updateNotificationsSyncInfo(userId, user.username, user.profileImageUrl, true)
        dao.updateReviewsSyncInfo(userId, user.username, true)
        dao.updatePartAnnotationsSyncInfo(userId, user.username, true)
        dao.updateStoriesSyncInfo(userId, user.username, true)
    }

    suspend fun upgradeToAdFree90Min(): Boolean {
        if (currentUserId.isEmpty()) return false
        val user = dao.getUser(currentUserId).firstOrNull() ?: return false
        if (user.readerCoins < 500) return false
        
        dao.deductReaderCoins(currentUserId, 500)
        val ninetyMinFromNow = System.currentTimeMillis() + (90 * 60 * 1000)
        dao.updateAdFreeUntil(currentUserId, ninetyMinFromNow)
        return true
    }

    suspend fun upgradeToAdFreePermanent(): Boolean {
        if (currentUserId.isEmpty()) return false
        dao.markAdFreePermanently(currentUserId)
        return true
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
            // Handle 5% commission for the referrer
            val user = dao.getUser(currentUserId).firstOrNull()
            if (user != null && user.referredBy.isNotBlank()) {
                val commission = amount * 0.05
                // We don't need a separate dao call to add commission if we calculate it on the fly, 
                // but we need to ensure the referral stats correctly reflect this as "earned".
            }
        } else if (source == "READER") {
            // For readers, amount is in PHP, coins = amount * 100
            val coinsToDeduct = (amount * 100).toInt()
            dao.deductReaderCoins(currentUserId, coinsToDeduct)
        } else if (source == "REFERRAL") {
            // No direct deduction from user entity fields needed as it is calculated from transactions on the fly.
            // Just need to ensure we don't withdraw more than available.
        } else {
            dao.deductBalance(currentUserId, amount)
        }
        
        dao.insertTransaction(
            Transaction(
                id = UUID.randomUUID().toString(),
                userId = currentUserId, amount = amount,
                method = method, accountInfo = accountInfo,
                source = source
            )
        )
        return true
    }

    fun getTransactions(userId: String): Flow<List<Transaction>> =
        dao.getTransactionsForUser(userId)

    fun getTotalWithdrawalsBySource(userId: String, source: String): Flow<Double> =
        dao.getTotalWithdrawalsBySource(userId, source).map { it ?: 0.0 }

    fun getTotalReferralCoins(username: String): Flow<Int> =
        dao.getTotalReferralCoins(username).map { it ?: 0 }

    fun getReferralAuthorWithdrawals(username: String): Flow<Double> =
        dao.getReferralAuthorWithdrawals(username).map { it ?: 0.0 }

    fun searchStories(query: String, genre: String = "All"): Flow<List<Story>> =
        dao.searchStoriesExcludingAuthor(query, genre, currentUserId).map { stories ->
            if (genre == "All") stories else stories.filter { story -> story.hasGenre(genre) }
        }

    private fun Story.hasGenre(genre: String): Boolean =
        genres.split(",").map { it.trim() }.any { it.equals(genre, ignoreCase = true) }

    fun searchAuthors(query: String): Flow<List<User>> = dao.searchAuthorsExcludingSelf(query, currentUserId)

    // ── Conversations ─────────────────────────────────────────────────────────

    suspend fun sendMessage(authorId: String, message: String, parentId: String? = null) {
        val currentUser = dao.getUser(currentUserId).firstOrNull()
        val conversationId = UUID.randomUUID().toString()
        val isSenderVerified = currentUser?.isVerified == true || currentUserId == OFFICIAL_USER_ID
        dao.insertConversation(
            Conversation(
                id = conversationId,
                authorId = authorId, senderId = currentUserId,
                senderName = currentUsername, message = message, 
                senderProfileImageUrl = currentUser?.profileImageUrl,
                parentId = parentId,
                isSenderVerified = isSenderVerified
            )
        )

        // Notification Logic
        if (parentId == null) {
            // New conversation post
            if (authorId == currentUserId) {
                // User posted on their own wall - Notify followers
                val followers = dao.getFollowers(currentUserId).firstOrNull() ?: emptyList()
                followers.forEach { follower ->
                    dao.insertNotification(
                        Notification(
                            id = UUID.randomUUID().toString(),
                            userId = follower.id,
                            type = "CONVERSATION",
                            actorId = currentUserId,
                            actorName = currentUsername,
                            actorProfileImageUrl = currentUser?.profileImageUrl,
                            storyId = authorId, // Target profile ID
                            partId = conversationId, // For direct navigation
                            content = message,
                            isActorVerified = isSenderVerified
                        )
                    )
                }
            } else {
                // Someone else posted on the user's wall - Notify wall owner
                dao.insertNotification(
                    Notification(
                        id = UUID.randomUUID().toString(),
                        userId = authorId,
                        type = "CONVERSATION",
                        actorId = currentUserId,
                        actorName = currentUsername,
                        actorProfileImageUrl = currentUser?.profileImageUrl,
                        storyId = authorId, // Target profile ID
                        partId = conversationId, // For direct navigation
                        content = message,
                        isActorVerified = isSenderVerified
                    )
                )
            }
        } else {
            // Reply to a conversation
            // 1. Notify the wall owner if the replier is NOT the wall owner
            if (authorId != currentUserId) {
                dao.insertNotification(
                    Notification(
                        id = UUID.randomUUID().toString(),
                        userId = authorId,
                        type = "REPLY",
                        actorId = currentUserId,
                        actorName = currentUsername,
                        actorProfileImageUrl = currentUser?.profileImageUrl,
                        storyId = authorId, // Target profile ID
                        partId = conversationId, // For direct navigation
                        content = message,
                        isActorVerified = isSenderVerified
                    )
                )
            }
            
            // 2. Notify the parent message sender if they are not the replier and not the wall owner (who is already notified)
            val parentConv = dao.getConversationById(parentId)
            if (parentConv != null && parentConv.senderId != currentUserId && parentConv.senderId != authorId) {
                dao.insertNotification(
                    Notification(
                        id = UUID.randomUUID().toString(),
                        userId = parentConv.senderId,
                        type = "REPLY",
                        actorId = currentUserId,
                        actorName = currentUsername,
                        actorProfileImageUrl = currentUser?.profileImageUrl,
                        storyId = authorId, // Target profile ID
                        partId = conversationId, // For direct navigation
                        content = message,
                        isActorVerified = isSenderVerified
                    )
                )
            }
        }

        // 3. Mention Notifications
        val mentionRegex = Regex("@(\\w+)")
        val mentions = mentionRegex.findAll(message).map { it.groupValues[1] }.distinct()
        mentions.forEach { mentionedUsername ->
            if (mentionedUsername != currentUsername) {
                val mentionedUser = dao.getUserByUsername(mentionedUsername)
                if (mentionedUser != null) {
                    dao.insertNotification(
                        Notification(
                            id = UUID.randomUUID().toString(),
                            userId = mentionedUser.id,
                            type = "MENTION",
                            actorId = currentUserId,
                            actorName = currentUsername,
                            actorProfileImageUrl = currentUser?.profileImageUrl,
                            storyId = authorId, // Target profile ID
                            partId = conversationId,
                            content = message,
                            isActorVerified = isSenderVerified
                        )
                    )
                }
            }
        }
    }

    fun getConversations(authorId: String): Flow<List<Conversation>> =
        dao.getConversationsForAuthor(authorId)

    fun getReplies(parentId: String): Flow<List<Conversation>> = dao.getReplies(parentId)

    suspend fun getConversation(id: String): Conversation? = dao.getConversationById(id)

    suspend fun toggleConversationLike(conversationId: String, delta: Int) {
        dao.updateConversationLikes(conversationId, delta)
        
        // Notification for Like
        if (delta > 0) {
            val conv = dao.getConversationById(conversationId)
            val currentUser = dao.getUser(currentUserId).firstOrNull()
            if (conv != null && conv.senderId != currentUserId) {
                dao.insertNotification(
                    Notification(
                        id = UUID.randomUUID().toString(),
                        userId = conv.senderId,
                        type = "CONVERSATION_LIKE",
                        actorId = currentUserId,
                        actorName = currentUsername,
                        actorProfileImageUrl = currentUser?.profileImageUrl,
                        storyId = conv.authorId, // Target profile ID
                        partId = conversationId,
                        isActorVerified = currentUser?.isVerified == true || currentUserId == OFFICIAL_USER_ID
                    )
                )
            }
        }
    }

    // ── Reviews ───────────────────────────────────────────────────────────────

    suspend fun addReview(storyId: String, rating: Int, comment: String) {
        val currentUser = dao.getUser(currentUserId).firstOrNull()
        dao.insertReview(
            Review(
                id = UUID.randomUUID().toString(),
                storyId = storyId, userId = currentUserId,
                username = currentUsername, rating = rating, comment = comment,
                isUserVerified = currentUser?.isVerified == true || currentUserId == OFFICIAL_USER_ID
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
    ): String? {
        val currentUser = dao.getUser(currentUserId).firstOrNull()
        val part = dao.getStoryPartById(partId)
        val storyId = part?.storyId

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
                content = content,
                isUserVerified = currentUser?.isVerified == true || currentUserId == OFFICIAL_USER_ID
            )
        )

        if (type == "COMMENT" && storyId != null) {
            dao.updateStoryCommentsCount(storyId)
        }
        
        return storyId
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

    fun isPartRead(partId: String): Flow<Boolean> = dao.isPartRead(currentUserId, partId)

    // ── Reading Lists ─────────────────────────────────────────────────────────

    suspend fun createReadingList(name: String, description: String = "") {
        val readingList = ReadingList(
            id = UUID.randomUUID().toString(),
            userId = currentUserId,
            name = name,
            description = description
        )
        dao.insertReadingList(readingList)
    }

    suspend fun deleteReadingList(listId: String) {
        dao.deleteReadingList(listId)
    }

    fun getReadingLists(): Flow<List<ReadingList>> = dao.getReadingListsForUser(currentUserId)

    fun getReadingLists(userId: String): Flow<List<ReadingList>> = dao.getReadingListsForUser(userId)

    suspend fun addStoryToReadingList(listId: String, storyId: String) {
        dao.insertReadingListStory(ReadingListStory(listId, storyId))
    }

    suspend fun removeStoryFromReadingList(listId: String, storyId: String) {
        dao.deleteReadingListStory(listId, storyId)
    }

    fun getStoriesForReadingList(listId: String): Flow<List<Story>> = dao.getStoriesForReadingList(listId)

    suspend fun isStoryInReadingList(listId: String, storyId: String): Boolean = dao.isStoryInReadingList(listId, storyId)
}
