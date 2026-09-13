package com.myvoice.app.data.remote

import com.myvoice.app.core.AssistantException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class TavilyClient(private val http: OkHttpClient) {

    /**
     * Web search via Tavily's /search endpoint.
     */
    suspend fun search(query: String, apiKey: String): TavilyResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw AssistantException("No Tavily API key set — add one in Settings → Web search.")
        }
        val call = Request.Builder()
            .url("https://api.tavily.com/search")
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .post(jsonBody(TavilyRequest(query = query)))
            .build()

        http.newCall(call).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw httpError(resp.code, raw, "Tavily")
            runCatching { appJson.decodeFromString<TavilyResponse>(raw) }
                .getOrElse { throw AssistantException("Could not parse Tavily response: ${it.message}") }
        }
    }
}
