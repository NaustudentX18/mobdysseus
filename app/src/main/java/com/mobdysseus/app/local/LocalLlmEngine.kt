package com.mobdysseus.app.local

import android.content.Context
import com.mobdysseus.app.provider.ChatEngine
import com.mobdysseus.app.provider.ChatMessage
import io.aatricks.llmedge.LLMEdge
import io.aatricks.llmedge.core.InferenceFailedException
import io.aatricks.llmedge.core.InsufficientMemoryException
import io.aatricks.llmedge.core.InvalidModelFileException
import io.aatricks.llmedge.core.ModelFileNotFoundException
import io.aatricks.llmedge.core.ModelLoadException
import io.aatricks.llmedge.core.WorkerCrashedException
import io.aatricks.llmedge.core.WorkerKilledByMemoryException
import io.aatricks.llmedge.model.ModelSpec
import io.aatricks.llmedge.text.TextGenerationRequest
import io.aatricks.llmedge.text.TextModelOptions
import io.aatricks.llmedge.text.TextStreamEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/**
 * On-device GGUF chat engine backed by llmedge (llama.cpp JNI).
 * Streams tokens locally; no network required after the model is cached.
 *
 * The [LLMEdge] is created lazily on the first [stream] call, off the main
 * thread, so a corrupt or oversized model can never crash the app at startup.
 * All llmedge failures are caught and surfaced as a single friendly error
 * chunk instead of crashing the process.
 */
class LocalLlmEngine(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repoId: String,
    private val filename: String,
) : ChatEngine {

    private val modelSpec = ModelSpec.huggingFace(repoId = repoId, filename = filename)

    override fun stream(messages: List<ChatMessage>): Flow<String> = flow {
        // Create the edge lazily here (off the main thread via flowOn(IO))
        // rather than in the constructor, so nothing heavy runs at app startup.
        val edge = LLMEdge.create(context = context, scope = scope)
        try {
            // Resolve the cached model file (downloading it if needed) and
            // validate it before loading, so a corrupt/incomplete GGUF is
            // caught and deleted instead of crashing the native worker.
            val modelFile = edge.models.prefetch(modelSpec)
            validateModelFile(modelFile)

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
        } catch (e: InsufficientMemoryException) {
            emit("Model is too large for this device's available memory. Pick a smaller model in Settings.")
        } catch (e: WorkerKilledByMemoryException) {
            emit("Model is too large for this device's available memory. Pick a smaller model in Settings.")
        } catch (e: InvalidModelFileException) {
            emit("Model file is missing or corrupt. Re-download it from the Cookbook or Settings.")
        } catch (e: ModelLoadException) {
            emit("Model file is missing or corrupt. Re-download it from the Cookbook or Settings.")
        } catch (e: ModelFileNotFoundException) {
            emit("Model file is missing or corrupt. Re-download it from the Cookbook or Settings.")
        } catch (e: WorkerCrashedException) {
            emit("The on-device model crashed. Try a smaller model or re-download.")
        } catch (e: InferenceFailedException) {
            emit("The on-device model crashed. Try a smaller model or re-download.")
        } catch (e: Exception) {
            emit("On-device inference failed: ${e.message ?: "unknown error"}")
        }
    }.flowOn(Dispatchers.IO)

    private fun buildPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        sb.append("You are Mobdysseus, a helpful assistant running fully on-device. Answer clearly and concisely.\n\n")
        // Bound multi-turn context to the most recent turns so long conversations
        // stay within the generation token budget (maxTokens = 1024).
        val recent = messages.takeLast(8)
        for (m in recent) {
            when (m.role) {
                "system" -> sb.append("System: ").append(m.content).append("\n\n")
                "user" -> sb.append("User: ").append(m.content).append("\n\n")
                "assistant" -> sb.append("Assistant: ").append(m.content).append("\n\n")
            }
        }
        sb.append("Assistant: ")
        return sb.toString()
    }

    companion object {
        /**
         * Validates a cached GGUF model file before it is loaded for inference.
         * Checks that the file exists, is larger than 1 MB, and starts with the
         * GGUF magic bytes ("GGUF" = 0x47 0x47 0x55 0x46). On failure the corrupt
         * file is deleted so the user can re-download it, and a descriptive
         * llmedge exception is thrown (caught upstream and shown to the user).
         */
        fun validateModelFile(file: File) {
            if (!file.exists() || !file.isFile) {
                throw ModelFileNotFoundException("Model file not found: ${file.absolutePath}", file.absolutePath)
            }
            if (file.length() <= 1_048_576L) {
                file.delete()
                throw InvalidModelFileException(
                    "Model file is too small (${file.length()} bytes) and likely corrupt. Deleted; re-download it.",
                    file.absolutePath,
                    null,
                )
            }
            val magic = ByteArray(4)
            file.inputStream().use { it.read(magic) }
            val isGguf = magic[0] == 0x47.toByte() &&
                magic[1] == 0x47.toByte() &&
                magic[2] == 0x55.toByte() &&
                magic[3] == 0x46.toByte()
            if (!isGguf) {
                file.delete()
                throw InvalidModelFileException(
                    "Model file is not a valid GGUF file. Deleted; re-download it.",
                    file.absolutePath,
                    null,
                )
            }
        }
    }
}
