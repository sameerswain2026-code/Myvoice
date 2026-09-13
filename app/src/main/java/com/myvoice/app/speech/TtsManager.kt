package com.myvoice.app.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Android TTS wrapper with a speaking state and a suspending
 * [speakAwait] used by the live conversation loop.
 */
class TtsManager(context: Context) {

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    @Volatile
    private var ready = false

    private var doneCallback: (() -> Unit)? = null

    private val tts: TextToSpeech? = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.getDefault()
        }
    }

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            @Deprecated("Deprecated in Java")
            override fun onStart(utteranceId: String?) {
                _speaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                finish()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                finish()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                finish()
            }
        })
    }

    /** Fire-and-forget speech. */
    fun speak(text: String) {
        speakInternal(text, null)
    }

    /** Speaks and suspends until playback finishes or is stopped. */
    suspend fun speakAwait(text: String): Unit = suspendCancellableCoroutine { cont ->
        speakInternal(text) { if (cont.isActive) cont.resume(Unit) }
        cont.invokeOnCancellation { stop() }
    }

    private fun speakInternal(text: String, onDone: (() -> Unit)?) {
        if (!ready) {
            onDone?.invoke()
            return
        }
        val clean = text
            .replace(Regex("[*_#`>\\[\\]]"), "")
            .replace(Regex("https?://\\S+"), "link")
            .trim()
        if (clean.isEmpty()) {
            onDone?.invoke()
            return
        }
        doneCallback = onDone
        _speaking.value = true
        tts?.apply {
            stop()
            setSpeechRate(1.0f)
            speak(clean, TextToSpeech.QUEUE_FLUSH, Bundle(), "myvoice-${System.currentTimeMillis()}")
        }
    }

    private fun finish() {
        _speaking.value = false
        doneCallback?.invoke()
        doneCallback = null
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
        finish()
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
        }
    }
}
