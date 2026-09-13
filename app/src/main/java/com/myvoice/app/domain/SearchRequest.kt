package com.myvoice.app.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parses the LLM's answer to the "should we search the web?" decision prompt.
 * Tolerates markdown fences and stray text around the JSON object.
 */
@Serializable
data class SearchRequest(val search: String? = null, val query: String? = null) {

    val queryOrNull: String?
        get() = (search ?: query)?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun parse(raw: String?): SearchRequest? {
            val text = raw?.trim().orEmpty()
            if (text.isEmpty()) return null
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            return runCatching {
                json.decodeFromString<SearchRequest>(text.substring(start, end + 1))
            }.getOrNull()
        }
    }
}
