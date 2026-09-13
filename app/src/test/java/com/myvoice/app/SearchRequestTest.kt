package com.myvoice.app

import com.myvoice.app.domain.SearchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchRequestTest {

    @Test
    fun `parses plain json with a query`() {
        val r = SearchRequest.parse("""{"search": "india inflation latest"}""")
        assertEquals("india inflation latest", r?.queryOrNull)
    }

    @Test
    fun `parses json wrapped in markdown fences`() {
        val raw = "```json\n{\"search\": \"odisha cyclone update\"}\n```"
        assertEquals("odisha cyclone update", SearchRequest.parse(raw)?.queryOrNull)
    }

    @Test
    fun `null search means no search`() {
        val r = SearchRequest.parse("""{"search": null}""")
        assertNull(r?.queryOrNull)
    }

    @Test
    fun `garbage input returns null`() {
        assertNull(SearchRequest.parse(null))
        assertNull(SearchRequest.parse(""))
        assertNull(SearchRequest.parse("no json here"))
        assertNull(SearchRequest.parse("{broken"))
    }

    @Test
    fun `unknown keys are ignored`() {
        val r = SearchRequest.parse("""{"search":"q","confidence":0.9,"extra":[1,2]}""")
        assertEquals("q", r?.queryOrNull)
    }

    @Test
    fun `query field is accepted as alias`() {
        val r = SearchRequest.parse("""{"query": "best laptops 2026"}""")
        assertEquals("best laptops 2026", r?.queryOrNull)
    }
}
