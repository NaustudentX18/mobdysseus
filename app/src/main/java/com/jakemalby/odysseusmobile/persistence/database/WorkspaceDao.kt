package com.jakemalby.odysseusmobile.persistence.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface WorkspaceDao {
    @Query("SELECT * FROM workspace WHERE id = :id") suspend fun workspace(id: Int = WorkspaceEntity.SINGLETON_ID): WorkspaceEntity?
    @Query("SELECT * FROM conversation WHERE workspaceId = :workspaceId ORDER BY sortOrder") suspend fun conversations(workspaceId: Int = WorkspaceEntity.SINGLETON_ID): List<ConversationEntity>
    @Query("SELECT * FROM chat_message WHERE conversationId = :conversationId ORDER BY sortOrder") suspend fun messages(conversationId: String): List<ChatMessageEntity>
    @Query("SELECT * FROM note WHERE workspaceId = :workspaceId ORDER BY sortOrder") suspend fun notes(workspaceId: Int = WorkspaceEntity.SINGLETON_ID): List<NoteEntity>
    @Query("SELECT * FROM task WHERE workspaceId = :workspaceId ORDER BY sortOrder") suspend fun tasks(workspaceId: Int = WorkspaceEntity.SINGLETON_ID): List<TaskEntity>
    @Query("SELECT * FROM memory WHERE workspaceId = :workspaceId ORDER BY sortOrder") suspend fun memories(workspaceId: Int = WorkspaceEntity.SINGLETON_ID): List<MemoryEntity>
    @Query("SELECT * FROM gallery_item WHERE workspaceId = :workspaceId ORDER BY sortOrder") suspend fun gallery(workspaceId: Int = WorkspaceEntity.SINGLETON_ID): List<GalleryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWorkspace(value: WorkspaceEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertConversations(values: List<ConversationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMessages(values: List<ChatMessageEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNotes(values: List<NoteEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTasks(values: List<TaskEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMemories(values: List<MemoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGallery(values: List<GalleryEntity>)

    @Query("DELETE FROM workspace") suspend fun deleteWorkspace()
}
