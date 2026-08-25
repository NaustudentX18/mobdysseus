package com.mobdysseus.app.data

import android.content.Context
import com.mobdysseus.app.provider.ChatMessage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Persists the chat transcript so conversations survive app restarts. */
class ChatStore(private val context: Context) {
    private val file: File = File(context.filesDir, "chat.json")

    fun load(): List<ChatMessage> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ChatMessage(o.optString("role"), o.optString("content"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(messages: List<ChatMessage>) {
        val arr = JSONArray()
        for (m in messages) {
            arr.put(JSONObject().put("role", m.role).put("content", m.content))
        }
        file.writeText(arr.toString())
    }

    fun clear() {
        file.delete()
    }
}
