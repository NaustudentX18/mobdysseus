package com.jakemalby.odysseusmobile.core.memory

import com.jakemalby.odysseusmobile.core.Memory
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryFeatureSupportTest {
    private val memories = listOf(
        Memory("1", "Galaxy local model setup", 4),
        Memory("2", "Plant tomatoes", 7),
    )

    @Test fun `search requires every term and ignores case`() {
        assertEquals(listOf(memories[0]), MemoryFeatureSupport.search(memories, "LOCAL galaxy"))
        assertEquals(memories, MemoryFeatureSupport.search(memories, "  "))
    }

    @Test fun `export is valid versioned json and preserves hostile text`() {
        val source = listOf(Memory("id\\\"", "line one\nline \"two\"", 12))
        val parsed = JSONObject(MemoryFeatureSupport.exportJson(source))
        val exported = parsed.getJSONArray("memories").getJSONObject(0)

        assertEquals("mobdysseus-memory-v1", parsed.getString("format"))
        assertEquals(source[0].id, exported.getString("id"))
        assertEquals(source[0].text, exported.getString("text"))
        assertEquals(12, exported.getLong("createdAt"))
    }

    @Test fun `export filename uses stable utc date`() {
        assertEquals("mobdysseus-memories-1970-01-01.json", MemoryFeatureSupport.exportFilename(0))
        assertTrue(MemoryFeatureSupport.exportJson(emptyList()).contains("\"memories\": []"))
    }
}
