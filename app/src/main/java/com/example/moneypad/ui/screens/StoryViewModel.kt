package com.example.moneypad.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.data.model.Story
import com.example.moneypad.data.model.StoryPart
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class StoryViewModel(private val repository: MoneyPadRepository) : ViewModel() {

    val stories: StateFlow<List<Story>> = repository.getAllStories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val myPublishedStories: StateFlow<List<Story>> = repository.getPublishedStoriesByAuthor(repository.currentUserId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val myDraftStories: StateFlow<List<Story>> = repository.getDraftStoriesByAuthor(repository.currentUserId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentStory = MutableStateFlow<Story?>(null)
    val currentStory = _currentStory.asStateFlow()

    private val _currentParts = MutableStateFlow<List<StoryPart>>(emptyList())
    val currentParts = _currentParts.asStateFlow()

    private val _reviews = MutableStateFlow<List<com.example.moneypad.data.model.Review>>(emptyList())
    val reviews = _reviews.asStateFlow()

    // Holds the ID of the most recently created story so navigation can pick it up
    private val _lastCreatedStoryId = MutableStateFlow<String?>(null)
    val lastCreatedStoryId: StateFlow<String?> = _lastCreatedStoryId.asStateFlow()

    fun clearLastCreatedStoryId() {
        _lastCreatedStoryId.value = null
    }

    fun getStoryById(id: String) {
        viewModelScope.launch {
            _currentStory.value = repository.getStoryById(id)
        }
        viewModelScope.launch {
            repository.getPartsForStory(id).collect {
                _currentParts.value = it
            }
        }
        viewModelScope.launch {
            repository.getReviewsForStory(id).collect {
                _reviews.value = it
            }
        }
    }

    fun addReview(storyId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            repository.addReview(storyId, rating, comment)
        }
    }

    fun addStory(
        title: String,
        genres: String,
        overview: String,
        coverImageUrl: String? = null,
        isPublished: Boolean = false
    ) {
        viewModelScope.launch {
            val id = repository.createStoryAndReturnId(title, genres, overview, coverImageUrl, isPublished)
            _lastCreatedStoryId.value = id
        }
    }

    fun publishStory(storyId: String) {
        viewModelScope.launch {
            repository.publishStory(storyId)
        }
    }

    fun getStoriesByGenre(genre: String): Flow<List<Story>> = repository.getStoriesByGenre(genre)

    fun addPartToStory(storyId: String, partTitle: String, content: String) {
        viewModelScope.launch {
            val currentPartsCount = _currentParts.value.size
            repository.addStoryPart(storyId, partTitle, content, currentPartsCount + 1)
        }
    }

    // Saves current part as draft (does the same as addPartToStory — stored but not published)
    fun savePartAsDraft(storyId: String, partTitle: String, content: String) {
        if (partTitle.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val currentPartsCount = _currentParts.value.size
            repository.addStoryPart(
                storyId,
                partTitle.ifBlank { "Untitled Chapter" },
                content,
                currentPartsCount + 1
            )
        }
    }

    fun recordRead(storyId: String) {
        viewModelScope.launch {
            repository.recordRead(storyId)
        }
    }

    fun search(query: String, genre: String = "All"): Flow<List<Story>> =
        repository.searchStories(query, genre)

    fun searchAuthors(query: String) = repository.searchAuthors(query)

    fun getStoriesByAuthor(authorId: String): Flow<List<Story>> =
        repository.getPublishedStoriesByAuthor(authorId)
}