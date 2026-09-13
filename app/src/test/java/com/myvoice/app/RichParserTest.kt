package com.myvoice.app

import com.myvoice.app.ui.components.RichParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichParserTest {

    @Test
    fun `headings map to correct levels`() {
        val lines = RichParser.parse("# Title\n## Section\n### Sub")
        assertEquals(RichParser.LineType.H1, lines[0].type)
        assertEquals(RichParser.LineType.H2, lines[1].type)
        assertEquals(RichParser.LineType.H3, lines[2].type)
    }

    @Test
    fun `bullets and numbered steps are detected`() {
        val lines = RichParser.parse("- first\n2. second\n* third")
        assertEquals(RichParser.LineType.BULLET, lines[0].type)
        assertEquals(2, lines[1].number)
        assertEquals(RichParser.LineType.NUMBERED, lines[1].type)
        assertEquals(RichParser.LineType.BULLET, lines[2].type)
    }

    @Test
    fun `bold segments are parsed`() {
        val segs = RichParser.parseInline("plain **bold** plain")
        assertEquals(3, segs.size)
        assertTrue(segs[1].bold)
        assertEquals("bold", segs[1].text)
    }

    @Test
    fun `code segments are parsed`() {
        val segs = RichParser.parseInline("run `npm start` now")
        assertTrue(segs.any { it.code && it.text == "npm start" })
    }

    @Test
    fun `unterminated bold is kept as literal text`() {
        val segs = RichParser.parseInline("2 ** 3 = 6")
        assertEquals(1, segs.size)
        assertEquals("2 ** 3 = 6", segs[0].text)
    }

    @Test
    fun `dividers and blank lines are handled`() {
        val lines = RichParser.parse("a\n\n---\nb")
        assertEquals(RichParser.LineType.PARAGRAPH, lines[0].type)
        assertEquals(RichParser.LineType.DIVIDER, lines[1].type)
        assertEquals(RichParser.LineType.PARAGRAPH, lines[2].type)
    }

    @Test
    fun `quotes are detected`() {
        val lines = RichParser.parse("> wise words")
        assertEquals(RichParser.LineType.QUOTE, lines[0].type)
    }
}
