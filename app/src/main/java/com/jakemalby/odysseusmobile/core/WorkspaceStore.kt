package com.jakemalby.odysseusmobile.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Temporary v0 repository boundary. MOB-002 replaces this implementation without changing feature callers. */
class WorkspaceStore(context: Context) {
    private val prefs = context.getSharedPreferences("odysseus_workspace", Context.MODE_PRIVATE)
    fun load(): Workspace = runCatching { decode(SecureWorkspaceStorage.decrypt(prefs.getString("workspace", "") ?: "")) }.getOrElse { seedWorkspace() }
    fun save(workspace: Workspace) { prefs.edit().putString("workspace", SecureWorkspaceStorage.encrypt(encode(workspace))).apply() }
    fun reset() { prefs.edit().remove("workspace").apply() }

    /** Returns null only when there is no legacy payload; corruption remains a visible migration error. */
    fun readLegacyPlaintextOrNull(): String? {
        val encrypted = prefs.getString("workspace", null)?.takeIf(String::isNotBlank) ?: return null
        return SecureWorkspaceStorage.decrypt(encrypted)
    }

    /** Synchronous removal lets the migration coordinator know whether retirement really completed. */
    fun retireLegacy(): Unit {
        check(prefs.edit().remove("workspace").commit()) { "Could not retire the legacy workspace" }
        check(!prefs.contains("workspace")) { "Legacy workspace is still present after retirement" }
    }

    fun decode(raw: String): Workspace {
        val root = JSONObject(raw)
        val conversations = root.getJSONArray("conversations").mapTyped { conversationJson(it) }
        val notes = root.getJSONArray("notes").mapTyped { noteJson(it) }
        val tasks = root.getJSONArray("tasks").mapTyped { taskJson(it) }
        val memories = root.getJSONArray("memories").mapTyped { memoryJson(it) }
        val gallery = root.optJSONArray("gallery")?.mapTyped { galleryJson(it) }.orEmpty()
        val settings = root.getJSONObject("settings")
        return Workspace(conversations, root.getString("active"), notes, tasks, memories, gallery,
            MobileSettings(settings.optString("recipe", "Private quick chat"), settings.optBoolean("localOnly", true), settings.optBoolean("compact", false)))
    }

    fun encode(workspace: Workspace): String = JSONObject().apply {
        put("active", workspace.activeConversationId)
        put("conversations", JSONArray().apply { workspace.conversations.forEach { put(it.toJson()) } })
        put("notes", JSONArray().apply { workspace.notes.forEach { put(it.toJson()) } })
        put("tasks", JSONArray().apply { workspace.tasks.forEach { put(it.toJson()) } })
        put("memories", JSONArray().apply { workspace.memories.forEach { put(it.toJson()) } })
        put("gallery", JSONArray().apply { workspace.gallery.forEach { put(it.toJson()) } })
        put("settings", JSONObject().put("recipe", workspace.settings.selectedRecipe).put("localOnly", workspace.settings.localOnly).put("compact", workspace.settings.compactDensity))
    }.toString(2)
}

private inline fun <reified T> JSONArray.mapTyped(block: (Any) -> T): List<T> = List(length()) { block(get(it)) }
private fun conversationJson(value: Any): Conversation { val o = value as JSONObject; return Conversation(o.getString("id"), o.getString("title"), o.getJSONArray("messages").mapTyped { messageJson(it) }) }
private fun noteJson(value: Any): Note { val o = value as JSONObject; return Note(o.getString("id"), o.getString("title"), o.getString("body"), o.getLong("updated")) }
private fun taskJson(value: Any): Task { val o = value as JSONObject; return Task(o.getString("id"), o.getString("title"), o.getBoolean("done"), if (o.isNull("dueAt")) null else o.optLong("dueAt"), com.jakemalby.odysseusmobile.core.task.TaskRecurrence.valueOf(o.optString("recurrence", "NONE")), o.optLong("remindBefore", 0)) }
private fun memoryJson(value: Any): Memory { val o = value as JSONObject; return Memory(o.getString("id"), o.getString("text"), o.getLong("created")) }
private fun galleryJson(value: Any): GalleryItem { val o = value as JSONObject; return GalleryItem(o.getString("id"), o.getString("name"), o.getString("path"), o.getLong("created")) }
private fun messageJson(value: Any): Message { val o = value as JSONObject; return Message(o.getString("id"), o.getString("author"), o.getString("text"), o.getBoolean("mine"), o.getLong("created")) }
private fun Message.toJson() = JSONObject().put("id", id).put("author", author).put("text", text).put("mine", mine).put("created", createdAt)
private fun Conversation.toJson() = JSONObject().put("id", id).put("title", title).put("messages", JSONArray().apply { messages.forEach { put(it.toJson()) } })
private fun Note.toJson() = JSONObject().put("id", id).put("title", title).put("body", body).put("updated", updatedAt)
private fun Task.toJson() = JSONObject().put("id", id).put("title", title).put("done", done).put("dueAt", dueAt ?: JSONObject.NULL).put("recurrence", recurrence.name).put("remindBefore", remindBeforeMillis)
private fun Memory.toJson() = JSONObject().put("id", id).put("text", text).put("created", createdAt)
private fun GalleryItem.toJson() = JSONObject().put("id", id).put("name", name).put("path", path).put("created", createdAt)
