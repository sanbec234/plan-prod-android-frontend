package com.hanghub.app.core.network

import com.hanghub.app.data.dto.AddMembersRequest
import com.hanghub.app.data.dto.AddPlaceRequest
import com.hanghub.app.data.dto.AuraResponse
import com.hanghub.app.data.dto.AuthResponse
import com.hanghub.app.data.dto.ChatDto
import com.hanghub.app.data.dto.CheckContactsRequest
import com.hanghub.app.data.dto.CreateChatRequest
import com.hanghub.app.data.dto.CreatePlanRequest
import com.hanghub.app.data.dto.DeviceTokenRequest
import com.hanghub.app.data.dto.DiscoveryResponse
import com.hanghub.app.data.dto.FriendRequestBody
import com.hanghub.app.data.dto.FriendRequestsResponse
import com.hanghub.app.data.dto.FriendsResponse
import com.hanghub.app.data.dto.GoogleAuthRequest
import com.hanghub.app.data.dto.GroupsResponse
import com.hanghub.app.data.dto.LocationRequest
import com.hanghub.app.data.dto.MessageDto
import com.hanghub.app.data.dto.NotificationReadRequest
import com.hanghub.app.data.dto.NotificationsResponse
import com.hanghub.app.data.dto.PlanCreateResponse
import com.hanghub.app.data.dto.PlanDto
import com.hanghub.app.data.dto.ProfileDto
import com.hanghub.app.data.dto.RequestIdBody
import com.hanghub.app.data.dto.RsvpRequest
import com.hanghub.app.data.dto.SendMessageRequest
import com.hanghub.app.data.dto.SentRequestsResponse
import com.hanghub.app.data.dto.TransitionRequest
import com.hanghub.app.data.dto.UpdateGroupRequest
import com.hanghub.app.data.dto.UpdateProfileRequest
import com.hanghub.app.data.dto.UsersResponse
import com.hanghub.app.data.dto.VoteRequest
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit declaration of every backend endpoint the iOS app consumes.
 * Endpoints whose response body the app does not need return [ResponseBody]
 * (a raw passthrough); [safeApiCall] closes it.
 */
interface ApiService {

    // ── Auth ────────────────────────────────────────────────────────────────
    @POST("auth/google")
    suspend fun googleAuth(@Body body: GoogleAuthRequest): AuthResponse

    // ── Profile ─────────────────────────────────────────────────────────────
    @GET("api/v1/profile/me")
    suspend fun getMyProfile(): ProfileDto

    @PATCH("api/v1/profile/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ProfileDto

    @GET("api/v1/profile/{userId}")
    suspend fun getProfile(@Path("userId") userId: String): ProfileDto

    // ── Plans ───────────────────────────────────────────────────────────────
    @GET("plans")
    suspend fun getPlans(): List<PlanDto>

    @POST("plans")
    suspend fun createPlan(@Body body: CreatePlanRequest): PlanCreateResponse

    @POST("plans/{id}/add-place")
    suspend fun addPlace(@Path("id") planId: String, @Body body: AddPlaceRequest): ResponseBody

    @POST("plans/{id}/vote")
    suspend fun vote(@Path("id") planId: String, @Body body: VoteRequest): ResponseBody

    @POST("plans/{id}/rsvp")
    suspend fun rsvp(@Path("id") planId: String, @Body body: RsvpRequest): ResponseBody

    @POST("plans/{id}/transition")
    suspend fun transition(@Path("id") planId: String, @Body body: TransitionRequest): ResponseBody

    @DELETE("plans/{id}")
    suspend fun deletePlan(@Path("id") planId: String): ResponseBody

    // ── Friends ─────────────────────────────────────────────────────────────
    @GET("friends")
    suspend fun getFriends(): FriendsResponse

    @GET("friend-requests")
    suspend fun getFriendRequests(): FriendRequestsResponse

    @GET("friend-requests/sent")
    suspend fun getSentRequests(): SentRequestsResponse

    @POST("friend-request")
    suspend fun sendFriendRequest(@Body body: FriendRequestBody): ResponseBody

    @POST("friend-accept")
    suspend fun acceptFriendRequest(@Body body: RequestIdBody): ResponseBody

    @POST("friend-decline")
    suspend fun declineFriendRequest(@Body body: RequestIdBody): ResponseBody

    @DELETE("friend-request/{requestId}")
    suspend fun cancelFriendRequest(@Path("requestId") requestId: String): ResponseBody

    @DELETE("friends/{friendId}")
    suspend fun removeFriend(@Path("friendId") friendId: String): ResponseBody

    // ── Users ───────────────────────────────────────────────────────────────
    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): UsersResponse

    @POST("users/check-contacts")
    suspend fun checkContacts(@Body body: CheckContactsRequest): UsersResponse

    @POST("users/location")
    suspend fun updateLocation(@Body body: LocationRequest): ResponseBody

    // ── Chats ───────────────────────────────────────────────────────────────
    @GET("chats")
    suspend fun getChats(): List<ChatDto>

    @GET("groups")
    suspend fun getGroups(): GroupsResponse

    @GET("chats/{id}/messages")
    suspend fun getMessages(@Path("id") chatId: String): List<MessageDto>

    @POST("chats")
    suspend fun createChat(@Body body: CreateChatRequest): ChatDto

    @PATCH("chats/{id}")
    suspend fun updateGroup(@Path("id") chatId: String, @Body body: UpdateGroupRequest): ChatDto

    @POST("chats/{id}/members")
    suspend fun addMembers(@Path("id") chatId: String, @Body body: AddMembersRequest): ChatDto

    @DELETE("chats/{id}/members/{userId}")
    suspend fun removeMember(
        @Path("id") chatId: String,
        @Path("userId") userId: String,
    ): ResponseBody

    @POST("messages")
    suspend fun sendMessage(@Body body: SendMessageRequest): ResponseBody

    // ── Discovery ───────────────────────────────────────────────────────────
    @GET("api/discovery/places")
    suspend fun getDiscoveryPlaces(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Int = 2000,
        @Query("limit") limit: Int = 30,
        @Query("vibe") vibe: String? = null,
    ): DiscoveryResponse

    // ── Notifications ───────────────────────────────────────────────────────
    @GET("notifications")
    suspend fun getNotifications(): NotificationsResponse

    @POST("notifications/read")
    suspend fun markNotificationRead(@Body body: NotificationReadRequest): ResponseBody

    @POST("notifications/device-token")
    suspend fun registerDeviceToken(@Body body: DeviceTokenRequest): ResponseBody

    // ── Aura ────────────────────────────────────────────────────────────────
    @GET("aura")
    suspend fun getAura(): AuraResponse
}
