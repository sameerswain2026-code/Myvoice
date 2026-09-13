package com.myvoice.app.data.remote

import com.myvoice.app.core.AssistantException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class SarvamClient(private val http: OkHttpClient) {

    /**
     * Transcribes a WAV file with Sarvam AI — best choice for Indic languages
     * (Hindi, Odia, Bengali, Tamil, Telugu, …) plus English.
     */
    suspend fun transcribe(
        wav: ByteArray,
        apiKey: String,
        model: String = "saaras:v3",
        mode: String = "transcribe"
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw AssistantException("No Sarvam API key set — add one in Settings → Speech-to-text.")
        }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "audio.wav", wav.toRequestBody("audio/wav".toMediaType()))
            .addFormDataPart("model", model.trim())
            .apply {
                // "mode" is only supported by the saaras family
                if (mode.isNotBlank() && model.trim().startsWith("saaras")) {
                    addFormDataPart("mode", mode.trim())
                }
            }
            .build()

        val call = Request.Builder()
            .url("https://api.sarvam.ai/speech-to-text")
            .header("api-subscription-key", apiKey.trim())
            .post(body)
            .build()

        http.newCall(call).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw httpError(resp.code, raw, "Sarvam")
            val parsed = runCatching { appJson.decodeFromString<SarvamResponse>(raw) }
                .getOrElse { throw AssistantException("Could not parse Sarvam response: ${it.message}") }
            parsed.transcript.orEmpty().trim()
        }
    }
}
