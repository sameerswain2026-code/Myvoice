package com.myvoice.app.domain

import com.myvoice.app.data.remote.GeminiClient

/**
 * Cleans up raw speech-to-text transcripts: punctuation, spelling, grammar,
 * filler words and obviously-misheard words — without changing language or
 * meaning.
 */
class TranscriptPolisher(private val gemini: GeminiClient) {

    suspend fun polish(text: String): String {
        if (text.trim().length < MIN_LENGTH) return text
        val system = """
            You clean up speech-to-text transcripts.

            Return ONLY the corrected transcript text:
            - Fix punctuation, spelling and grammar.
            - Remove filler words (um, hmm, matlab, like, you know) and repeated stumbles.
            - Fix words that were clearly misheard, using context.
            - Keep the SAME language (never translate) and the same meaning.
            - Do NOT answer, comment, summarize or add anything.
        """.trimIndent()
        val cleaned = gemini.generate(
            system = system,
            user = text.take(6000),
            temperature = 0.1,
            maxOutputTokens = 2048
        ).trim()
        return cleaned.ifBlank { text }
    }

    private companion object {
        const val MIN_LENGTH = 8
    }
}
