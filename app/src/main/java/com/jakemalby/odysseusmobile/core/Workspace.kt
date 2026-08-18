package com.jakemalby.odysseusmobile.core

import com.jakemalby.odysseusmobile.core.task.TaskRecurrence
import java.util.UUID

data class Message(val id: String, val author: String, val text: String, val mine: Boolean, val createdAt: Long)
data class Conversation(val id: String, val title: String, val messages: List<Message>)
data class Note(val id: String, val title: String, val body: String, val updatedAt: Long)
data class Task(
    val id: String,
    val title: String,
    val done: Boolean,
    val dueAt: Long? = null,
    val recurrence: TaskRecurrence = TaskRecurrence.NONE,
    val remindBeforeMillis: Long = 0,
)
data class Memory(val id: String, val text: String, val createdAt: Long)
data class GalleryItem(val id: String, val name: String, val path: String, val createdAt: Long)
data class MobileSettings(val selectedRecipe: String = "Private quick chat", val localOnly: Boolean = true, val compactDensity: Boolean = false)
data class Workspace(
    val conversations: List<Conversation>,
    val activeConversationId: String,
    val notes: List<Note>,
    val tasks: List<Task>,
    val memories: List<Memory>,
    val gallery: List<GalleryItem>,
    val settings: MobileSettings,
)

fun seedWorkspace(): Workspace {
    val welcome = Message(UUID.randomUUID().toString(), "Mobdysseus", "Welcome aboard. This workspace is native to your phone: chats, notes, tasks, memory, and model choices are stored locally. Open Cookbook to choose how you want to work.", false, System.currentTimeMillis())
    val chat = Conversation(UUID.randomUUID().toString(), "New workspace", listOf(welcome))
    return Workspace(
        conversations = listOf(chat), activeConversationId = chat.id,
        notes = listOf(Note(UUID.randomUUID().toString(), "Welcome to Mobdysseus", "This is your private, local-first workspace. Add thoughts, tasks, memories, and models as you go.", System.currentTimeMillis())),
        tasks = listOf(Task(UUID.randomUUID().toString(), "Choose a local model from Cookbook", false)),
        memories = emptyList(), gallery = emptyList(), settings = MobileSettings(),
    )
}
