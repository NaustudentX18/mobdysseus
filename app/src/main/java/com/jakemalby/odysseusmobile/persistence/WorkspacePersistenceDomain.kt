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
data class NoteRecord(
    val id: String,
    val title: String,
    val body: String,
    val updatedAt: Long,
    val tags: String = "",
)
data class TaskRecord(
    val id: String,
    val title: String,
    val done: Boolean,
    val dueAt: Long? = null,
    val recurrence: String = "NONE",
    val remindBeforeMillis: Long = 0,
)
data class MemoryRecord(val id: String, val text: String, val createdAt: Long)
data class GalleryRecord(val id: String, val name: String, val privatePath: String, val createdAt: Long)
data class WorkspaceSettingsRecord(
    val selectedRecipe: String,
    val localOnly: Boolean,
    val compactDensity: Boolean,
    val theme: String = "OBSIDIAN_CORAL",
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 32,
    val maxTokens: Int = 2048,
    val systemPrompt: String = "You are Mobdysseus, a private, concise assistant running entirely on this Android phone.",
    val ragTopK: Int = 3,
    val voiceAutoSpeak: Boolean = false,
    val voiceSpeechRate: Float = 1.0f,
    val voiceSpeechPitch: Float = 1.0f,
    val biometricLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val markdownPreviewDefault: Boolean = true,
    val autoSaveDrafts: Boolean = true,
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
