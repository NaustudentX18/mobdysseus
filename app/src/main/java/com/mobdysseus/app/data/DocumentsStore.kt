package com.mobdysseus.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Document(
    val id: String,
    val title: String,
    val body: String,
    val updatedAt: Long,
)

class DocumentsStore(private val context: Context) {
    private val file: File = File(context.filesDir, "documents.json")

    fun load(): List<Document> {
        if (!file.exists()) return emptyList()
        val text = file.readText()
        if (text.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(text)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Document(
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

    fun save(documents: List<Document>) {
        val arr = JSONArray()
        for (d in documents) {
            arr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("title", d.title)
                    .put("body", d.body)
                    .put("updatedAt", d.updatedAt)
            )
        }
        file.writeText(arr.toString())
    }

    fun newId(): String = UUID.randomUUID().toString()
}
