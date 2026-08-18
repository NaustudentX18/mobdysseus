package com.jakemalby.odysseusmobile

/**
 * A deliberately small, inert Markdown subset for model output.
 *
 * Only fenced code blocks are recognised. Everything else remains plain text, so HTML, links,
 * images and other active Markdown constructs can never become executable or clickable content.
 */
internal sealed interface SafeMarkdownBlock {
    val text: String

    data class Plain(override val text: String) : SafeMarkdownBlock
    data class Code(val language: String?, override val text: String) : SafeMarkdownBlock
}

internal object SafeMarkdownParser {
    internal const val MAX_INPUT_CHARS = 65_536
    internal const val MAX_BLOCKS = 128
    private const val TRUNCATION_NOTICE = "\n\n… Output truncated for safe display."

    fun parse(source: String): List<SafeMarkdownBlock> {
        if (source.isEmpty()) return emptyList()

        val bounded = if (source.length > MAX_INPUT_CHARS) {
            source.take(MAX_INPUT_CHARS) + TRUNCATION_NOTICE
        } else {
            source
        }
        val clean = sanitizeControls(bounded).replace("\r\n", "\n").replace('\r', '\n')
        val blocks = mutableListOf<SafeMarkdownBlock>()
        val current = StringBuilder()
        val overflow = StringBuilder()
        var inCode = false
        var language: String? = null

        fun emit(block: SafeMarkdownBlock) {
            if (block.text.isEmpty()) return
            if (blocks.size < MAX_BLOCKS) {
                blocks += block
            } else {
                if (overflow.isNotEmpty()) overflow.append('\n')
                overflow.append(block.text)
            }
        }

        fun flush() {
            val text = current.toString().trimEnd('\n')
            if (inCode) emit(SafeMarkdownBlock.Code(language, text))
            else emit(SafeMarkdownBlock.Plain(text))
            current.clear()
        }

        clean.split('\n').forEach { line ->
            val fence = fenceInfo(line)
            when {
                !inCode && fence != null -> {
                    flush()
                    inCode = true
                    language = sanitizeLanguage(fence)
                }
                inCode && fence != null && fence.isBlank() -> {
                    flush()
                    inCode = false
                    language = null
                }
                else -> current.append(line).append('\n')
            }
        }
        flush()

        if (overflow.isNotEmpty()) {
            blocks += SafeMarkdownBlock.Plain(
                "… Additional formatting flattened for safe display.\n${overflow}",
            )
        }
        return blocks
    }

    private fun fenceInfo(line: String): String? {
        val candidate = line.dropWhile { it == ' ' }.takeIf { line.length - it.length <= 3 }
            ?: return null
        if (!candidate.startsWith("```")) return null
        return candidate.drop(3).trim()
    }

    private fun sanitizeLanguage(value: String): String? = value
        .take(32)
        .filter { it.isLetterOrDigit() || it in charArrayOf('+', '-', '_', '.', '#') }
        .takeIf { it.isNotBlank() }

    private fun sanitizeControls(value: String): String = buildString(value.length) {
        value.forEach { character ->
            val unsafeBidi = character in '\u202A'..'\u202E' || character in '\u2066'..'\u2069'
            when {
                character == '\n' || character == '\r' || character == '\t' -> append(character)
                character.isISOControl() || unsafeBidi -> append('\uFFFD')
                else -> append(character)
            }
        }
    }
}
