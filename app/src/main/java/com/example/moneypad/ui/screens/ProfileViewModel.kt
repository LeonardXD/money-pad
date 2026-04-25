package com.example.moneypad.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.data.model.User
import com.example.moneypad.data.model.Conversation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: MoneyPadRepository) : ViewModel() {
    
    val user: StateFlow<User?> = repository.getUser(repository.currentUserId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val storiesPublished: StateFlow<Int> = repository.getPublishedStoriesByAuthor(repository.currentUserId)
        .map { stories -> stories.size }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val conversations: StateFlow<List<Conversation>> = repository.getConversations(repository.currentUserId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getUser(userId: String): Flow<User?> = repository.getUser(userId)

    fun getConversations(authorId: String): Flow<List<Conversation>> = repository.getConversations(authorId)

    fun getReplies(parentId: String): Flow<List<Conversation>> = repository.getReplies(parentId)

    fun updateProfile(bio: String, profileImageUrl: String?, coverImageUrl: String?) {
        viewModelScope.launch {
            repository.updateUserProfile(bio, profileImageUrl, coverImageUrl)
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
            if (isFollowing) {
                repository.unfollowUser(followedId)
            } else {
                repository.followUser(followedId)
            }
        }
    }

    fun logout() {
        repository.logout()
    }
}
