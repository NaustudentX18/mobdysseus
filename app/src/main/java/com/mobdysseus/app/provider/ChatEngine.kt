package com.mobdysseus.app.provider

import kotlinx.coroutines.flow.Flow

/** A streaming chat backend: remote API or on-device model. */
interface ChatEngine {
    fun stream(messages: List<ChatMessage>): Flow<String>
}
