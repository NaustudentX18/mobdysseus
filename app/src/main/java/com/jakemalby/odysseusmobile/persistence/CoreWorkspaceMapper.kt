package com.jakemalby.odysseusmobile.persistence

import com.jakemalby.odysseusmobile.core.Conversation
import com.jakemalby.odysseusmobile.core.GalleryItem
import com.jakemalby.odysseusmobile.core.Memory
import com.jakemalby.odysseusmobile.core.Message
import com.jakemalby.odysseusmobile.core.MobileSettings
import com.jakemalby.odysseusmobile.core.Note
import com.jakemalby.odysseusmobile.core.Task
import com.jakemalby.odysseusmobile.core.Workspace

/** Lossless mapping between the current core model and storage-neutral records. */
object CoreWorkspaceMapper {
    fun toSnapshot(workspace: Workspace): WorkspaceSnapshot = WorkspaceSnapshot(
        activeConversationId = workspace.activeConversationId,
        conversations = workspace.conversations.map(::conversationRecord),
        notes = workspace.notes.map(::noteRecord),
        tasks = workspace.tasks.map(::taskRecord),
        memories = workspace.memories.map(::memoryRecord),
        gallery = workspace.gallery.map(::galleryRecord),
        settings = WorkspaceSettingsRecord(
            selectedRecipe = workspace.settings.selectedRecipe,
            localOnly = workspace.settings.localOnly,
            compactDensity = workspace.settings.compactDensity,
        ),
    )

    /**
     * Core Workspace currently requires a selected conversation. A snapshot
     * without one is valid for a future empty-state UI but cannot be silently
     * coerced into today's core model without losing information.
     */
    fun toWorkspace(snapshot: WorkspaceSnapshot): Workspace = Workspace(
        activeConversationId = requireNotNull(snapshot.activeConversationId) {
            "Current core Workspace cannot represent a null active conversation"
        },
        conversations = snapshot.conversations.map(::conversation),
        notes = snapshot.notes.map(::note),
        tasks = snapshot.tasks.map(::task),
        memories = snapshot.memories.map(::memory),
        gallery = snapshot.gallery.map(::gallery),
        settings = MobileSettings(
            selectedRecipe = snapshot.settings.selectedRecipe,
            localOnly = snapshot.settings.localOnly,
            compactDensity = snapshot.settings.compactDensity,
        ),
    )

    private fun conversationRecord(value: Conversation) = ConversationRecord(
        id = value.id, title = value.title, messages = value.messages.map(::messageRecord),
    )
    private fun messageRecord(value: Message) = ChatMessageRecord(
        id = value.id, author = value.author, text = value.text, mine = value.mine, createdAt = value.createdAt,
    )
    private fun noteRecord(value: Note) = NoteRecord(value.id, value.title, value.body, value.updatedAt)
    private fun taskRecord(value: Task) = TaskRecord(value.id, value.title, value.done, value.dueAt, value.recurrence.name, value.remindBeforeMillis)
    private fun memoryRecord(value: Memory) = MemoryRecord(value.id, value.text, value.createdAt)
    private fun galleryRecord(value: GalleryItem) = GalleryRecord(value.id, value.name, value.path, value.createdAt)

    private fun conversation(value: ConversationRecord) = Conversation(
        id = value.id, title = value.title, messages = value.messages.map(::message),
    )
    private fun message(value: ChatMessageRecord) = Message(
        id = value.id, author = value.author, text = value.text, mine = value.mine, createdAt = value.createdAt,
    )
    private fun note(value: NoteRecord) = Note(value.id, value.title, value.body, value.updatedAt)
    private fun task(value: TaskRecord) = Task(value.id, value.title, value.done, value.dueAt, com.jakemalby.odysseusmobile.core.task.TaskRecurrence.valueOf(value.recurrence), value.remindBeforeMillis)
    private fun memory(value: MemoryRecord) = Memory(value.id, value.text, value.createdAt)
    private fun gallery(value: GalleryRecord) = GalleryItem(value.id, value.name, value.privatePath, value.createdAt)
}
