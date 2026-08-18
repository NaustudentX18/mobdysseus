package com.jakemalby.odysseusmobile.model

/**
 * Immutable metadata for a model artifact. This is deliberately independent of
 * Compose, Android networking, and LiteRT-LM so catalog data can be verified
 * before an artifact is ever exposed to the runtime.
 */
data class ModelCatalogEntry(
    val id: String,
    val version: String,
    val displayName: String,
    val artifactUri: String,
    val sha256: String,
    val byteSize: Long,
    val minRamBytes: Long,
    val minApiLevel: Int,
    val format: ModelFormat,
    val supportedBackends: Set<ModelBackend>,
    val capabilities: Set<ModelCapability>,
) {
    init {
        require(id.matches(ID_PATTERN)) { "Model id must contain only lowercase letters, numbers, . _ or -" }
        require(version.isNotBlank()) { "Model version is required" }
        require(displayName.isNotBlank()) { "Model display name is required" }
        require(artifactUri.startsWith("https://")) { "Model artifacts must use HTTPS" }
        require(Sha256.isCanonical(sha256)) { "Model SHA-256 must be 64 lowercase hexadecimal characters" }
        require(byteSize > 0) { "Model byte size must be positive" }
        require(minRamBytes > 0) { "Model minimum RAM must be positive" }
        require(minApiLevel > 0) { "Model minimum API level must be positive" }
        require(supportedBackends.isNotEmpty()) { "A model must support at least one backend" }
    }

    /** Stable file-system-safe identity; never use a server-provided filename. */
    val artifactKey: String get() = "$id-$version"

    companion object {
        private val ID_PATTERN = Regex("[a-z0-9][a-z0-9._-]*")
    }
}

enum class ModelFormat(val extension: String) {
    LITERT_LM("litertlm"),
}

enum class ModelBackend { GPU, NPU, CPU }

enum class ModelCapability { TEXT, VISION, AUDIO, TOOL_USE }

data class DeviceProfile(
    val apiLevel: Int,
    val availableRamBytes: Long,
    val availableStorageBytes: Long,
    val availableBackends: Set<ModelBackend>,
)

sealed interface ModelCompatibility {
    data class Compatible(val preferredBackend: ModelBackend) : ModelCompatibility

    data class Incompatible(val reasons: List<Reason>) : ModelCompatibility {
        init { require(reasons.isNotEmpty()) }
    }

    enum class Reason {
        API_LEVEL,
        INSUFFICIENT_RAM,
        INSUFFICIENT_STORAGE,
        NO_SUPPORTED_BACKEND,
    }
}

object ModelCompatibilityChecker {
    /**
     * Evaluates only facts supplied by the device probe. Callers reserve extra
     * storage for staging before constructing [DeviceProfile].
     */
    fun check(entry: ModelCatalogEntry, device: DeviceProfile): ModelCompatibility {
        val reasons = buildList {
            if (device.apiLevel < entry.minApiLevel) add(ModelCompatibility.Reason.API_LEVEL)
            if (device.availableRamBytes < entry.minRamBytes) add(ModelCompatibility.Reason.INSUFFICIENT_RAM)
            if (device.availableStorageBytes < entry.byteSize) add(ModelCompatibility.Reason.INSUFFICIENT_STORAGE)
            if (entry.supportedBackends.intersect(device.availableBackends).isEmpty()) add(ModelCompatibility.Reason.NO_SUPPORTED_BACKEND)
        }
        if (reasons.isNotEmpty()) return ModelCompatibility.Incompatible(reasons)

        val preferred = BACKEND_ORDER.first { it in entry.supportedBackends && it in device.availableBackends }
        return ModelCompatibility.Compatible(preferred)
    }

    private val BACKEND_ORDER = listOf(ModelBackend.NPU, ModelBackend.GPU, ModelBackend.CPU)
}
