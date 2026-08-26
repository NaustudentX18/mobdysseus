package com.mobdysseus.app.local

import android.content.Context
import io.aatricks.llmedge.LLMEdge
import io.aatricks.llmedge.model.ModelSpec
import kotlinx.coroutines.CoroutineScope
import java.io.File

/**
 * Owns a single [LLMEdge] instance and manages on-device model acquisition.
 *
 * Downloads (or resolves from cache) a HuggingFace GGUF via llmedge's model
 * repository, reporting download progress through [onProgress] in the range
 * 0f..1f. This is the piece the Cookbook UI drives when a user taps "download"
 * on a recommended model; the resulting cached [File] is what a [LocalLlmEngine]
 * (or a service-hosted engine) later loads for inference.
 */
class ModelDownloadManager(
    context: Context,
    scope: CoroutineScope,
) {

    private val edge: LLMEdge = LLMEdge.create(context = context, scope = scope)

    /**
     * Fetch [repoId]/[filename] from HuggingFace (or return the cached copy),
     * emitting progress via [onProgress].
     *
     * The resolved file is validated (exists, > 1 MB, GGUF magic) before being
     * returned; a corrupt/incomplete download is deleted so the user can retry.
     *
     * @return the cached model [File].
     * @throws IllegalStateException with a descriptive message if prefetch fails.
     */
    suspend fun prefetch(
        repoId: String,
        filename: String,
        onProgress: (Float) -> Unit = {},
    ): File {
        val spec = ModelSpec.huggingFace(repoId = repoId, filename = filename)
        try {
            onProgress(0f)
            val file = edge.models.prefetch(spec) { event ->
                val total = event.totalBytes
                if (total != null && total > 0L) {
                    val fraction = event.downloadedBytes.toFloat() / total.toFloat()
                    onProgress(fraction.coerceIn(0f, 1f))
                }
            }
            // Validate the downloaded/cached file so a corrupt GGUF is caught
            // here (and deleted) rather than crashing inference later.
            LocalLlmEngine.validateModelFile(file)
            onProgress(1f)
            return file
        } catch (t: Throwable) {
            throw IllegalStateException(
                "Failed to prefetch model $repoId/$filename: ${t.message}",
                t,
            )
        }
    }
}
