package com.example.moneypad.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneypad.data.MoneyPadRepository
import com.example.moneypad.data.model.Story
import com.example.moneypad.data.model.StoryPart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class StoryViewModel(private val repository: MoneyPadRepository) : ViewModel() {

    val currentUserId: String get() = repository.currentUserId

    val stories: StateFlow<List<Story>> = repository.getAllStories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val myPublishedStories: StateFlow<List<Story>> = repository.getPublishedStoriesByAuthor(repository.currentUserId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val myDraftStories: StateFlow<List<Story>> = repository.getDraftStoriesByAuthor(repository.currentUserId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val updatedStories: StateFlow<List<Story>> = repository.getUpdatedStories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val continueReadingStories: StateFlow<List<Pair<Story, Float>>> = repository.getRecentlyReadStoryIds()
        .flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList<Pair<Story, Float>>())
            else {
                val flows = ids.map { id ->
                    val storyFlow = flow { emit(repository.getStoryById(id)) }
                    val progressFlow = repository.getReadPartsCount(id)
                    val partsFlow = repository.getPartsForStory(id)
                    
                    combine(storyFlow, progressFlow, partsFlow) { story, readCount, parts ->
                        if (story != null && (story.isPublished || story.authorId == repository.currentUserId) && parts.isNotEmpty()) {
                            val publishedParts = if (story.authorId == repository.currentUserId) parts else parts.filter { it.isPublished }
                            val total = publishedParts.size.coerceAtLeast(1)
                            story to (readCount.toFloat() / total).coerceIn(0f, 1f)
                        } else null
                    }
                }
                combine(flows) { it.filterNotNull() }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recommendedStories: StateFlow<List<Story>> = repository.getCurrentUser()
        .flatMapLatest { user ->
            val genres = user?.preferredGenres?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            if (genres.isEmpty()) repository.getAllStories()
            else {
                combine(genres.map { repository.getStoriesByGenre(it) }) { lists ->
                    lists.flatMap { it }.distinctBy { it.id }.filter { it.authorId != repository.currentUserId && it.isPublished }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
            val story = repository.getStoryById(id)
            _currentStory.value = story

            // Fetch parts based on author vs reader
            val partsFlow = if (story?.authorId == repository.currentUserId) {
                repository.getPartsForStory(id)
            } else {
                repository.getPublishedPartsForStory(id)
            }
            
            partsFlow.collect {
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
            getStoryById(storyId)
        }
    }

    fun unpublishStory(storyId: String) {
        viewModelScope.launch {
            repository.unpublishStory(storyId)
            getStoryById(storyId)
        }
    }

    fun getStoriesByGenre(genre: String): Flow<List<Story>> = repository.getStoriesByGenre(genre)

    fun addPartToStory(storyId: String, partTitle: String, content: String, partId: String? = null, isPublished: Boolean = false) {
        viewModelScope.launch {
            val currentPartsCount = _currentParts.value.size
            repository.addStoryPart(storyId, partTitle, content, currentPartsCount + 1, partId, isPublished)
            if (isPublished) {
                getStoryById(storyId)
            }
        }
    }

    // Saves current part as draft (does the same as addPartToStory — stored but not published)
    fun savePartAsDraft(storyId: String, partTitle: String, content: String, partId: String? = null) {
        if (partTitle.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val currentPartsCount = _currentParts.value.size
            repository.addStoryPart(
                storyId,
                partTitle.ifBlank { "Untitled Chapter" },
                content,
                currentPartsCount + 1,
                partId,
                isPublished = false
            )
        }
    }

    fun updatePartStatus(storyId: String, partId: String, title: String, content: String, isPublished: Boolean) {
        viewModelScope.launch {
            val part = _currentParts.value.find { it.id == partId }
            val order = part?.order ?: (_currentParts.value.size + 1)
            repository.updateStoryPart(partId, storyId, title, content, order, isPublished)
            getStoryById(storyId)
        }
    }

    fun recordRead(storyId: String) {
        viewModelScope.launch {
            repository.recordRead(storyId)
        }
    }

    fun recordPartRead(storyId: String, partId: String) {
        viewModelScope.launch {
            repository.recordPartRead(storyId, partId)
        }
    }

    fun deletePart(partId: String) {
        viewModelScope.launch {
            repository.deleteStoryPart(partId)
            // Reload parts for the current story
            _currentStory.value?.id?.let {
                getStoryById(it)
            }
        }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            repository.deleteStory(storyId)
        }
    }

    fun search(query: String, genre: String = "All"): Flow<List<Story>> =
        repository.searchStories(query, genre)

    fun searchAuthors(query: String) = repository.searchAuthors(query)

    fun getStoriesByAuthor(authorId: String): Flow<List<Story>> =
        repository.getPublishedStoriesByAuthor(authorId)

    fun isStoryLiked(storyId: String): Flow<Boolean> = repository.isStoryLikedByUser(storyId)

    fun toggleLike(storyId: String) {
        viewModelScope.launch {
            repository.toggleStoryLike(storyId)
            // Refresh current story to update like count
            _currentStory.value = repository.getStoryById(storyId)
        }
    }

    private val _partAnnotations = MutableStateFlow<List<com.example.moneypad.data.model.PartAnnotation>>(emptyList())
    val partAnnotations = _partAnnotations.asStateFlow()

    fun getAnnotationsForPart(partId: String) {
        viewModelScope.launch {
            repository.getAnnotationsForPart(partId).collect {
                _partAnnotations.value = it
            }
        }
    }

    fun addPartAnnotation(
        partId: String,
        selectedText: String,
        startIndex: Int,
        endIndex: Int,
        type: String,
        content: String? = null
    ) {
        viewModelScope.launch {
            repository.addPartAnnotation(partId, selectedText, startIndex, endIndex, type, content)
            // Refresh annotations to show the new one
            getAnnotationsForPart(partId)
        }
    }

    // ── Library ───────────────────────────────────────────────────────────────

    val libraryStories = repository.getLibraryStories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun downloadStoryToLibrary(storyId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.addStoryToLibrary(storyId)
            onResult(result)
        }
    }

    fun removeStoryFromLibrary(storyId: String) {
        viewModelScope.launch {
            repository.removeStoryFromLibrary(storyId)
        }
    }

    fun isStoryInLibrary(storyId: String) = repository.isStoryInLibrary(storyId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
}