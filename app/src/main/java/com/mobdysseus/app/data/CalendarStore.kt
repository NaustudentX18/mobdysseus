package com.mobdysseus.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Event(
    val id: String,
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val notes: String,
)

/**
 * Pure JSON codec for [Event] lists. No android.* imports, so it is unit-testable on the JVM.
 */
object EventCodec {
    fun encode(events: List<Event>): String {
        val arr = JSONArray()
        for (e in events) {
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("title", e.title)
                    .put("startEpochMs", e.startEpochMs)
                    .put("endEpochMs", e.endEpochMs)
                    .put("notes", e.notes)
            )
        }
        return arr.toString()
    }

    fun decode(json: String): List<Event> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Event(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    startEpochMs = o.optLong("startEpochMs"),
                    endEpochMs = o.optLong("endEpochMs"),
                    notes = o.optString("notes"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

class CalendarStore(private val context: Context) {
    private val file: File = File(context.filesDir, "events.json")

    fun load(): List<Event> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        return EventCodec.decode(text)
    }

    fun save(events: List<Event>) {
        file.writeText(EventCodec.encode(events))
    }

    fun newId(): String = UUID.randomUUID().toString()
}
