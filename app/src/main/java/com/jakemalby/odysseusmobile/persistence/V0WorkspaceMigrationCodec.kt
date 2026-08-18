package com.jakemalby.odysseusmobile.persistence

import org.json.JSONArray
import org.json.JSONObject

/**
 * Converts the exact early encrypted-JSON payload shape into the typed v1
 * domain. The caller decrypts the string with its existing Keystore mechanism,
 * invokes this codec, persists atomically through [WorkspaceRepository], then
 * may remove the old preference only after that transaction succeeds.
 */
object V0WorkspaceMigrationCodec : LegacyWorkspaceMigrator {
    const val SOURCE_VERSION = 0

    override fun migrate(legacyPlainJson: String): LegacyMigrationResult = runCatching {
        val root = JSONObject(legacyPlainJson)
        requireNoFutureSchema(root)
        val conversations = root.requiredArray("conversations").mapObjects(::conversation)
        val active = root.requiredString("active")
        WorkspaceSnapshot(
            activeConversationId = active,
            conversations = conversations,
            notes = root.requiredArray("notes").mapObjects(::note),
            tasks = root.requiredArray("tasks").mapObjects(::task),
            memories = root.requiredArray("memories").mapObjects(::memory),
            gallery = root.optionalArray("gallery").mapObjects(::gallery),
            settings = settings(root.requiredObject("settings")),
        )
    }.fold(
        onSuccess = { LegacyMigrationResult.Migrated(it, SOURCE_VERSION) },
        onFailure = { LegacyMigrationResult.Rejected(it.message ?: "Invalid v0 workspace payload") },
    )

    private fun requireNoFutureSchema(root: JSONObject) {
        if (root.has("schemaVersion")) {
            require(root.getInt("schemaVersion") == SOURCE_VERSION) { "Unsupported workspace schema version" }
        }
    }

    private fun conversation(value: JSONObject) = ConversationRecord(
        id = value.requiredString("id"),
        title = value.requiredString("title"),
        messages = value.requiredArray("messages").mapObjects(::message),
    )

    private fun message(value: JSONObject) = ChatMessageRecord(
        id = value.requiredString("id"), author = value.requiredString("author"),
        text = value.requiredString("text"), mine = value.requiredBoolean("mine"),
        createdAt = value.requiredLong("created"),
    )

    private fun note(value: JSONObject) = NoteRecord(
        id = value.requiredString("id"), title = value.requiredString("title"),
        body = value.requiredString("body"), updatedAt = value.requiredLong("updated"),
    )

    private fun task(value: JSONObject) = TaskRecord(
        id = value.requiredString("id"), title = value.requiredString("title"), done = value.requiredBoolean("done"),
    )

    private fun memory(value: JSONObject) = MemoryRecord(
        id = value.requiredString("id"), text = value.requiredString("text"), createdAt = value.requiredLong("created"),
    )

    private fun gallery(value: JSONObject) = GalleryRecord(
        id = value.requiredString("id"), name = value.requiredString("name"),
        privatePath = value.requiredString("path"), createdAt = value.requiredLong("created"),
    )

    private fun settings(value: JSONObject) = WorkspaceSettingsRecord(
        selectedRecipe = value.requiredString("recipe"),
        localOnly = value.requiredBoolean("localOnly"),
        compactDensity = value.requiredBoolean("compact"),
    )

    private fun JSONObject.requiredString(name: String): String = getString(name).also {
        require(it.isNotBlank()) { "$name must not be blank" }
    }
    private fun JSONObject.requiredBoolean(name: String): Boolean = getBoolean(name)
    private fun JSONObject.requiredLong(name: String): Long = getLong(name)
    private fun JSONObject.requiredObject(name: String): JSONObject = getJSONObject(name)
    private fun JSONObject.requiredArray(name: String): JSONArray = getJSONArray(name)
    private fun JSONObject.optionalArray(name: String): JSONArray = optJSONArray(name) ?: JSONArray()
    private fun <T> JSONArray.mapObjects(map: (JSONObject) -> T): List<T> = List(length()) { index -> map(getJSONObject(index)) }
}
