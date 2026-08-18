package com.jakemalby.odysseusmobile.core.memory

import com.jakemalby.odysseusmobile.core.Memory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MemoryFeatureSupport {
    fun search(memories: List<Memory>, query: String): List<Memory> {
        val terms = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        if (terms.isEmpty()) return memories
        return memories.filter { memory ->
            terms.all { term -> memory.text.contains(term, ignoreCase = true) }
        }
    }

    fun exportJson(memories: List<Memory>): String = buildString {
        append("{\n  \"format\": \"mobdysseus-memory-v1\",\n  \"memories\": [")
        if (memories.isEmpty()) {
            append(']')
        } else {
            memories.forEachIndexed { index, memory ->
                if (index > 0) append(',')
                append("\n    {\"id\": \"")
                    .append(jsonEscape(memory.id))
                    .append("\", \"text\": \"")
                    .append(jsonEscape(memory.text))
                    .append("\", \"createdAt\": ")
                    .append(memory.createdAt)
                    .append('}')
            }
            append("\n  ]")
        }
        append("\n}\n")
    }

    fun exportFilename(now: Long): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(now))
        return "mobdysseus-memories-$date.json"
    }

    private fun jsonEscape(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000c' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) {
                    append("\\u").append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
            }
        }
    }
}
