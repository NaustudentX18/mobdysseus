package com.jakemalby.odysseusmobile.persistence.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workspace")
internal data class WorkspaceEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val schemaVersion: Int,
    val activeConversationId: String?,
    val selectedRecipe: String,
    val localOnly: Boolean,
    val compactDensity: Boolean,
) {
    companion object { const val SINGLETON_ID = 1 }
}

@Entity(
    tableName = "conversation",
    foreignKeys = [ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("workspaceId")],
)
internal data class ConversationEntity(
    @PrimaryKey val id: String,
    val workspaceId: Int = WorkspaceEntity.SINGLETON_ID,
    val title: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "chat_message",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"], childColumns = ["conversationId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("conversationId")],
)
internal data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val author: String,
    val text: String,
    val mine: Boolean,
    val createdAt: Long,
    val sortOrder: Int,
)

@Entity(
    tableName = "note",
    foreignKeys = [ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("workspaceId")],
)
internal data class NoteEntity(
    @PrimaryKey val id: String,
    val workspaceId: Int = WorkspaceEntity.SINGLETON_ID,
    val title: String,
    val body: String,
    val updatedAt: Long,
    val sortOrder: Int,
)

@Entity(
    tableName = "task",
    foreignKeys = [ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("workspaceId")],
)
internal data class TaskEntity(
    @PrimaryKey val id: String,
    val workspaceId: Int = WorkspaceEntity.SINGLETON_ID,
    val title: String,
    val done: Boolean,
    val dueAt: Long? = null,
    val recurrence: String = "NONE",
    val remindBeforeMillis: Long = 0,
    val sortOrder: Int,
)

@Entity(
    tableName = "memory",
    foreignKeys = [ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("workspaceId")],
)
internal data class MemoryEntity(
    @PrimaryKey val id: String,
    val workspaceId: Int = WorkspaceEntity.SINGLETON_ID,
    val text: String,
    val createdAt: Long,
    val sortOrder: Int,
)

@Entity(
    tableName = "gallery_item",
    foreignKeys = [ForeignKey(
        entity = WorkspaceEntity::class,
        parentColumns = ["id"], childColumns = ["workspaceId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("workspaceId")],
)
internal data class GalleryEntity(
    @PrimaryKey val id: String,
    val workspaceId: Int = WorkspaceEntity.SINGLETON_ID,
    val name: String,
    val privatePath: String,
    val createdAt: Long,
    val sortOrder: Int,
)
