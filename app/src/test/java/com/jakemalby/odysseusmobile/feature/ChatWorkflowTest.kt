package com.jakemalby.odysseusmobile

import com.jakemalby.odysseusmobile.core.Message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatWorkflowTest {
    private val messages = listOf(
        Message("u1", "You", "Plan an offline launch", true, 1),
        Message("a1", "Mobdysseus", "Here is the launch checklist", false, 2),
        Message("u2", "You", "Include device testing", true, 3),
        Message("a2", "Mobdysseus", "Test on the Galaxy device", false, 4),
    )

    @Test
    fun searchRequiresEveryTermAndPreservesMessageOrder() {
        assertEquals(listOf("a2"), filterChatMessages(messages, "galaxy device").map { it.id })
        assertEquals(listOf("u1", "a1", "u2", "a2"), filterChatMessages(messages, " ").map { it.id })
    }

    @Test
    fun retryUsesNearestPrecedingUserMessage() {
        assertEquals("Plan an offline launch", retryPromptFor(messages, "a1"))
        assertEquals("Include device testing", retryPromptFor(messages, "a2"))
        assertNull(retryPromptFor(messages, "u1"))
        assertNull(retryPromptFor(messages, "missing"))
    }
}
