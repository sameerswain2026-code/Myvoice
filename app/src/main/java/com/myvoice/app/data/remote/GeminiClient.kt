package com.myvoice.app.data.remote

import com.myvoice.app.core.AssistantException
import com.myvoice.app.data.prefs.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GeminiClient(
    private val http: OkHttpClient,
    private val settings: SettingsRepository
) {

    /**
     * One-shot text generation. Returns the concatenated text of all response parts.
     */
    suspend fun generate(
        system: String,
        user: String,
        temperature: Double = 0.7,
        jsonMode: Boolean = false,
        maxOutputTokens: Int = 2048
    ): String = withContext(Dispatchers.IO) {
        val s = settings.current()
        val key = s.geminiKey.trim()
        if (key.isEmpty()) {
            throw AssistantException("No Gemini API key set — add one in Settings → Thinking model.")
        }
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart(text = user)))
            ),
            systemInstruction = GeminiSystemInstruction(parts = listOf(GeminiPart(text = system))),
            generationConfig = GeminiGenerationConfig(
                temperature = temperature,
                responseMimeType = if (jsonMode) "application/json" else null,
                maxOutputTokens = maxOutputTokens
            )
        )
        val call = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${s.geminiModel.trim()}:generateContent")
            .header("x-goog-api-key", key)
            .post(jsonBody(request))
            .build()

        http.newCall(call).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw httpError(resp.code, raw, "Gemini")
            val parsed = runCatching { appJson.decodeFromString<GeminiResponse>(raw) }
                .getOrElse { throw AssistantException("Could not parse Gemini response: ${it.message}") }
            parsed.candidates.firstOrNull()?.content?.parts
                ?.mapNotNull { it.text.takeIf { t -> t.isNotEmpty() } }
                ?.joinToString("")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: throw AssistantException("Gemini returned an empty response. Try again.")
        }
    }
}
