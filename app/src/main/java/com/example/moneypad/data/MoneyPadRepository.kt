package com.example.moneypad.data

import com.example.moneypad.data.dao.MoneyPadDao
import com.example.moneypad.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MoneyPadRepository(private val dao: MoneyPadDao) {

    // Dynamic current user session
    var currentUserId: String = ""
    var currentUsername: String = ""

    suspend fun signup(username: String, email: String, password: String): Result<User> {
        // Check if username already exists
        if (dao.getUserByUsername(username) != null) {
            return Result.failure(Exception("Username already taken"))
        }
        // Check if email already exists
        if (dao.getUserByEmail(email) != null) {
            return Result.failure(Exception("Email already registered"))
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            username = username,
            email = email,
            password = password
        )
        dao.insertUser(user)
        
        // Set current session after successful signup
        currentUserId = user.id
        currentUsername = user.username
        
        return Result.success(user)
    }

    fun logout() {
        currentUserId = ""
        currentUsername = ""
    }

    suspend fun login(identifier: String, password: String): Result<User> {
        val user = dao.login(identifier, password)
        return if (user != null) {
            currentUserId = user.id
            currentUsername = user.username
            Result.success(user)
        } else {
            Result.failure(Exception("Invalid username or password"))
        }
    }

    fun getUser(userId: String): Flow<User?> = dao.getUser(userId)

    suspend fun initUser() {
        // Only init if database is empty or for first-time use
        // For now we'll let users signup normally
    }

    fun getAllStories(): Flow<List<Story>> = dao.getAllStories()

    fun getPublishedStoriesByAuthor(authorId: String): Flow<List<Story>> = dao.getPublishedStoriesByAuthor(authorId)

    fun getDraftStoriesByAuthor(authorId: String): Flow<List<Story>> = dao.getDraftStoriesByAuthor(authorId)

    suspend fun getStoryById(storyId: String): Story? = dao.getStoryById(storyId)

    suspend fun createStory(title: String, genres: String, overview: String, coverImageUrl: String?, isPublished: Boolean = false) {
        val story = Story(
            id = UUID.randomUUID().toString(),
            authorId = currentUserId,
            authorName = currentUsername,
            title = title,
            genres = genres,
            overview = overview,
            coverImageUrl = coverImageUrl,
            isPublished = isPublished
        )
        dao.insertStory(story)
    }

    suspend fun publishStory(storyId: String) {
        dao.publishStory(storyId)
    }

    fun getStoriesByGenre(genre: String): Flow<List<Story>> = dao.getStoriesByGenre(genre)

    suspend fun addStoryPart(storyId: String, title: String, content: String, order: Int) {
        val part = StoryPart(
            id = UUID.randomUUID().toString(),
            storyId = storyId,
            title = title,
            content = content,
            order = order
        )
        dao.insertStoryPart(part)
    }

    fun getPartsForStory(storyId: String): Flow<List<StoryPart>> = dao.getPartsForStory(storyId)

    suspend fun recordRead(storyId: String) {
        val story = dao.getStoryById(storyId) ?: return
        dao.incrementReadCount(storyId)
        // Earn $0.0001 per read for the author
        dao.updateBalance(story.authorId, 0.0001)
    }

    suspend fun withdraw(amount: Double, method: String, accountInfo: String): Boolean {
        if (amount < 0.10) return false
        
        // In a real app, check current balance first
        dao.deductBalance(currentUserId, amount)
        val transaction = Transaction(
            id = UUID.randomUUID().toString(),
            userId = currentUserId,
            amount = amount,
            method = method,
            accountInfo = accountInfo
        )
        dao.insertTransaction(transaction)
        return true
    }

    fun getTransactions(userId: String): Flow<List<Transaction>> = dao.getTransactionsForUser(userId)

    fun searchStories(query: String, genre: String = "All"): Flow<List<Story>> = dao.searchStories(query, genre)
    
    fun searchAuthors(query: String): Flow<List<User>> = dao.searchAuthors(query)

    suspend fun updateUserProfile(bio: String, profileImageUrl: String?, coverImageUrl: String?) {
        dao.updateUserProfile(currentUserId, bio, profileImageUrl, coverImageUrl)
    }

    suspend fun sendMessage(authorId: String, message: String, parentId: String? = null) {
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            authorId = authorId,
            senderId = currentUserId,
            senderName = currentUsername,
            message = message,
            parentId = parentId
        )
        dao.insertConversation(conversation)
    }

    fun getConversations(authorId: String): Flow<List<Conversation>> = dao.getConversationsForAuthor(authorId)
    
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

    fun isFollowing(followedId: String): Flow<Boolean> = dao.isFollowing(currentUserId, followedId)
}
