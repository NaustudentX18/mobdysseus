package com.jakemalby.odysseusmobile

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingTextAssemblerTest {
    @Test
    fun joinsDeltaChunks() {
        val assembler = StreamingTextAssembler()

        assertEquals("Hello", listOf("Hel", "lo").fold("") { _, chunk -> assembler.accept(chunk) })
        assertEquals("Hello world", assembler.accept(" world"))
    }

    @Test
    fun replacesCumulativeSnapshotsInsteadOfDuplicatingThem() {
        val assembler = StreamingTextAssembler()

        assembler.accept("H")
        assembler.accept("Hel")
        assertEquals("Hello", assembler.accept("Hello"))
    }

    @Test
    fun preservesRepeatedEqualDeltaChunks() {
        val assembler = StreamingTextAssembler()

        assembler.accept("ha")
        assertEquals("haha", assembler.accept("ha"))
    }

    @Test
    fun ignoresEmptyTransportChunks() {
        val assembler = StreamingTextAssembler()

        assembler.accept("partial")
        assertEquals("partial", assembler.accept(""))
        assertEquals("partial", assembler.value())
    }

    @Test
    fun fallsBackToDeltaWhenAStreamChangesShape() {
        val assembler = StreamingTextAssembler()

        assembler.accept("A")
        assembler.accept("AB")
        assertEquals("ABC", assembler.accept("C"))
        assertEquals("ABCD", assembler.accept("D"))
    }
}
