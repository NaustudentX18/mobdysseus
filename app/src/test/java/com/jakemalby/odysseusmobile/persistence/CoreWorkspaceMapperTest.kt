package com.jakemalby.odysseusmobile.persistence

import com.jakemalby.odysseusmobile.core.Conversation
import com.jakemalby.odysseusmobile.core.GalleryItem
import com.jakemalby.odysseusmobile.core.Memory
import com.jakemalby.odysseusmobile.core.Message
import com.jakemalby.odysseusmobile.core.MobileSettings
import com.jakemalby.odysseusmobile.core.Note
import com.jakemalby.odysseusmobile.core.Task
import com.jakemalby.odysseusmobile.core.Workspace
import com.jakemalby.odysseusmobile.core.task.TaskRecurrence
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreWorkspaceMapperTest {
    @Test
    fun `round trip preserves every populated core workspace field`() {
        val workspace = Workspace(
            conversations = listOf(
                Conversation("active", "Active", listOf(Message("m1", "You", "A precise message", true, 101))),
                Conversation("empty", "Empty conversation", emptyList()),
            ),
            activeConversationId = "active",
            notes = listOf(Note("n1", "Title", "Body", 102)),
            tasks = listOf(Task("t1", "Task", true)),
            memories = listOf(Memory("memory1", "Remember this", 103)),
            gallery = listOf(GalleryItem("g1", "photo.jpg", "/private/gallery/photo.jpg", 104)),
            settings = MobileSettings("Deep private work", localOnly = false, compactDensity = true),
        )

        val snapshot = CoreWorkspaceMapper.toSnapshot(workspace)

        assertEquals(workspace, CoreWorkspaceMapper.toWorkspace(snapshot))
        assertEquals("/private/gallery/photo.jpg", snapshot.gallery.single().privatePath)
        assertEquals("Deep private work", snapshot.settings.selectedRecipe)
        assertEquals("empty", snapshot.conversations.single { it.id == "empty" }.id)
        assertEquals(emptyList<ChatMessageRecord>(), snapshot.conversations.single { it.id == "empty" }.messages)
    }

    @Test
    fun `task due date and recurrence round trip`() {
        val workspace = Workspace(
            conversations = listOf(Conversation("c", "Chat", emptyList())),
            activeConversationId = "c",
            notes = emptyList(),
            tasks = listOf(Task("t1", "Pay rent", false, dueAt = 1_800_000_000_000L, recurrence = TaskRecurrence.MONTHLY, remindBeforeMillis = 86_400_000L)),
            memories = emptyList(), gallery = emptyList(), settings = MobileSettings(),
        )

        val restored = CoreWorkspaceMapper.toWorkspace(CoreWorkspaceMapper.toSnapshot(workspace))

        assertEquals(1_800_000_000_000L, restored.tasks.single().dueAt)
        assertEquals(TaskRecurrence.MONTHLY, restored.tasks.single().recurrence)
        assertEquals(86_400_000L, restored.tasks.single().remindBeforeMillis)
    }

    @Test
    fun `snapshot maps to core with active empty conversation`() {
        val snapshot = WorkspaceSnapshot(
            activeConversationId = "empty",
            conversations = listOf(ConversationRecord("empty", "Fresh", emptyList())),
            notes = emptyList(), tasks = emptyList(), memories = emptyList(), gallery = emptyList(),
            settings = WorkspaceSettingsRecord("Private quick chat", localOnly = true, compactDensity = false),
        )

        assertEquals(
            Workspace(
                conversations = listOf(Conversation("empty", "Fresh", emptyList())),
                activeConversationId = "empty",
                notes = emptyList(), tasks = emptyList(), memories = emptyList(), gallery = emptyList(),
                settings = MobileSettings(),
            ),
            CoreWorkspaceMapper.toWorkspace(snapshot),
        )
    }
}
