package com.myvoice.app.domain

/**
 * Offline keyword extraction and titling — used when no LLM key is configured,
 * or as a fallback if metadata generation fails.
 */
object KeywordTagger {

    private val STOPWORDS = setOf(
        "the", "a", "an", "and", "or", "but", "if", "then", "else", "so", "to", "of", "in", "on",
        "for", "with", "is", "am", "are", "was", "were", "be", "been", "being", "it", "its",
        "this", "that", "these", "those", "i", "me", "my", "mine", "we", "our", "us", "you",
        "your", "he", "she", "they", "them", "their", "at", "by", "from", "as", "about", "into",
        "like", "want", "wants", "need", "needs", "think", "thinking", "thought", "feel",
        "feels", "feeling", "really", "very", "just", "can", "cannot", "could", "should",
        "would", "will", "shall", "do", "does", "did", "done", "have", "has", "had", "not",
        "no", "yes", "what", "when", "where", "why", "how", "who", "whom", "get", "got",
        "make", "made", "today", "tomorrow", "yesterday", "there", "here", "also", "because",
        "some", "any", "all", "more", "most", "much", "many", "over", "under", "again"
    )

    /** Returns up to [max] lowercase keywords ordered by frequency. */
    fun keywords(text: String, max: Int = 3): List<String> =
        text.lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 && it !in STOPWORDS }
            .groupBy { it }
            .map { (word, occurrences) -> word to occurrences.size }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
            .take(max)
            .map { it.first }

    /** Builds a short title from the first words of the thought. */
    fun titleFor(text: String, maxWords: Int = 8): String {
        val clean = text.trim().replace(Regex("\\s+"), " ")
        if (clean.isEmpty()) return "Untitled thought"
        val words = clean.split(" ")
        val base = if (words.size <= maxWords) clean else words.take(maxWords).joinToString(" ") + "…"
        return base.replaceFirstChar { it.uppercaseChar }.trimEnd('.', ',', '!', '?')
    }
}
