package com.jakemalby.odysseusmobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeMarkdownParserTest {
    @Test
    fun `hostile html links and images remain inert plain text`() {
        val hostile = "<script>alert('x')</script> [open](javascript:alert(1)) ![x](file:///secret)"

        val blocks = SafeMarkdownParser.parse(hostile)

        assertEquals(listOf(SafeMarkdownBlock.Plain(hostile)), blocks)
    }

    @Test
    fun `fenced code becomes a native code block with a bounded language label`() {
        val blocks = SafeMarkdownParser.parse("Before\n```kotlin<script>\nprintln(\"safe\")\n```\nAfter")

        assertEquals(SafeMarkdownBlock.Plain("Before"), blocks[0])
        assertEquals(
            SafeMarkdownBlock.Code("kotlinscript", "println(\"safe\")"),
            blocks[1],
        )
        assertEquals(SafeMarkdownBlock.Plain("After"), blocks[2])
    }

    @Test
    fun `huge fenced code is bounded and visibly truncated`() {
        val huge = "```text\n" + "x".repeat(SafeMarkdownParser.MAX_INPUT_CHARS * 2) + "\n```"

        val blocks = SafeMarkdownParser.parse(huge)
        val rendered = blocks.joinToString("") { it.text }

        assertTrue(rendered.contains("Output truncated for safe display"))
        assertTrue(rendered.length < SafeMarkdownParser.MAX_INPUT_CHARS + 100)
    }

    @Test
    fun `unclosed fence safely consumes the remaining text as code`() {
        val blocks = SafeMarkdownParser.parse("answer\n```sh\necho hello")

        assertEquals(
            listOf(
                SafeMarkdownBlock.Plain("answer"),
                SafeMarkdownBlock.Code("sh", "echo hello"),
            ),
            blocks,
        )
    }

    @Test
    fun `control and bidi override characters are neutralized but layout controls survive`() {
        val blocks = SafeMarkdownParser.parse("one\u0000two\ttab\nline\u202Etxt")
        val text = (blocks.single() as SafeMarkdownBlock.Plain).text

        assertEquals("one�two\ttab\nline�txt", text)
        assertFalse(text.contains('\u0000'))
        assertFalse(text.contains('\u202E'))
    }
}
