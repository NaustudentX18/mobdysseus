package com.mobdysseus.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Memory(
    val id: String,
    val content: String,
    val tags: String,
    val createdAt: Long,
)

object MemoryCodec {
    fun encode(items: List<Memory>): String {
        val arr = JSONArray()
        for (m in items) {
            arr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("content", m.content)
                    .put("tags", m.tags)
                    .put("createdAt", m.createdAt)
            )
        }
        return arr.toString()
    }

    fun decode(json: String): List<Memory> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Memory(
                    id = o.optString("id"),
                    content = o.optString("content"),
                    tags = o.optString("tags"),
                    createdAt = o.optLong("createdAt"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

class MemoryStore(private val context: Context) {
    private val file: File = File(context.filesDir, "memory.json")

    fun load(): List<Memory> {
        if (!file.exists()) return emptyList()
        return MemoryCodec.decode(file.readText())
    }

    fun save(items: List<Memory>) {
        file.writeText(MemoryCodec.encode(items))
    }

    fun newId(): String = UUID.randomUUID().toString()
}
