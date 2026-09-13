package com.myvoice.app

import com.myvoice.app.data.db.Thought
import com.myvoice.app.ui.home.HomeViewModel
import com.myvoice.app.ui.settings.toMarkdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineContextTest {

    private fun thought(
        title: String,
        transcript: String,
        reply: String = "",
        createdAt: Long = 0
    ) = Thought(
        id = title,
        title = title,
        transcript = transcript,
        reply = reply,
        tags = emptyList(),
        createdAt = createdAt,
        updatedAt = createdAt
    )

    @Test
    fun `empty history produces empty context`() {
        assertEquals("", HomeViewModel.buildRecentContext(emptyList()))
    }

    @Test
    fun `context contains titles and truncated text`() {
        val longTranscript = "x".repeat(500)
        val context = HomeViewModel.buildRecentContext(
            listOf(thought("Career", longTranscript, "short reply"))
        )
        assertTrue(context.contains("Career"))
        assertTrue(context.length < 500)
    }

    @Test
    fun `markdown export has header and sections`() {
        val md = listOf(
            thought("Buy milk", "remember to buy milk", "Set a reminder at 6pm.", createdAt = 1_700_000_000_000)
        ).toMarkdown()
        assertTrue(md.startsWith("# Myvoice export"))
        assertTrue(md.contains("## Buy milk"))
        assertTrue(md.contains("**Me:** remember to buy milk"))
        assertTrue(md.contains("Set a reminder at 6pm."))
    }

    @Test
    fun `markdown export of nothing says so`() {
        assertTrue(emptyList<Thought>().toMarkdown().contains("No thoughts yet"))
    }
}
