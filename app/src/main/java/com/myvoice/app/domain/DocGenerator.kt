package com.myvoice.app.domain

import com.myvoice.app.data.remote.GeminiClient

/**
 * Document types Myvoice can generate, each with its own writing instruction.
 */
enum class DocKind(
    val id: String,
    val label: String,
    val emoji: String,
    val instruction: String
) {
    NOTES("notes", "Notes", "📝", "clean, well-organized notes with clear headings and bullet points"),
    EMAIL("email", "Email", "✉️", "a professional, ready-to-send email with a subject line, greeting, body and sign-off"),
    REPORT("report", "Report", "📊", "a structured report with an executive summary, key findings, analysis and recommendations"),
    BLOG("blog", "Blog post", "✍️", "an engaging blog post with a catchy title, introduction, sections and a conclusion"),
    PLAN("plan", "Action plan", "🎯", "an actionable plan with goals, step-by-step actions, a timeline and success metrics"),
    SUMMARY("summary", "Summary", "🧾", "a crisp executive summary using short paragraphs and bullets"),
    CUSTOM("custom", "Custom", "📄", "a clean, well-structured document following the extra instructions provided");

    companion object {
        fun byId(id: String?): DocKind = entries.firstOrNull { it.id == id } ?: NOTES
    }
}

/**
 * Generates polished Markdown documents from raw thoughts or conversations.
 */
class DocGenerator(private val gemini: GeminiClient) {

    suspend fun generate(
        kind: DocKind,
        sourceText: String,
        extra: String = "",
        persona: String = ""
    ): String {
        val system = buildString {
            appendLine("You are an expert document writer inside a personal voice-thinking app.")
            appendLine("Turn the user's raw thought or conversation into ${kind.instruction}.")
            appendLine("Formatting rules: output Markdown only — use '#', '##' and '###' headings, '- ' bullets, numbered steps for procedures, and **bold** for key points.")
            appendLine("Write in the same language as the source (English, Hindi, Hinglish, Odia…).")
            appendLine("Output ONLY the document itself — no preamble, no explanations, no code fences.")
            if (persona.isNotBlank()) {
                appendLine()
                appendLine("Author context: ${persona.trim()}")
            }
        }
        val user = buildString {
            appendLine("SOURCE MATERIAL:")
            appendLine(sourceText.take(8000))
            if (extra.isNotBlank()) {
                appendLine()
                appendLine("EXTRA INSTRUCTIONS: ${extra.take(500)}")
            }
        }
        return gemini.generate(
            system = system,
            user = user,
            temperature = 0.6,
            maxOutputTokens = 4096
        ).removeSurrounding("```", "```").trim()
    }
}
