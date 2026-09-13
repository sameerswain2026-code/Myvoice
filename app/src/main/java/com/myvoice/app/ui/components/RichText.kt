package com.myvoice.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A tiny markdown-lite parser (pure JVM, unit-testable). Supports:
 * # / ## / ### headings, "- " bullets, "1." numbered steps, "> " quotes,
 * --- dividers, **bold**, *italic* and `code`.
 */
object RichParser {

    enum class LineType { H1, H2, H3, BULLET, NUMBERED, QUOTE, PARAGRAPH, DIVIDER }

    data class Seg(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val code: Boolean = false
    )

    data class Line(val type: LineType, val segs: List<Seg>, val number: Int = 0)

    private val numbered = Regex("^(\\d{1,3})[.)]\\s+(.*)$")

    fun parse(raw: String): List<Line> {
        val lines = mutableListOf<Line>()
        raw.replace("\r\n", "\n").split('\n').forEach { rawLine ->
            val t = rawLine.trim()
            when {
                t.isEmpty() -> Unit
                t == "---" || t == "***" || t == "___" ->
                    lines += Line(LineType.DIVIDER, emptyList())
                t.startsWith("### ") ->
                    lines += Line(LineType.H3, parseInline(t.removePrefix("### ")))
                t.startsWith("## ") ->
                    lines += Line(LineType.H2, parseInline(t.removePrefix("## ")))
                t.startsWith("# ") ->
                    lines += Line(LineType.H1, parseInline(t.removePrefix("# ")))
                t.startsWith("- ") || t.startsWith("• ") ->
                    lines += Line(LineType.BULLET, parseInline(t.substring(2)))
                t.startsWith("* ") ->
                    lines += Line(LineType.BULLET, parseInline(t.substring(2)))
                t.startsWith("> ") ->
                    lines += Line(LineType.QUOTE, parseInline(t.removePrefix("> ")))
                numbered.containsMatchIn(t) -> {
                    val m = numbered.find(t)
                    if (m != null) {
                        lines += Line(
                            LineType.NUMBERED,
                            parseInline(m.groupValues[2]),
                            number = m.groupValues[1].toIntOrNull() ?: 1
                        )
                    } else {
                        lines += Line(LineType.PARAGRAPH, parseInline(t))
                    }
                }
                else -> lines += Line(LineType.PARAGRAPH, parseInline(t))
            }
        }
        return lines
    }

    /** Parses **bold**, *italic* and `code` spans into segments. */
    fun parseInline(s: String): List<Seg> {
        val segs = mutableListOf<Seg>()
        val buf = StringBuilder()

        fun flush() {
            if (buf.isNotEmpty()) {
                segs += Seg(buf.toString())
                buf.clear()
            }
        }

        var i = 0
        while (i < s.length) {
            when {
                s.startsWith("**", i) -> {
                    val end = s.indexOf("**", i + 2)
                    if (end > i + 1) {
                        flush()
                        segs += Seg(s.substring(i + 2, end), bold = true)
                        i = end + 2
                    } else {
                        buf.append(s[i]); i++
                    }
                }
                s[i] == '`' -> {
                    val end = s.indexOf('`', i + 1)
                    if (end > i) {
                        flush()
                        segs += Seg(s.substring(i + 1, end), code = true)
                        i = end + 1
                    } else {
                        buf.append(s[i]); i++
                    }
                }
                s[i] == '*' -> {
                    val end = s.indexOf('*', i + 1)
                    val inner = if (end > i) s.substring(i + 1, end) else ""
                    if (end > i && inner.isNotEmpty() && inner.length <= 60 &&
                        !inner.startsWith(" ") && !inner.endsWith(" ")
                    ) {
                        flush()
                        segs += Seg(inner, italic = true)
                        i = end + 1
                    } else {
                        buf.append(s[i]); i++
                    }
                }
                else -> {
                    buf.append(s[i]); i++
                }
            }
        }
        flush()
        return segs
    }
}

private fun List<RichParser.Seg>.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    forEach { seg ->
        pushStyle(
            SpanStyle(
                fontWeight = if (seg.bold) FontWeight.Bold else null,
                fontStyle = if (seg.italic) FontStyle.Italic else null,
                fontFamily = if (seg.code) FontFamily.Monospace else null
            )
        )
        append(seg.text)
        pop()
    }
}

/** Renders markdown-lite text nicely inside the app. */
@Composable
fun RichText(text: String, modifier: Modifier = Modifier) {
    val lines = remember(text) { RichParser.parse(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            when (line.type) {
                RichParser.LineType.DIVIDER ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                RichParser.LineType.H1 -> Text(
                    text = line.segs.toAnnotatedString(),
                    style = MaterialTheme.typography.titleLarge
                )
                RichParser.LineType.H2 -> Text(
                    text = line.segs.toAnnotatedString(),
                    style = MaterialTheme.typography.titleMedium
                )
                RichParser.LineType.H3 -> Text(
                    text = line.segs.toAnnotatedString(),
                    style = MaterialTheme.typography.titleSmall
                )
                RichParser.LineType.BULLET -> Row {
                    Text(
                        text = "•  ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = line.segs.toAnnotatedString(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                RichParser.LineType.NUMBERED -> Row {
                    Text(
                        text = "${line.number}.  ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = line.segs.toAnnotatedString(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                RichParser.LineType.QUOTE -> Row {
                    Text(
                        text = "❝  ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = line.segs.toAnnotatedString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RichParser.LineType.PARAGRAPH -> Text(
                    text = line.segs.toAnnotatedString(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
