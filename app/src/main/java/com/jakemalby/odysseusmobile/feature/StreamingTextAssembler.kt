package com.jakemalby.odysseusmobile

/**
 * Normalises model streams that emit either token deltas ("Hel", "lo") or
 * cumulative snapshots ("Hel", "Hello") into one displayable response.
 *
 * LiteRT-LM implementations may vary in how partial text is surfaced, so the
 * mode is inferred only once a later chunk unambiguously extends the current
 * response. Repeated equal chunks are treated as deltas because they can be
 * legitimate repeated tokens.
 */
internal class StreamingTextAssembler {
    private enum class Mode { UNKNOWN, DELTA, CUMULATIVE }

    private var mode = Mode.UNKNOWN
    private var text = ""

    fun accept(chunk: String): String {
        if (chunk.isEmpty()) return text

        text = when (mode) {
            Mode.DELTA -> text + chunk
            Mode.CUMULATIVE -> {
                if (chunk.startsWith(text)) chunk
                else {
                    mode = Mode.DELTA
                    text + chunk
                }
            }
            Mode.UNKNOWN -> when {
                text.isEmpty() -> chunk
                chunk.length > text.length && chunk.startsWith(text) -> {
                    mode = Mode.CUMULATIVE
                    chunk
                }
                else -> {
                    mode = Mode.DELTA
                    text + chunk
                }
            }
        }
        return text
    }

    fun value(): String = text
}
