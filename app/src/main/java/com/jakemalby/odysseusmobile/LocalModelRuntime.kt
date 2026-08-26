package com.jakemalby.odysseusmobile

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.util.UUID
import com.jakemalby.odysseusmobile.model.ModelImportPolicy
import com.jakemalby.odysseusmobile.model.AndroidDeviceProfileProbe
import com.jakemalby.odysseusmobile.model.BuiltinS25ModelCatalog
import com.jakemalby.odysseusmobile.model.S25ProfileCompatibility
import com.jakemalby.odysseusmobile.model.compatibility

internal data class ModelDeviceSummary(
    val manufacturer: String,
    val model: String,
    val apiLevel: Int,
    val availableRamBytes: Long,
    val totalRamBytes: Long,
    val availableStorageBytes: Long,
    val backends: String,
    val profiles: Map<String, ModelProfileSummary>,
)

internal data class ModelProfileSummary(
    val requiredRamBytes: Long,
    val requiredStorageBytes: Long,
    val chargingRecommended: Boolean,
    val limitations: List<String>,
)

/** Owns an app-private LiteRT-LM session. No prompt or document is sent off-device. */
internal class LocalModelRuntime(private val context: Context) : AutoCloseable {
    private val modelsDirectory = File(context.filesDir, "models").apply { mkdirs() }
    private val preferences = context.getSharedPreferences("mobdysseus_model_runtime", Context.MODE_PRIVATE)
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedModel: File? = null

    fun installedModels(): List<File> = modelsDirectory.listFiles()
        ?.filter { it.isFile && it.extension.equals("litertlm", ignoreCase = true) }
        ?.sortedBy { it.name.lowercase() }
        .orEmpty()

    fun deviceSummary(): ModelDeviceSummary {
        val facts = AndroidDeviceProfileProbe(context).probe()
        val profile = facts.asDeviceProfile()
        return ModelDeviceSummary(
            manufacturer = facts.manufacturer,
            model = facts.model,
            apiLevel = facts.apiLevel,
            availableRamBytes = facts.availableRamBytes,
            totalRamBytes = facts.totalRamBytes,
            availableStorageBytes = facts.availableStorageBytes,
            backends = facts.confirmedBackends.joinToString(),
            profiles = BuiltinS25ModelCatalog.value.profiles.associate { candidate ->
                val limitations = when (val fit = candidate.compatibility(profile)) {
                    S25ProfileCompatibility.Ready -> emptyList()
                    is S25ProfileCompatibility.Limited -> fit.reasons.map { it.name.lowercase().replace('_', ' ') }
                }
                candidate.id to ModelProfileSummary(
                    requiredRamBytes = candidate.minimumAvailableRamBytes,
                    requiredStorageBytes = candidate.minimumFreeStorageBytes,
                    chargingRecommended = candidate.chargingRecommended,
                    limitations = limitations,
                )
            },
        )
    }

    fun selectedModel(): File? = preferences.getString("selected_model", null)
        ?.let(::File)
        ?.takeIf { candidate -> candidate.isFile && candidate.parentFile?.canonicalPath == modelsDirectory.canonicalPath }

    fun selectModel(model: File): Result<Unit> = runCatching {
        require(model.isFile && model.extension.equals("litertlm", ignoreCase = true)) { "Choose an installed .litertlm model." }
        require(model.parentFile?.canonicalPath == modelsDirectory.canonicalPath) { "Model is outside app-private storage." }
        if (loadedModel?.canonicalPath != model.canonicalPath) close()
        preferences.edit().putString("selected_model", model.canonicalPath).commit()
        Unit
    }

