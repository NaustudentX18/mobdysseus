package com.jakemalby.odysseusmobile

import com.jakemalby.odysseusmobile.core.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Pure note operations kept outside Compose so search and exports are deterministic and testable. */
internal object NoteFeatureSupport {
    fun search(notes: List<Note>, query: String): List<Note> {
        val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (terms.isEmpty()) return notes
        return notes.filter { note ->
            val searchable = "${note.title}\n${note.body}"
            terms.all { searchable.contains(it, ignoreCase = true) }
        }
    }

    fun exportMarkdown(note: Note): String = buildString {
        append("# ").append(note.title.ifBlank { "Untitled note" }).append("\n\n")
        append(note.body)
        if (!note.body.endsWith('\n')) append('\n')
    }

    fun exportFilename(note: Note): String {
        val stem = note.title
            .trim()
            .replace(Regex("[^A-Za-z0-9._ -]+"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-', '.', '_')
            .take(64)
            .ifBlank { "mobdysseus-note" }
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(note.updatedAt))
        return "$stem-$date.md"
    }
}
