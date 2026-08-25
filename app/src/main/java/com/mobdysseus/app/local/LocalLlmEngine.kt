package com.mobdysseus.app.local

import android.content.Context
import com.mobdysseus.app.provider.ChatEngine
import com.mobdysseus.app.provider.ChatMessage
import io.aatricks.llmedge.LLMEdge
import io.aatricks.llmedge.model.ModelSpec
import io.aatricks.llmedge.text.TextGenerationRequest
import io.aatricks.llmedge.text.TextModelOptions
import io.aatricks.llmedge.text.TextStreamEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * On-device GGUF chat engine backed by llmedge (llama.cpp JNI).
 * Streams tokens locally; no network required after the model is cached.
 * The first request triggers a HuggingFace download of the GGUF, then
 * llmedge caches it for subsequent runs.
 */
class LocalLlmEngine(
    context: Context,
    scope: CoroutineScope,
    private val repoId: String,
    private val filename: String,
) : ChatEngine {

    private val edge = LLMEdge.create(context = context, scope = scope)
    private val modelSpec = ModelSpec.huggingFace(repoId = repoId, filename = filename)

    override fun stream(messages: List<ChatMessage>): Flow<String> = flow {
        val request = TextGenerationRequest(
            prompt = buildPrompt(messages),
            model = modelSpec,
            options = TextModelOptions(useVulkan = false, useFlashAttention = false),
            maxTokens = 1024,
        )
        edge.text.stream(request).collect { event ->
            if (event is TextStreamEvent.Chunk) {
                emit(event.value)
            }
        }
    }

    private fun buildPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        sb.append("You are Mobdysseus, a helpful assistant running fully on-device. Answer clearly and concisely.\n\n")
        for (m in messages) {
            when (m.role) {
                "system" -> sb.append("System: ").append(m.content).append("\n\n")
                "user" -> sb.append("User: ").append(m.content).append("\n\n")
                "assistant" -> sb.append("Assistant: ").append(m.content).append("\n\n")
            }
        }
        sb.append("Assistant: ")
        return sb.toString()
    }
}
