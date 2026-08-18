package com.jakemalby.odysseusmobile.persistence

/** Typed, storage-neutral records for the encrypted local workspace. */
data class WorkspaceSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val activeConversationId: String?,
    val conversations: List<ConversationRecord>,
    val notes: List<NoteRecord>,
    val tasks: List<TaskRecord>,
    val memories: List<MemoryRecord>,
    val gallery: List<GalleryRecord>,
    val settings: WorkspaceSettingsRecord,
) {
    init {
        require(schemaVersion == CURRENT_SCHEMA_VERSION)
        require(conversations.map(ConversationRecord::id).distinct().size == conversations.size)
        require(notes.map(NoteRecord::id).distinct().size == notes.size)
        require(tasks.map(TaskRecord::id).distinct().size == tasks.size)
        require(memories.map(MemoryRecord::id).distinct().size == memories.size)
        require(gallery.map(GalleryRecord::id).distinct().size == gallery.size)
        require(activeConversationId == null || conversations.any { it.id == activeConversationId }) {
            "The active conversation must exist in the workspace"
        }
    }

    companion object { const val CURRENT_SCHEMA_VERSION = 1 }
}

data class ConversationRecord(val id: String, val title: String, val messages: List<ChatMessageRecord>)
data class ChatMessageRecord(val id: String, val author: String, val text: String, val mine: Boolean, val createdAt: Long)
data class NoteRecord(val id: String, val title: String, val body: String, val updatedAt: Long)
data class TaskRecord(val id: String, val title: String, val done: Boolean)
data class MemoryRecord(val id: String, val text: String, val createdAt: Long)
data class GalleryRecord(val id: String, val name: String, val privatePath: String, val createdAt: Long)
data class WorkspaceSettingsRecord(
    val selectedRecipe: String,
    val localOnly: Boolean,
    val compactDensity: Boolean,
)

/** Room, encrypted SQL, or another app-private store must implement this API. */
interface WorkspaceRepository {
    suspend fun read(): WorkspaceSnapshot?
    suspend fun replace(snapshot: WorkspaceSnapshot)
    suspend fun clear()
}

/** One-time import boundary; it never decrypts, reads preferences, or writes storage itself. */
interface LegacyWorkspaceMigrator {
    fun migrate(legacyPlainJson: String): LegacyMigrationResult
}

sealed interface LegacyMigrationResult {
    data class Migrated(val snapshot: WorkspaceSnapshot, val sourceVersion: Int) : LegacyMigrationResult
    data class Rejected(val reason: String) : LegacyMigrationResult
}
