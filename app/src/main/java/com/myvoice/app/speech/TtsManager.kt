package com.myvoice.app.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Thin wrapper around Android [TextToSpeech] for reading replies aloud.
 */
class TtsManager(context: Context) {

    @Volatile
    private var ready = false

    private val tts: TextToSpeech? = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.getDefault()
        }
    }

    fun speak(text: String) {
        if (!ready) return
        val clean = text
            .replace(Regex("[*_#`>\\[\\]]"), "")
            .replace(Regex("https?://\\S+"), "link")
            .trim()
        tts?.apply {
            stop()
            setSpeechRate(1.0f)
            speak(clean, TextToSpeech.QUEUE_FLUSH, null, "myvoice-reply")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
