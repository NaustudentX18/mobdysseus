package com.mobdysseus.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EventCodecTest {

    @Test
    fun encodeDecodeRoundTripPreservesFields() {
        val events = listOf(
            Event(id = "1", title = "Standup", startEpochMs = 1000L, endEpochMs = 2000L, notes = "daily sync"),
            Event(id = "2", title = "Review", startEpochMs = 3000L, endEpochMs = 4000L, notes = ""),
        )
        val decoded = EventCodec.decode(EventCodec.encode(events))
        assertEquals(events, decoded)
    }

    @Test
    fun decodeGarbageReturnsEmptyList() {
        assertEquals(emptyList<Event>(), EventCodec.decode("garbage"))
    }

    @Test
    fun decodeEmptyJsonArrayReturnsEmptyList() {
        assertEquals(emptyList<Event>(), EventCodec.decode("[]"))
    }

    @Test
    fun encodeEmptyListDecodesBackToEmptyList() {
        assertEquals(emptyList<Event>(), EventCodec.decode(EventCodec.encode(emptyList())))
    }
}
