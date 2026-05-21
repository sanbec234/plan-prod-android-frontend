package com.hanghub.app.core.network

/**
 * Outcome of a single network operation. Every repository call returns one of
 * these so callers can render success / error / network-failure states without
 * try/catch leaking into the UI layer. (Loading is tracked separately by the
 * ViewModel's UI state.)
 */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/** Typed failure categories, each carrying a user-presentable [message]. */
sealed class ApiError(val message: String) {
    /** No connectivity / socket failure — distinct so the UI can show an offline state. */
    data object Network : ApiError("No internet connection. Check your network and try again.")

    /** 401 — the session token is missing, expired, or rejected. */
    data object Unauthorized : ApiError("Your session expired. Please sign in again.")

    /** Any other non-2xx HTTP response. */
    class Server(val code: Int, serverMessage: String?) :
        ApiError(serverMessage ?: "Server error ($code).")

    /** Unexpected client-side failure (serialization, etc.). */
    class Unknown(detail: String?) : ApiError(detail ?: "Something went wrong. Please try again.")
}

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(data)
    return this
}

inline fun <T> ApiResult<T>.onFailure(block: (ApiError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) block(error)
    return this
}

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

/** Transform a success value, leaving failures untouched. */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

/** Discard a success value — used for endpoints whose response body is ignored. */
fun <T> ApiResult<T>.discardValue(): ApiResult<Unit> = map { }
