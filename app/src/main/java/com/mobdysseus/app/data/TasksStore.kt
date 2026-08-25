package com.mobdysseus.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Task(
    val id: String,
    val title: String,
    val done: Boolean,
    val createdAt: Long,
)

class TasksStore(private val context: Context) {
    private val file: File = File(context.filesDir, "tasks.json")

    fun load(): List<Task> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Task(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    done = o.optBoolean("done"),
                    createdAt = o.optLong("createdAt"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(tasks: List<Task>) {
        val arr = JSONArray()
        for (t in tasks) {
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("title", t.title)
                    .put("done", t.done)
                    .put("createdAt", t.createdAt)
            )
        }
        file.writeText(arr.toString())
    }

    fun newId(): String = UUID.randomUUID().toString()
}
