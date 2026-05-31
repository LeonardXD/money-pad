package com.example.moneypad.data.model

data class UpdateAdFreeRequest(
    val userId: String,
    val coinsDelta: Int? = null,
    val timestamp: Long? = null,
    val permanent: Boolean? = null
)

data class ToggleConversationLikeRequest(
    val conversationId: String,
    val delta: Int,
    val userId: String
)

data class AddStoryToLibraryRequest(
    val userId: String,
    val storyId: String,
    val downloadedAt: Long
)

data class AddStoryToReadingListRequest(
    val listId: String,
    val storyId: String,
    val addedAt: Long
)

data class WithdrawRequest(
    val id: String,
    val userId: String,
    val amount: Double,
    val method: String,
    val accountInfo: String,
    val source: String,
    val timestamp: Long
)

data class RecordAdWatchRequest(
    val id: String,
    val userId: String,
    val watchedAt: Long
)
