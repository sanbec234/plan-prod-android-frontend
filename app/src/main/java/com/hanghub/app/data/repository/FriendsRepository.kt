package com.hanghub.app.data.repository

import com.hanghub.app.core.network.ApiResult
import com.hanghub.app.core.network.ApiService
import com.hanghub.app.core.network.discardValue
import com.hanghub.app.core.network.map
import com.hanghub.app.core.network.safeApiCall
import com.hanghub.app.data.dto.CheckContactsRequest
import com.hanghub.app.data.dto.FriendDto
import com.hanghub.app.data.dto.FriendRequestBody
import com.hanghub.app.data.dto.FriendRequestDto
import com.hanghub.app.data.dto.RequestIdBody
import com.hanghub.app.data.dto.SearchUserDto
import com.hanghub.app.data.dto.SentRequestDto

/**
 * Friends + friend-request operations — wraps the friends, friend-request and
 * users endpoints. Mirrors the iOS FriendsAPIService.
 */
class FriendsRepository(private val api: ApiService) {

    suspend fun getFriends(): ApiResult<List<FriendDto>> =
        safeApiCall { api.getFriends() }.map { it.friends }

    suspend fun getFriendRequests(): ApiResult<List<FriendRequestDto>> =
        safeApiCall { api.getFriendRequests() }.map { it.requests }

    suspend fun getSentRequests(): ApiResult<List<SentRequestDto>> =
        safeApiCall { api.getSentRequests() }.map { it.requests }

    suspend fun sendFriendRequest(toId: String, note: String? = null): ApiResult<Unit> =
        safeApiCall {
            api.sendFriendRequest(FriendRequestBody(toId, note?.trim()?.ifEmpty { null }))
        }.discardValue()

    suspend fun acceptFriendRequest(requestId: String): ApiResult<Unit> =
        safeApiCall { api.acceptFriendRequest(RequestIdBody(requestId)) }.discardValue()

    suspend fun declineFriendRequest(requestId: String): ApiResult<Unit> =
        safeApiCall { api.declineFriendRequest(RequestIdBody(requestId)) }.discardValue()

    suspend fun cancelFriendRequest(requestId: String): ApiResult<Unit> =
        safeApiCall { api.cancelFriendRequest(requestId) }.discardValue()

    suspend fun removeFriend(friendId: String): ApiResult<Unit> =
        safeApiCall { api.removeFriend(friendId) }.discardValue()

    suspend fun searchUsers(query: String): ApiResult<List<SearchUserDto>> =
        safeApiCall { api.searchUsers(query) }.map { it.users }

    suspend fun checkContacts(emails: List<String>): ApiResult<List<SearchUserDto>> =
        safeApiCall { api.checkContacts(CheckContactsRequest(emails)) }.map { it.users }
}
