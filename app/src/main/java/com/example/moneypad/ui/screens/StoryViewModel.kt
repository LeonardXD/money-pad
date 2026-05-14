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
    val currentUsername: String get() = repository.currentUsername

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

    private val _currentStoryId = MutableStateFlow<String?>(null)
    
    val currentStory: StateFlow<Story?> = _currentStoryId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.getStoryByIdFlow(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentParts: StateFlow<List<StoryPart>> = _currentStoryId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else {
                // We need to check if the user is the author to decide which flow to use
                // Since flatMapLatest can be used with multiple flows, we use a combine or similar
                // but simpler is to just use the reactive flow from DAO which Room provides.
                repository.getCurrentUser().flatMapLatest { user ->
                    repository.getStoryByIdFlow(id).flatMapLatest { story ->
                        if (story?.authorId == user?.id) {
                            repository.getPartsForStory(id)
                        } else {
                            repository.getPublishedPartsForStory(id)
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _reviews = MutableStateFlow<List<com.example.moneypad.data.model.Review>>(emptyList())
    val reviews = _reviews.asStateFlow()

    private val _authorProfileImageUrl = MutableStateFlow<String?>(null)
    val authorProfileImageUrl = _authorProfileImageUrl.asStateFlow()

    // Holds the ID of the most recently created story so navigation can pick it up
    private val _lastCreatedStoryId = MutableStateFlow<String?>(null)
    val lastCreatedStoryId: StateFlow<String?> = _lastCreatedStoryId.asStateFlow()

    fun clearLastCreatedStoryId() {
        _lastCreatedStoryId.value = null
    }

    fun getStoryById(id: String) {
        _currentStoryId.value = id
        
        viewModelScope.launch {
            repository.getStoryById(id)?.let { story ->
                repository.getUser(story.authorId).firstOrNull()?.let { author ->
                    _authorProfileImageUrl.value = author.profileImageUrl
                }
            }
        }
        
        viewModelScope.launch {
            repository.getReviewsForStory(id).collect {
                _reviews.value = it
            }
        }
    }

    fun earnReaderCoins(amount: Int) {
        viewModelScope.launch {
            repository.earnReaderCoins(amount)
        }
    }

    val user: StateFlow<com.example.moneypad.data.model.User?> = repository.getCurrentUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun getUser(userId: String) = repository.getUser(userId)

    fun claimReferralReward() {
        viewModelScope.launch {
            repository.claimReferralReward()
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

    fun updateStory(
        storyId: String,
        title: String,
        genres: String,
        overview: String,
        coverImageUrl: String? = null,
        isCompleted: Boolean = false,
        isMature: Boolean = false
    ) {
        viewModelScope.launch {
            val existing = repository.getStoryById(storyId) ?: return@launch
            val updated = existing.copy(
                title = title,
                genres = genres,
                overview = overview,
                coverImageUrl = coverImageUrl ?: existing.coverImageUrl,
                isCompleted = isCompleted,
                isMature = isMature,
                lastUpdatedAt = System.currentTimeMillis()
            )
            repository.updateStory(updated)
            // Trigger navigation or success state
            _lastCreatedStoryId.value = storyId
        }
    }

    fun publishStory(storyId: String) {
        viewModelScope.launch {
            repository.publishStory(storyId)
        }
    }

    fun unpublishStory(storyId: String) {
        viewModelScope.launch {
            repository.unpublishStory(storyId)
        }
    }

    fun getStoriesByGenre(genre: String): Flow<List<Story>> = repository.getStoriesByGenre(genre)

    fun addPartToStory(storyId: String, partTitle: String, content: String, partId: String? = null, isPublished: Boolean = false, headerImageUrl: String? = null) {
        viewModelScope.launch {
            val currentPartsCount = currentParts.value.size
            repository.addStoryPart(storyId, partTitle, content, currentPartsCount + 1, partId, isPublished, headerImageUrl)
        }
    }

    // Saves current part as draft (does the same as addPartToStory — stored but not published)
    fun savePartAsDraft(storyId: String, partTitle: String, content: String, partId: String? = null, headerImageUrl: String? = null) {
        if (partTitle.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val currentPartsCount = currentParts.value.size
            repository.addStoryPart(
                storyId,
                partTitle.ifBlank { "Untitled Chapter" },
                content,
                currentPartsCount + 1,
                partId,
                isPublished = false,
                headerImageUrl = headerImageUrl
            )
        }
    }

    fun updatePartStatus(storyId: String, partId: String, title: String, content: String, isPublished: Boolean, headerImageUrl: String? = null) {
        viewModelScope.launch {
            val part = currentParts.value.find { it.id == partId }
            val order = part?.order ?: (currentParts.value.size + 1)
            repository.updateStoryPart(partId, storyId, title, content, order, isPublished, headerImageUrl)
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
            currentStory.value?.id?.let {
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
            val storyId = repository.addPartAnnotation(partId, selectedText, startIndex, endIndex, type, content)
            // Refresh annotations to show the new one
            getAnnotationsForPart(partId)
            // Refresh current story to update comment count
            storyId?.let { getStoryById(it) }
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

    fun isStoryInLibrary(storyId: String): Flow<Boolean> = repository.isStoryInLibrary(storyId)

    fun isPartRead(partId: String): Flow<Boolean> = repository.isPartRead(partId)

    // ── Notifications ────────────────────────────────────────────────────────

    val notifications = repository.getNotifications().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unreadNotificationCount = repository.getUnreadNotificationCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    // ── Reading Lists ─────────────────────────────────────────────────────────

    val readingLists = repository.getReadingLists().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createReadingList(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createReadingList(name, description)
        }
    }

    fun deleteReadingList(listId: String) {
        viewModelScope.launch {
            repository.deleteReadingList(listId)
        }
    }

    fun addStoryToReadingList(listId: String, storyId: String) {
        viewModelScope.launch {
            repository.addStoryToReadingList(listId, storyId)
        }
    }

    fun removeStoryFromReadingList(listId: String, storyId: String) {
        viewModelScope.launch {
            repository.removeStoryFromReadingList(listId, storyId)
        }
    }

    fun getStoriesForReadingList(listId: String): Flow<List<Story>> = repository.getStoriesForReadingList(listId)

    suspend fun isStoryInReadingList(listId: String, storyId: String): Boolean = repository.isStoryInReadingList(listId, storyId)
}