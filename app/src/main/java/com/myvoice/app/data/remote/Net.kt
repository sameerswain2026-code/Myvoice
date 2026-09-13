package com.myvoice.app.data.remote

import com.myvoice.app.core.AssistantException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Shared JSON configuration for all network clients. */
val appJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

inline fun <reified T> jsonBody(value: T): RequestBody =
    appJson.encodeToString(value).toRequestBody("application/json; charset=utf-8".toMediaType())

fun rawJsonBody(json: String): RequestBody =
    json.toRequestBody("application/json; charset=utf-8".toMediaType())

/** Best-effort extraction of an API error message into a user-facing exception. */
fun httpError(code: Int, rawBody: String, service: String): AssistantException {
    val detail = runCatching {
        appJson.decodeFromString<GeminiResponse>(rawBody).error?.message
    }.getOrNull()
    return if (!detail.isNullOrBlank()) {
        AssistantException("$service: $detail")
    } else {
        AssistantException("$service request failed (HTTP $code): ${rawBody.take(180)}")
    }
}
