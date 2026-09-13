package com.myvoice.app.domain

import com.myvoice.app.data.remote.GeminiClient
import com.myvoice.app.data.remote.TavilyClient
import com.myvoice.app.data.remote.TavilyResponse
import com.myvoice.app.data.remote.appJson
import com.myvoice.app.data.prefs.SettingsRepository
import kotlinx.serialization.Serializable

/**
 * Orchestrates the "thinking" loop:
 *  1. (optional) decide with the LLM whether a web search helps → Tavily lookup
 *  2. generate the reflective reply, grounded in any search results
 */
class ThinkingEngine(
    private val gemini: GeminiClient,
    private val tavily: TavilyClient,
    private val settings: SettingsRepository
) {

    sealed interface EngineEvent {
        data class Stage(val label: String) : EngineEvent
        data class Searching(val query: String) : EngineEvent
        data class SearchDone(val count: Int) : EngineEvent
    }

    data class EngineResult(val reply: String, val usedWeb: Boolean)

    suspend fun think(
        input: String,
        recentContext: String,
        onEvent: (EngineEvent) -> Unit = {}
    ): EngineResult {
        val s = settings.current()
        var usedWeb = false
        var searchBlock = ""

        if (s.webSearchEnabled && s.tavilyKey.isNotBlank() && s.geminiKey.isNotBlank()) {
            onEvent(EngineEvent.Stage("Deciding if the web can help"))
            val decision = runCatching {
                gemini.generate(
                    system = SEARCH_DECISION_SYSTEM,
                    user = "User thought: ${input.take(500)}",
                    temperature = 0.0,
                    jsonMode = true,
                    maxOutputTokens = 64
                )
            }.getOrNull()
            val query = SearchRequest.parse(decision)?.queryOrNull
            if (query != null) {
                onEvent(EngineEvent.Searching(query))
                val results = runCatching { tavily.search(query, s.tavilyKey.trim()) }.getOrNull()
                if (results != null && results.results.isNotEmpty()) {
                    usedWeb = true
                    searchBlock = formatResults(query, results)
                    onEvent(EngineEvent.SearchDone(results.results.size))
                }
            }
        }

        onEvent(EngineEvent.Stage("Thinking"))
        val system = buildString {
            append(BASE_PROMPT)
            if (s.persona.isNotBlank()) {
                append("\n\nAdditional personalization from the user:\n")
                append(s.persona.trim())
            }
        }
        val user = buildString {
            if (recentContext.isNotBlank()) {
                appendLine("Earlier thoughts (for continuity only — do not repeat them):")
                appendLine(recentContext)
                appendLine()
            }
            if (searchBlock.isNotBlank()) {
                appendLine(searchBlock)
                appendLine()
            }
            append("What I am thinking right now:\n")
            append(input)
        }
        val reply = gemini.generate(system = system, user = user, temperature = 0.8)
        return EngineResult(reply = reply, usedWeb = usedWeb)
    }

    private fun formatResults(query: String, results: TavilyResponse): String = buildString {
        appendLine("WEB SEARCH RESULTS for \"$query\":")
        results.answer?.takeIf { it.isNotBlank() }?.let {
            appendLine("Overview: $it")
            appendLine()
        }
        results.results.take(5).forEachIndexed { i, r ->
            appendLine("[${i + 1}] ${r.title} — ${r.url}")
            appendLine(r.content.take(350))
            appendLine()
        }
    }

    companion object {
        val BASE_PROMPT = """
            You are Myvoice, a personal thinking assistant and thought partner.

            The user will share a raw thought — spoken or typed. Your job is to help them think,
            not to talk at them.

            Style rules:
            - Be warm, direct and concise. No flattery, no filler, never start with "great question".
            - Structure every reply in short sections: a 1-2 sentence reflection under "What I hear",
              then the sharpest insights as "- " bullet points, then (only when the thought is
              actionable) a "Next smallest step" suggestion, and always finish with
              "Questions to sit with" containing 2-3 open questions.
            - Use markdown-lite formatting: short paragraphs, "- " bullets, **bold** sparingly.
            - If WEB SEARCH RESULTS are provided in the prompt, ground your claims in them and cite
              them inline like [1], [2] using their numbers.
            - Never invent facts or sources. If something is uncertain, say so plainly.
            - Match the user's language: reply in the language the user wrote in.
        """.trimIndent()

        private val SEARCH_DECISION_SYSTEM = """
            You decide whether a web search would genuinely improve the answer for the user's
            thought. Searching helps for: current events, prices, news, sports, research,
            factual lookups. It does not help for: emotions, personal reflection, opinions,
            planning, brainstorming.

            Answer with STRICT JSON only, no markdown fences, exactly one of:
            {"search": "the search query to run"}
            {"search": null}
        """.trimIndent()
    }
}

/**
 * Generates a title + tags for a saved thought (LLM when available,
 * offline keyword extraction otherwise).
 */
class ThoughtMetadataGenerator(private val gemini: GeminiClient) {

    data class Metadata(val title: String, val tags: List<String>)

    suspend fun generate(transcript: String, reply: String): Metadata {
        return try {
            val system = """
                You label journal thoughts. Return STRICT JSON only, no markdown fences:
                {"title": "max 8 word title", "tags": ["max 3 short lowercase tags"]}
            """.trimIndent()
            val user = "Thought: ${transcript.take(600)}\n\nAssistant reply: ${reply.take(600)}"
            val raw = gemini.generate(
                system = system,
                user = user,
                temperature = 0.2,
                jsonMode = true,
                maxOutputTokens = 128
            )
            parseMetadata(raw) ?: fallback(transcript)
        } catch (_: Exception) {
            fallback(transcript)
        }
    }

    private fun parseMetadata(raw: String): Metadata? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val parsed = runCatching {
            appJson.decodeFromString<MetadataDto>(raw.substring(start, end + 1))
        }.getOrNull() ?: return null
        val title = parsed.title?.trim()?.takeIf { it.isNotEmpty() }
        val tags = parsed.tags
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?.take(3)
            .orEmpty()
        return if (title != null || tags.isNotEmpty()) Metadata(
            title = title ?: "",
            tags = tags
        ) else null
    }

    private fun fallback(transcript: String) = Metadata(
        title = KeywordTagger.titleFor(transcript),
        tags = KeywordTagger.keywords(transcript)
    )

    @Serializable
    private data class MetadataDto(val title: String? = null, val tags: List<String>? = null)
}
