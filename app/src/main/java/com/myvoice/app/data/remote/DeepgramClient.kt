package com.myvoice.app.data.remote

import com.myvoice.app.core.AssistantException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class DeepgramClient(private val http: OkHttpClient) {

    /**
     * Transcribes a WAV file using Deepgram's pre-recorded API.
     */
    suspend fun transcribe(
        wav: ByteArray,
        apiKey: String,
        model: String = "nova-2",
        language: String = "en"
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw AssistantException("No Deepgram API key set — add one in Settings → Speech-to-text.")
        }
        val url = "https://api.deepgram.com/v1/listen" +
            "?model=${model.trim()}&smart_format=true&punctuate=true&language=${language.trim()}"
        val call = Request.Builder()
            .url(url)
            .header("Authorization", "Token ${apiKey.trim()}")
            .header("Content-Type", "audio/wav")
            .post(wav.toRequestBody("audio/wav".toMediaType()))
            .build()

        http.newCall(call).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw httpError(resp.code, raw, "Deepgram")
            val parsed = runCatching { appJson.decodeFromString<DeepgramResponse>(raw) }
                .getOrElse { throw AssistantException("Could not parse Deepgram response: ${it.message}") }
            parsed.results?.channels?.firstOrNull()?.alternatives?.firstOrNull()?.transcript
                .orEmpty()
                .trim()
        }
    }
}
