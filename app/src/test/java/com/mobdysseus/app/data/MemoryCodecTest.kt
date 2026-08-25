package com.mobdysseus.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryCodecTest {

    @Test
    fun roundTrip() {
        val items = listOf(
            Memory("a", "remember the milk", "groceries", 1000L),
            Memory("b", "meeting notes", "", 2000L),
            Memory("c", "", "tag1,tag2", 3000L),
        )
        val decoded = MemoryCodec.decode(MemoryCodec.encode(items))
        assertEquals(items, decoded)
    }

    @Test
    fun decodeMalformedReturnsEmptyList() {
        assertTrue(MemoryCodec.decode("not json at all").isEmpty())
        assertTrue(MemoryCodec.decode("{ \"id\": 1 }").isEmpty())
        assertTrue(MemoryCodec.decode("[1, 2, 3]").isEmpty())
    }

    @Test
    fun decodeEmptyArrayReturnsEmptyList() {
        assertTrue(MemoryCodec.decode("[]").isEmpty())
        assertTrue(MemoryCodec.decode("").isEmpty())
    }
}
