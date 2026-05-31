package com.example.moneypad.data.remote

import com.example.moneypad.data.model.*
import com.example.moneypad.data.model.PublishStoryRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface MoneyPadApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("auth/auth.php?action=login")
    suspend fun login(@Body body: Map<String, String>): Response<User>

    @POST("auth/auth.php?action=signup")
    suspend fun signup(@Body body: Map<String, String>): Response<User>

    @POST("auth/auth.php?action=check_username")
    suspend fun checkUsername(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("auth/auth.php?action=check_email")
    suspend fun checkEmail(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("auth/auth.php?action=change_password")
    suspend fun changePassword(@Body body: Map<String, String>): Response<Map<String, Boolean>>


    // ── Users ─────────────────────────────────────────────────────────────────
    @GET("users/users.php?action=get_user")
    suspend fun getUser(@Query("userId") userId: String): Response<User>

    @POST("users/users.php?action=update_profile")
    suspend fun updateProfile(@Body body: Map<String, String?>): Response<Map<String, Boolean>>

    @POST("users/users.php?action=update_settings")
    suspend fun updateSettings(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("users/users.php?action=onboarding_gender")
    suspend fun onboardingGender(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("users/users.php?action=onboarding_birthday")
    suspend fun onboardingBirthday(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("users/users.php?action=onboarding_genres")
    suspend fun onboardingGenres(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("users/users.php?action=complete_onboarding")
    suspend fun completeOnboarding(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @GET("users/users.php?action=search_authors")
    suspend fun searchAuthors(
        @Query("query") query: String,
        @Query("excludeUserId") excludeUserId: String
    ): Response<List<User>>

    @POST("users/users.php?action=verify_user")
    suspend fun verifyUser(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("users/users.php?action=update_ad_free")
    suspend fun updateAdFree(@Body body: Map<String, Any>): Response<Map<String, Boolean>>


    // ── Stories ───────────────────────────────────────────────────────────────
    @GET("stories/stories.php?action=list_all")
    suspend fun getAllStories(): Response<List<Story>>

    @GET("stories/stories.php?action=get_by_id")
    suspend fun getStoryById(@Query("storyId") storyId: String): Response<Story>

    @POST("stories/stories.php?action=create")
    suspend fun createStory(@Body story: Story): Response<Map<String, Any>>

    @POST("stories/stories.php?action=update")
    suspend fun updateStory(@Body story: Story): Response<Map<String, Boolean>>

    @POST("stories/stories.php?action=delete")
    suspend fun deleteStory(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("stories/stories.php?action=publish")
    suspend fun publishStory(@Body body: PublishStoryRequest): Response<Map<String, Boolean>>

    @POST("stories/stories.php?action=unpublish")
    suspend fun unpublishStory(@Body body: PublishStoryRequest): Response<Map<String, Boolean>>

    @GET("stories/stories.php?action=list_by_author_published")
    suspend fun getPublishedStoriesByAuthor(@Query("authorId") authorId: String): Response<List<Story>>

    @GET("stories/stories.php?action=list_by_author_drafts")
    suspend fun getDraftStoriesByAuthor(@Query("authorId") authorId: String): Response<List<Story>>

    @GET("stories/stories.php?action=search")
    suspend fun searchStories(
        @Query("query") query: String,
        @Query("genre") genre: String,
        @Query("excludeAuthorId") excludeAuthorId: String
    ): Response<List<Story>>

    @GET("stories/stories.php?action=get_genres")
    suspend fun getAvailableGenres(): Response<List<String>>


    // ── Parts ─────────────────────────────────────────────────────────────────
    @GET("parts/parts.php?action=list_parts")
    suspend fun getPartsForStory(
        @Query("storyId") storyId: String,
        @Query("onlyPublished") onlyPublished: Boolean
    ): Response<List<StoryPart>>

    @GET("parts/parts.php?action=get_part_by_id")
    suspend fun getStoryPartById(@Query("partId") partId: String): Response<StoryPart>

    @POST("parts/parts.php?action=add_part")
    suspend fun addStoryPart(@Body part: StoryPart): Response<Map<String, Any>>

    @POST("parts/parts.php?action=update_part")
    suspend fun updateStoryPart(@Body part: StoryPart): Response<Map<String, Boolean>>

    @POST("parts/parts.php?action=delete_part")
    suspend fun deleteStoryPart(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("parts/parts.php?action=record_read")
    suspend fun recordRead(@Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("parts/parts.php?action=record_part_read")
    suspend fun recordPartRead(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("parts/parts.php?action=record_part_view")
    suspend fun recordPartView(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @GET("parts/parts.php?action=get_published_count")
    suspend fun getPublishedPartCount(@Query("storyId") storyId: String): Response<Map<String, Int>>


    // ── Interactions ──────────────────────────────────────────────────────────
    @POST("interactions/interactions.php?action=follow")
    suspend fun followUser(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("interactions/interactions.php?action=unfollow")
    suspend fun unfollowUser(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @GET("interactions/interactions.php?action=is_following")
    suspend fun isFollowing(
        @Query("followerId") followerId: String,
        @Query("followedId") followedId: String
    ): Response<Map<String, Boolean>>

    @GET("interactions/interactions.php?action=get_followers")
    suspend fun getFollowers(@Query("userId") userId: String): Response<List<User>>

    @GET("interactions/interactions.php?action=get_following")
    suspend fun getFollowing(@Query("userId") userId: String): Response<List<User>>

    @POST("interactions/interactions.php?action=send_message")
    suspend fun sendMessage(@Body message: Conversation): Response<Map<String, Boolean>>

    @GET("interactions/interactions.php?action=get_conversations")
    suspend fun getConversations(@Query("authorId") authorId: String): Response<List<Conversation>>

    @GET("interactions/interactions.php?action=get_replies")
    suspend fun getReplies(@Query("parentId") parentId: String): Response<List<Conversation>>

    @POST("interactions/interactions.php?action=toggle_conversation_like")
    suspend fun toggleConversationLike(@Body body: Map<String, Any>): Response<Map<String, Boolean>>

    @POST("interactions/interactions.php?action=add_review")
    suspend fun addReview(@Body review: Review): Response<Map<String, Boolean>>

    @GET("interactions/interactions.php?action=get_reviews")
    suspend fun getReviewsForStory(@Query("storyId") storyId: String): Response<List<Review>>

    @GET("interactions/interactions.php?action=has_reviewed")
    suspend fun hasUserReviewed(
        @Query("storyId") storyId: String,
        @Query("userId") userId: String
    ): Response<Map<String, Boolean>>

    @POST("interactions/interactions.php?action=toggle_story_like")
    suspend fun toggleStoryLike(@Body body: Map<String, String>): Response<Map<String, Any>>

    @GET("interactions/interactions.php?action=is_story_liked")
    suspend fun isStoryLikedByUser(
        @Query("userId") userId: String,
        @Query("storyId") storyId: String
    ): Response<Map<String, Boolean>>

    @POST("interactions/interactions.php?action=add_annotation")
    suspend fun addPartAnnotation(@Body annotation: PartAnnotation): Response<Map<String, Boolean>>

    @GET("interactions/interactions.php?action=get_annotations")
    suspend fun getAnnotationsForPart(@Query("partId") partId: String): Response<List<PartAnnotation>>


    // ── Library ───────────────────────────────────────────────────────────────
    @POST("library/library.php?action=add_library")
    suspend fun addStoryToLibrary(@Body body: Map<String, Any>): Response<Map<String, Boolean>>

    @POST("library/library.php?action=remove_library")
    suspend fun removeStoryFromLibrary(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @GET("library/library.php?action=get_library")
    suspend fun getLibraryStories(@Query("userId") userId: String): Response<List<Story>>

    @POST("library/library.php?action=create_reading_list")
    suspend fun createReadingList(@Body list: ReadingList): Response<Map<String, Boolean>>

    @POST("library/library.php?action=delete_reading_list")
    suspend fun deleteReadingList(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @GET("library/library.php?action=get_reading_lists")
    suspend fun getReadingLists(@Query("userId") userId: String): Response<List<ReadingList>>

    @POST("library/library.php?action=add_to_reading_list")
    suspend fun addStoryToReadingList(@Body body: Map<String, Any>): Response<Map<String, Boolean>>

    @POST("library/library.php?action=remove_from_reading_list")
    suspend fun removeStoryFromReadingList(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @GET("library/library.php?action=get_reading_list_stories")
    suspend fun getStoriesForReadingList(@Query("listId") listId: String): Response<List<Story>>


    // ── Transactions ──────────────────────────────────────────────────────────
    @POST("transactions/transactions.php?action=withdraw")
    suspend fun withdraw(@Body body: Map<String, Any>): Response<Map<String, Boolean>>

    @GET("transactions/transactions.php?action=get_transactions")
    suspend fun getTransactions(@Query("userId") userId: String): Response<List<Transaction>>

    @POST("transactions/transactions.php?action=claim_referral_reward")
    suspend fun claimReferralReward(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("transactions/transactions.php?action=record_ad_watch")
    suspend fun recordAdWatch(@Body body: Map<String, Any>): Response<Map<String, Any>>

    @GET("transactions/transactions.php?action=get_referral_stats")
    suspend fun getReferralStats(@Query("username") username: String): Response<Map<String, Any>>


    // ── Notifications ─────────────────────────────────────────────────────────
    @GET("notifications/notifications.php?action=list_notifications")
    suspend fun getNotifications(@Query("userId") userId: String): Response<List<Notification>>

    @POST("notifications/notifications.php?action=create_notification")
    suspend fun createNotification(@Body notification: Notification): Response<Map<String, Boolean>>

    @GET("notifications/notifications.php?action=get_unread_count")
    suspend fun getUnreadNotificationCount(@Query("userId") userId: String): Response<Map<String, Int>>

    @POST("notifications/notifications.php?action=mark_read")
    suspend fun markNotificationAsRead(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @POST("notifications/notifications.php?action=mark_all_read")
    suspend fun markAllNotificationsAsRead(@Body body: Map<String, String>): Response<Map<String, Boolean>>

    @Multipart
    @POST("upload.php")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<Map<String, String>>
}
