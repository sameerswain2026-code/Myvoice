package com.myvoice.app

import com.myvoice.app.live.LiveEngine
import com.myvoice.app.live.LiveMessage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveEngineHelpersTest {

    @Test
    fun `empty conversation yields empty context`() {
        assertTrue(LiveEngine.buildLiveContext(emptyList()).isEmpty())
    }

    @Test
    fun `context labels user and assistant turns`() {
        val ctx = LiveEngine.buildLiveContext(
            listOf(
                LiveMessage(fromUser = true, text = "hello"),
                LiveMessage(fromUser = false, text = "hi there")
            )
        )
        assertTrue(ctx.contains("User: hello"))
        assertTrue(ctx.contains("Myvoice: hi there"))
    }

    @Test
    fun `context only keeps the last 12 turns`() {
        val many = (1..30).map { LiveMessage(fromUser = it % 2 == 0, text = "msg $it") }
        val ctx = LiveEngine.buildLiveContext(many)
        assertFalse(ctx.contains("msg 18"))
        assertTrue(ctx.contains("msg 30"))
    }

    @Test
    fun `live prompt keeps replies short and includes persona`() {
        val prompt = LiveEngine.liveSystemPrompt("I am a founder")
        assertTrue(prompt.contains("under ~80 words"))
        assertFalse(prompt.contains("markdown bullets"))
        assertTrue(prompt.contains("I am a founder"))
    }

    @Test
    fun `live prompt without persona omits personalization`() {
        assertFalse(LiveEngine.liveSystemPrompt("").contains("Personalization"))
    }
}
