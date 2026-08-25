package com.mobdysseus.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Note(
    val id: String,
    val title: String,
    val body: String,
    val updatedAt: Long,
)

class NotesStore(private val context: Context) {
    private val file: File = File(context.filesDir, "notes.json")

    fun load(): List<Note> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Note(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    body = o.optString("body"),
                    updatedAt = o.optLong("updatedAt"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(notes: List<Note>) {
        val arr = JSONArray()
        for (n in notes) {
            arr.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("body", n.body)
                    .put("updatedAt", n.updatedAt)
            )
        }
        file.writeText(arr.toString())
    }

    fun newId(): String = UUID.randomUUID().toString()
}
