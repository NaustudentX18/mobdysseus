package com.jakemalby.odysseusmobile.persistence.database

import android.content.Context
import androidx.room.withTransaction
import com.jakemalby.odysseusmobile.persistence.ChatMessageRecord
import com.jakemalby.odysseusmobile.persistence.ConversationRecord
import com.jakemalby.odysseusmobile.persistence.GalleryRecord
import com.jakemalby.odysseusmobile.persistence.MemoryRecord
import com.jakemalby.odysseusmobile.persistence.NoteRecord
import com.jakemalby.odysseusmobile.persistence.TaskRecord
import com.jakemalby.odysseusmobile.persistence.WorkspaceRepository
import com.jakemalby.odysseusmobile.persistence.WorkspaceSettingsRecord
import com.jakemalby.odysseusmobile.persistence.WorkspaceSnapshot

/** Normalized, transactional app-private Room implementation of [WorkspaceRepository]. */
class RoomWorkspaceRepository internal constructor(
    private val database: MobdysseusDatabase,
) : WorkspaceRepository {
    constructor(context: Context) : this(MobdysseusDatabase.create(context))

    override suspend fun read(): WorkspaceSnapshot? = database.withTransaction {
        val dao = database.workspaceDao()
        val workspace = dao.workspace() ?: return@withTransaction null
        val conversations = dao.conversations().map { conversation ->
            ConversationRecord(
                id = conversation.id,
                title = conversation.title,
                messages = dao.messages(conversation.id).map { message ->
                    ChatMessageRecord(message.id, message.author, message.text, message.mine, message.createdAt)
                },
            )
        }
        WorkspaceSnapshot(
            schemaVersion = workspace.schemaVersion,
            activeConversationId = workspace.activeConversationId,
            conversations = conversations,
            notes = dao.notes().map { NoteRecord(it.id, it.title, it.body, it.updatedAt, it.tags) },
            tasks = dao.tasks().map { TaskRecord(it.id, it.title, it.done, it.dueAt, it.recurrence, it.remindBeforeMillis) },
            memories = dao.memories().map { MemoryRecord(it.id, it.text, it.createdAt) },
            gallery = dao.gallery().map { GalleryRecord(it.id, it.name, it.privatePath, it.createdAt) },
            settings = WorkspaceSettingsRecord(
                selectedRecipe = workspace.selectedRecipe,
                localOnly = workspace.localOnly,
                compactDensity = workspace.compactDensity,
                theme = workspace.theme,
                temperature = workspace.temperature,
                topP = workspace.topP,
                topK = workspace.topK,
                maxTokens = workspace.maxTokens,
                systemPrompt = workspace.systemPrompt,
                ragTopK = workspace.ragTopK,
                voiceAutoSpeak = workspace.voiceAutoSpeak,
                voiceSpeechRate = workspace.voiceSpeechRate,
                voiceSpeechPitch = workspace.voiceSpeechPitch,
                biometricLockEnabled = workspace.biometricLockEnabled,
                notificationsEnabled = workspace.notificationsEnabled,
                markdownPreviewDefault = workspace.markdownPreviewDefault,
                autoSaveDrafts = workspace.autoSaveDrafts,
            ),
        )
    }

    override suspend fun replace(snapshot: WorkspaceSnapshot) = database.withTransaction {
        val dao = database.workspaceDao()
        // Delete the root first; FK cascades clear all normalized child rows.
        dao.deleteWorkspace()
        dao.insertWorkspace(WorkspaceEntity(
            schemaVersion = snapshot.schemaVersion,
            activeConversationId = snapshot.activeConversationId,
            selectedRecipe = snapshot.settings.selectedRecipe,
            localOnly = snapshot.settings.localOnly,
            compactDensity = snapshot.settings.compactDensity,
            theme = snapshot.settings.theme,
            temperature = snapshot.settings.temperature,
            topP = snapshot.settings.topP,
            topK = snapshot.settings.topK,
            maxTokens = snapshot.settings.maxTokens,
            systemPrompt = snapshot.settings.systemPrompt,
            ragTopK = snapshot.settings.ragTopK,
            voiceAutoSpeak = snapshot.settings.voiceAutoSpeak,
            voiceSpeechRate = snapshot.settings.voiceSpeechRate,
            voiceSpeechPitch = snapshot.settings.voiceSpeechPitch,
            biometricLockEnabled = snapshot.settings.biometricLockEnabled,
            notificationsEnabled = snapshot.settings.notificationsEnabled,
            markdownPreviewDefault = snapshot.settings.markdownPreviewDefault,
            autoSaveDrafts = snapshot.settings.autoSaveDrafts,
        ))
        dao.insertConversations(snapshot.conversations.mapIndexed { order, value ->
            ConversationEntity(value.id, title = value.title, sortOrder = order)
        })
        dao.insertMessages(snapshot.conversations.flatMap { conversation ->
            conversation.messages.mapIndexed { order, value ->
                ChatMessageEntity(value.id, conversation.id, value.author, value.text, value.mine, value.createdAt, order)
            }
        })
        dao.insertNotes(snapshot.notes.mapIndexed { order, value -> NoteEntity(value.id, title = value.title, body = value.body, updatedAt = value.updatedAt, tags = value.tags, sortOrder = order) })
        dao.insertTasks(snapshot.tasks.mapIndexed { order, value -> TaskEntity(value.id, title = value.title, done = value.done, dueAt = value.dueAt, recurrence = value.recurrence, remindBeforeMillis = value.remindBeforeMillis, sortOrder = order) })
        dao.insertMemories(snapshot.memories.mapIndexed { order, value -> MemoryEntity(value.id, text = value.text, createdAt = value.createdAt, sortOrder = order) })
        dao.insertGallery(snapshot.gallery.mapIndexed { order, value -> GalleryEntity(value.id, name = value.name, privatePath = value.privatePath, createdAt = value.createdAt, sortOrder = order) })
    }

    override suspend fun clear() = database.withTransaction { database.workspaceDao().deleteWorkspace() }
}
