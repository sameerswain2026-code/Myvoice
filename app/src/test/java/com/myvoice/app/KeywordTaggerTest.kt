package com.myvoice.app

import com.myvoice.app.domain.KeywordTagger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeywordTaggerTest {

    @Test
    fun `keywords exclude stopwords and short tokens`() {
        val tags = KeywordTagger.keywords(
            "I think the migration plan is risky and the deadline is unrealistic"
        )
        assertTrue(tags.contains("migration") || tags.contains("plan") ||
            tags.contains("risky") || tags.contains("deadline") || tags.contains("unrealistic"))
        assertTrue(tags.none { it == "the" || it == "and" || it == "think" })
        assertTrue(tags.size <= 3)
    }

    @Test
    fun `title takes first words and adds ellipsis for long text`() {
        val title = KeywordTagger.titleFor(
            "the quick brown fox jumps over the lazy dog every single morning"
        )
        assertTrue(title.startsWith("The quick brown fox"))
        assertTrue(title.endsWith("…"))
    }

    @Test
    fun `short text becomes its own title`() {
        assertEquals("Buy milk", KeywordTagger.titleFor("Buy milk"))
    }

    @Test
    fun `empty text gets untitled`() {
        assertEquals("Untitled thought", KeywordTagger.titleFor("   "))
    }
}