    suspend fun importModel(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val metadata = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) null else {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) cursor.getString(nameIndex) else null
                    val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                    name to size
                }
            }
            val safeName = (metadata?.first ?: "mobdysseus-model.litertlm")
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .takeLast(120)
                .ifBlank { "mobdysseus-model.litertlm" }
            val destination = File(modelsDirectory, safeName)
            val available = StatFs(modelsDirectory.absolutePath).availableBytes.coerceAtLeast(0L)
            val decision = ModelImportPolicy.evaluate(safeName, metadata?.second, available)
            val maximumBytes = when (decision) {
                is ModelImportPolicy.Decision.Allowed -> decision.maximumCopyBytes
                is ModelImportPolicy.Decision.Rejected -> error(decision.reason.userMessage())
            }
            val staged = File(modelsDirectory, ".import-${UUID.randomUUID()}.part")
            try {
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(staged).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            if (read == 0) continue
                            total += read
                            require(total <= maximumBytes) { "Model exceeds safe free space; import cancelled." }
                            output.write(buffer, 0, read)
                        }
                        output.fd.sync()
                        total
                    }
                } ?: error("Android could not open that model file.")
                require(copied >= ModelImportPolicy.MIN_MODEL_BYTES) { "That file is too small to be a LiteRT-LM model." }
                metadata?.second?.let { declared -> require(copied == declared) { "Model copy was incomplete." } }
                if (loadedModel?.canonicalPath == destination.canonicalPath) close()
                try {
                    Files.move(staged.toPath(), destination.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
                } catch (error: AtomicMoveNotSupportedException) {
                    throw IllegalStateException("This device storage cannot safely activate the imported model.", error)
                }
            } finally {
                staged.delete()
            }
            preferences.edit().putString("selected_model", destination.canonicalPath).commit()
            destination
        }
    }

    private fun ModelImportPolicy.Reason.userMessage(): String = when (this) {
        ModelImportPolicy.Reason.WRONG_EXTENSION -> "Choose a .litertlm model file."
        ModelImportPolicy.Reason.TOO_SMALL -> "That file is too small to be a LiteRT-LM model."
        ModelImportPolicy.Reason.INSUFFICIENT_STORAGE -> "Not enough free space to import safely (512 MB must remain free)."
    }

    fun removeModel(model: File): Result<Unit> = runCatching {
        require(model.parentFile?.canonicalPath == modelsDirectory.canonicalPath) { "Invalid model location." }
        if (loadedModel?.canonicalPath == model.canonicalPath) close()
        if (selectedModel()?.canonicalPath == model.canonicalPath) preferences.edit().remove("selected_model").commit()
        require(model.delete()) { "Could not remove ${model.name}." }
    }

    suspend fun reply(
        prompt: String,
        systemInstruction: String = "You are Mobdysseus, a private, concise assistant running entirely on this Android phone.",
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        topK: Int = 32,
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            val model = selectedModel() ?: installedModels().firstOrNull()?.also { selectModel(it).getOrThrow() }
                ?: error("No local model is installed. In Cookbook, import a .litertlm model into this phone.")
            if (loadedModel?.absolutePath != model.absolutePath) load(model, systemInstruction, temperature, topP, topK)
            conversation?.sendMessage(Contents.of(prompt))?.contents?.contents
                ?.filterIsInstance<Content.Text>()
                ?.joinToString("") { it.text }
                ?.trim().orEmpty().ifBlank {
                error("The local model returned an empty response.")
            }
        }
    }

    fun streamReply(
        prompt: String,
        systemInstruction: String = "You are Mobdysseus, a private, concise assistant running entirely on this Android phone.",
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        topK: Int = 32,
    ): Flow<String> = flow {
        val activeConversation = withContext(Dispatchers.Default) {
            val model = selectedModel() ?: installedModels().firstOrNull()?.also { selectModel(it).getOrThrow() }
                ?: error("No local model is installed. In Cookbook, import a .litertlm model into this phone.")
            if (loadedModel?.absolutePath != model.absolutePath) load(model, systemInstruction, temperature, topP, topK)
            conversation ?: error("The local conversation could not be created.")
        }
        activeConversation.sendMessageAsync(Contents.of(prompt)).collect { response ->
            val text = response.contents.contents
                .filterIsInstance<Content.Text>()
                .joinToString("") { it.text }
            if (text.isNotEmpty()) emit(text)
        }
    }

    private fun load(
        model: File,
        systemInstruction: String = "You are Mobdysseus, a private, concise assistant running entirely on this Android phone.",
        temperature: Float = 0.7f,
        topP: Float = 0.9f,
        topK: Int = 32,
    ) {
        close()
        val newEngine = runCatching {
            Engine(EngineConfig(
                modelPath = model.absolutePath,
                backend = Backend.GPU(),
                cacheDir = context.cacheDir.absolutePath,
            )).apply { initialize() }
        }.recoverCatching {
            Engine(EngineConfig(
                modelPath = model.absolutePath,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.absolutePath,
            )).apply { initialize() }
        }.getOrThrow()

        engine = newEngine
        conversation = newEngine.createConversation(ConversationConfig(
            systemInstruction = Contents.of(systemInstruction),
            samplerConfig = SamplerConfig(
                topK = topK.coerceIn(1, 128),
                topP = topP.coerceIn(0.1f, 1.0f),
                temperature = temperature.coerceIn(0.0f, 2.0f),
            ),
        ))
        loadedModel = model
    }

    override fun close() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
        loadedModel = null
    }
}
