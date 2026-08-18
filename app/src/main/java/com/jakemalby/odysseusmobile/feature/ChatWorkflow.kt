package com.jakemalby.odysseusmobile

import com.jakemalby.odysseusmobile.core.Message

internal fun filterChatMessages(messages: List<Message>, query: String): List<Message> {
    val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (terms.isEmpty()) return messages
    return messages.filter { message ->
        val searchable = "${message.author} ${message.text}".lowercase()
        terms.all(searchable::contains)
    }
}

/** The user message immediately preceding an assistant response is its retry input. */
internal fun retryPromptFor(messages: List<Message>, assistantMessageId: String): String? {
    val responseIndex = messages.indexOfFirst { it.id == assistantMessageId && !it.mine }
    if (responseIndex <= 0) return null
    return messages.subList(0, responseIndex).lastOrNull { it.mine }?.text?.takeIf { it.isNotBlank() }
}
