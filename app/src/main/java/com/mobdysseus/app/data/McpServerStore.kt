package com.mobdysseus.app.data

import android.content.Context
import com.mobdysseus.app.mcp.McpServerConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class McpServerStore(private val context: Context) {
    private val file: File = File(context.filesDir, "mcp_servers.json")

    fun load(): List<McpServerConfig> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                McpServerConfig(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    url = o.optString("url"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(servers: List<McpServerConfig>) {
        val arr = JSONArray()
        for (s in servers) {
            arr.put(
                JSONObject()
                    .put("id", s.id)
                    .put("name", s.name)
                    .put("url", s.url)
            )
        }
        file.writeText(arr.toString())
    }

    fun newId(): String = UUID.randomUUID().toString()
}
