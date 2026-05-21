package com.hanghub.app.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Runs a network [block] and converts any thrown exception into a typed
 * [ApiResult.Failure]. This is the single choke point that gives every
 * repository call uniform success / server-error / network-failure handling.
 *
 * Void endpoints return [ResponseBody]; it is closed here so the connection
 * is released back to the pool.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> =
    try {
        val data = block()
        if (data is ResponseBody) data.close()
        ApiResult.Success(data)
    } catch (e: HttpException) {
        val code = e.code()
        if (code == 401) {
            ApiResult.Failure(ApiError.Unauthorized)
        } else {
            ApiResult.Failure(ApiError.Server(code, parseServerMessage(e)))
        }
    } catch (e: IOException) {
        ApiResult.Failure(ApiError.Network)
    } catch (e: Exception) {
        ApiResult.Failure(ApiError.Unknown(e.message))
    }

/** Extract a human-readable message from a backend error response body. */
private fun parseServerMessage(e: HttpException): String? =
    try {
        val raw = e.response()?.errorBody()?.string()
        if (raw.isNullOrBlank()) {
            null
        } else {
            val obj = errorJson.parseToJsonElement(raw) as? JsonObject
            val error = obj?.get("error")
            when (error) {
                is JsonObject -> error["message"]?.jsonPrimitive?.contentOrNull
                is JsonPrimitive -> error.contentOrNull
                else -> (obj?.get("message") as? JsonPrimitive)?.contentOrNull
            }
        }
    } catch (_: Exception) {
        null
    }
