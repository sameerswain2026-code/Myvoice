package com.myvoice.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.myvoice.app.core.AssistantException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Zero-config, offline-capable speech recognition using the platform
 * [SpeechRecognizer] (Google speech services on most devices).
 */
class DeviceSpeechRecognizer(private val context: Context) {

    /**
     * Starts listening and suspends until a final result is available.
     * [onPartial] is called with partial transcriptions while listening.
     * Returns "" when nothing was recognized (silence / no match).
     */
    suspend fun listen(languageTag: String, onPartial: (String) -> Unit): String =
        withContext(Dispatchers.Main) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                throw AssistantException(
                    "On-device speech recognition is not available on this phone. " +
                        "Use Deepgram or Sarvam in Settings → Speech-to-text."
                )
            }
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            try {
                withTimeoutOrNull(LISTEN_TIMEOUT_MS) {
                    suspendCancellableCoroutine { cont ->
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        }
                        recognizer.setRecognitionListener(object : RecognitionListener {
                            override fun onReadyForSpeech(params: Bundle?) {}
                            override fun onBeginningOfSpeech() {}
                            override fun onRmsChanged(rmsdB: Float) {}
                            override fun onBufferReceived(buffer: ByteArray?) {}
                            override fun onEndOfSpeech() {}

                            override fun onError(error: Int) {
                                if (!cont.isActive) return
                                when (error) {
                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                        cont.resumeWithException(
                                            AssistantException("Microphone permission is required.")
                                        )
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                                    SpeechRecognizer.ERROR_NO_MATCH -> cont.resume("")
                                    else -> cont.resumeWithException(
                                        AssistantException(
                                            "Speech recognition failed (code $error). " +
                                                "Try again, or switch to Deepgram/Sarvam in Settings."
                                        )
                                    )
                                }
                            }

                            override fun onResults(results: Bundle?) {
                                val text = results
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull().orEmpty()
                                if (cont.isActive) cont.resume(text.trim())
                            }

                            override fun onPartialResults(partialResults: Bundle?) {
                                partialResults
                                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    ?.firstOrNull()
                                    ?.let(onPartial)
                            }

                            override fun onEvent(eventType: Int, params: Bundle?) {}
                        })
                        recognizer.startListening(intent)
                        cont.invokeOnCancellation {
                            try {
                                recognizer.stopListening()
                            } catch (_: Exception) {
                            }
                        }
                    }
                } ?: "" // timed out
            } finally {
                try {
                    recognizer.destroy()
                } catch (_: Exception) {
                }
            }
        }

    private companion object {
        const val LISTEN_TIMEOUT_MS = 120_000L
    }
}
