package com.mobdysseus.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val status: String, // "available" | "installed" | "coming_soon"
)

class SkillsStore(private val context: Context) {
    private val file: File = File(context.filesDir, "skills.json")

    /** Returns the set of skill ids currently marked as installed. */
    fun loadInstalled(): Set<String> {
        if (!file.exists()) return emptySet()
        val text = file.readText()
        if (text.isBlank()) return emptySet()
        return try {
            val arr = JSONArray(text)
            (0 until arr.length()).map { i -> arr.getString(i) }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /** Marks a skill as installed (or uninstalled) and persists atomically. */
    fun setInstalled(id: String, installed: Boolean) {
        val current = loadInstalled().toMutableSet()
        if (installed) {
            current.add(id)
        } else {
            current.remove(id)
        }
        writeAtomically(current)
    }

    private fun writeAtomically(ids: Set<String>) {
        val arr = JSONArray()
        for (id in ids) {
            arr.put(id)
        }
        val tmp = File(context.filesDir, "skills.json.tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(file)) {
            // Fallback if rename fails (e.g. cross-device edge cases): overwrite directly.
            file.writeText(arr.toString())
            tmp.delete()
        }
    }
}
