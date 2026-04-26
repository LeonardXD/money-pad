package com.example.moneypad.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.data.model.Conversation
import com.example.moneypad.data.model.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: MoneyPadRepository) : ViewModel() {

    val user: StateFlow<User?> = repository.getUser(repository.currentUserId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val storiesPublished: StateFlow<Int> = repository.getPublishedStoriesByAuthor(repository.currentUserId)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val conversations: StateFlow<List<Conversation>> =
        repository.getConversations(repository.currentUserId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Followers / Following lists
    val followers: StateFlow<List<User>> = repository.getFollowers(repository.currentUserId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val following: StateFlow<List<User>> = repository.getFollowing(repository.currentUserId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Settings result
    private val _settingsResult = MutableSharedFlow<Result<Unit>>(replay = 0)
    val settingsResult: SharedFlow<Result<Unit>> = _settingsResult

    // Change-password result
    private val _passwordResult = MutableSharedFlow<Result<Unit>>(replay = 0)
    val passwordResult: SharedFlow<Result<Unit>> = _passwordResult

    fun getUser(userId: String): Flow<User?> = repository.getUser(userId)

    fun getConversations(authorId: String): Flow<List<Conversation>> =
        repository.getConversations(authorId)

    fun getReplies(parentId: String): Flow<List<Conversation>> = repository.getReplies(parentId)

    fun getFollowers(userId: String): Flow<List<User>> = repository.getFollowers(userId)

    fun getFollowing(userId: String): Flow<List<User>> = repository.getFollowing(userId)

    fun updateProfile(bio: String, profileImageUrl: String?, coverImageUrl: String?) {
        viewModelScope.launch {
            repository.updateUserProfile(bio, profileImageUrl, coverImageUrl)
        }
    }

    fun saveSettings(username: String, birthday: String, gender: String, preferredGenres: String) {
        viewModelScope.launch {
            val result = repository.updateSettings(username, birthday, gender, preferredGenres)
            _settingsResult.emit(result)
        }
    }

    fun changePassword(current: String, new: String) {
        viewModelScope.launch {
            val result = repository.changePassword(current, new)
            _passwordResult.emit(result)
        }
    }

    fun sendMessage(authorId: String, message: String, parentId: String? = null) {
        viewModelScope.launch {
            repository.sendMessage(authorId, message, parentId)
        }
    }

    fun isFollowing(followedId: String): Flow<Boolean> = repository.isFollowing(followedId)

    fun toggleFollow(followedId: String, isFollowing: Boolean) {
        viewModelScope.launch {
            if (isFollowing) repository.unfollowUser(followedId)
            else repository.followUser(followedId)
        }
    }

    fun logout() {
        repository.logout()
    }

    val currentUserId: String get() = repository.currentUserId

    fun getPublishedStoriesForCurrentUser(): Flow<List<com.example.moneypad.data.model.Story>> =
        repository.getPublishedStoriesByAuthor(repository.currentUserId)
}