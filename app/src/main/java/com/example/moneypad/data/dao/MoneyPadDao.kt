package com.example.moneypad.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.moneypad.data.model.Review
import com.example.moneypad.data.model.Story
import com.example.moneypad.data.model.StoryPart
import com.example.moneypad.data.model.Transaction
import com.example.moneypad.data.model.User
import com.example.moneypad.data.model.Conversation
import com.example.moneypad.data.model.Follow
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyPadDao {
    // ── User ─────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUser(userId: String): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("UPDATE users SET balance = balance + :amount WHERE id = :userId")
    suspend fun updateBalance(userId: String, amount: Double)

    @Query("UPDATE users SET balance = balance - :amount WHERE id = :userId")
    suspend fun deductBalance(userId: String, amount: Double)

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE (username = :identifier OR email = :identifier) AND password = :password")
    suspend fun login(identifier: String, password: String): User?

    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%'")
    fun searchAuthors(query: String): Flow<List<User>>

    @Query("UPDATE users SET bio = :bio, profileImageUrl = :profileImageUrl, coverImageUrl = :coverImageUrl WHERE id = :userId")
    suspend fun updateUserProfile(userId: String, bio: String, profileImageUrl: String?, coverImageUrl: String?)

    @Query("UPDATE users SET username = :username, birthday = :birthday, gender = :gender, preferredGenres = :preferredGenres WHERE id = :userId")
    suspend fun updateUserSettings(userId: String, username: String, birthday: String, gender: String, preferredGenres: String)

    @Query("UPDATE users SET gender = :gender, onboardingStep = MAX(onboardingStep, 2) WHERE id = :userId")
    suspend fun updateOnboardingGender(userId: String, gender: String)

    @Query("UPDATE users SET birthday = :birthday, onboardingStep = MAX(onboardingStep, 3) WHERE id = :userId")
    suspend fun updateOnboardingBirthday(userId: String, birthday: String)

    @Query("UPDATE users SET preferredGenres = :preferredGenres WHERE id = :userId")
    suspend fun updatePreferredGenres(userId: String, preferredGenres: String)

    @Query("UPDATE users SET onboardingStep = :step WHERE id = :userId")
    suspend fun updateOnboardingStep(userId: String, step: Int)

    @Query("UPDATE users SET onboardingCompleted = 1, onboardingStep = 3 WHERE id = :userId")
    suspend fun markOnboardingCompleted(userId: String)

    @Query("UPDATE users SET password = :newPassword WHERE id = :userId")
    suspend fun updatePassword(userId: String, newPassword: String)

    @Query("SELECT password FROM users WHERE id = :userId")
    suspend fun getPassword(userId: String): String?

    @Query("UPDATE users SET loginTimestamp = :timestamp WHERE id = :userId")
    suspend fun updateLoginTimestamp(userId: String, timestamp: Long)

    @Query("UPDATE users SET referralCount = referralCount + 1 WHERE username = :username")
    suspend fun incrementReferralCount(username: String)

    @Query("UPDATE users SET followers = followers + :delta WHERE id = :userId")
    suspend fun updateFollowers(userId: String, delta: Int)

    @Query("UPDATE users SET following = following + :delta WHERE id = :userId")
    suspend fun updateFollowing(userId: String, delta: Int)

    // ── Follows ───────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: Follow)

    @Query("DELETE FROM follows WHERE followerId = :followerId AND followedId = :followedId")
    suspend fun deleteFollow(followerId: String, followedId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE followerId = :followerId AND followedId = :followedId)")
    fun isFollowing(followerId: String, followedId: String): Flow<Boolean>

    /** Returns list of User objects who follow the given user */
    @Query("SELECT * FROM users WHERE id IN (SELECT followerId FROM follows WHERE followedId = :userId)")
    fun getFollowers(userId: String): Flow<List<User>>

    /** Returns list of User objects that the given user follows */
    @Query("SELECT * FROM users WHERE id IN (SELECT followedId FROM follows WHERE followerId = :userId)")
    fun getFollowing(userId: String): Flow<List<User>>

    // ── Conversations ─────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: Conversation)

    @Query("SELECT * FROM conversations WHERE authorId = :authorId AND parentId IS NULL ORDER BY timestamp DESC")
    fun getConversationsForAuthor(authorId: String): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE parentId = :parentId ORDER BY timestamp ASC")
    fun getReplies(parentId: String): Flow<List<Conversation>>

    // ── Stories ───────────────────────────────────────────────────────────────
    @Query("SELECT * FROM stories WHERE isPublished = 1")
    fun getAllStories(): Flow<List<Story>>

    @Query("SELECT genres FROM stories WHERE genres != ''")
    fun getAllStoryGenres(): Flow<List<String>>

    @Query("SELECT * FROM stories WHERE authorId = :authorId AND isPublished = 1 ORDER BY rowid DESC")
    fun getPublishedStoriesByAuthor(authorId: String): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE authorId = :authorId AND isPublished = 0")
    fun getDraftStoriesByAuthor(authorId: String): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE id = :storyId")
    suspend fun getStoryById(storyId: String): Story?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)

    @Query("UPDATE stories SET readCount = readCount + 1 WHERE id = :storyId")
    suspend fun incrementReadCount(storyId: String)

    @Query("UPDATE stories SET isPublished = 1 WHERE id = :storyId")
    suspend fun publishStory(storyId: String)

    @Query("SELECT * FROM stories WHERE (title LIKE '%' || :query || '%' OR genres LIKE '%' || :query || '%') AND (genres LIKE '%' || :genre || '%' OR :genre = 'All') AND isPublished = 1")
    fun searchStories(query: String, genre: String): Flow<List<Story>>

    @Query("SELECT * FROM stories WHERE genres LIKE '%' || :genre || '%' AND isPublished = 1")
    fun getStoriesByGenre(genre: String): Flow<List<Story>>

    // ── Story Parts ───────────────────────────────────────────────────────────
    @Query("SELECT * FROM story_parts WHERE storyId = :storyId ORDER BY `order` ASC")
    fun getPartsForStory(storyId: String): Flow<List<StoryPart>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryPart(part: StoryPart)

    // ── Transactions ──────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: String): Flow<List<Transaction>>

    // ── Reviews ───────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)

    @Query("SELECT * FROM reviews WHERE storyId = :storyId ORDER BY timestamp DESC")
    fun getReviewsForStory(storyId: String): Flow<List<Review>>

    @Query("SELECT EXISTS(SELECT 1 FROM reviews WHERE storyId = :storyId AND userId = :userId)")
    fun hasUserReviewed(storyId: String, userId: String): Flow<Boolean>
}
