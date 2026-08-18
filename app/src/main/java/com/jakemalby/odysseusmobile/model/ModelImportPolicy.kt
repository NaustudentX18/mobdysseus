package com.jakemalby.odysseusmobile.model

/** Pure preflight rules for importing a user-selected LiteRT-LM artifact. */
object ModelImportPolicy {
    const val MIN_MODEL_BYTES = 1_000_001L
    const val STORAGE_RESERVE_BYTES = 512L * 1024L * 1024L

    sealed interface Decision {
        data class Allowed(val maximumCopyBytes: Long) : Decision
        data class Rejected(val reason: Reason) : Decision
    }

    enum class Reason { WRONG_EXTENSION, TOO_SMALL, INSUFFICIENT_STORAGE }

    fun evaluate(fileName: String, declaredBytes: Long?, availableStorageBytes: Long): Decision {
        if (!fileName.endsWith(".litertlm", ignoreCase = true)) {
            return Decision.Rejected(Reason.WRONG_EXTENSION)
        }
        if (declaredBytes != null && declaredBytes < MIN_MODEL_BYTES) {
            return Decision.Rejected(Reason.TOO_SMALL)
        }
        val maximumCopyBytes = (availableStorageBytes - STORAGE_RESERVE_BYTES).coerceAtLeast(0)
        if (maximumCopyBytes < MIN_MODEL_BYTES || declaredBytes != null && declaredBytes > maximumCopyBytes) {
            return Decision.Rejected(Reason.INSUFFICIENT_STORAGE)
        }
        return Decision.Allowed(maximumCopyBytes)
    }
}

sealed interface S25ProfileCompatibility {
    data object Ready : S25ProfileCompatibility
    data class Limited(val reasons: List<Reason>) : S25ProfileCompatibility

    enum class Reason { API_LEVEL, RAM, STORAGE, BACKEND }
}

fun S25ModelProfile.compatibility(device: DeviceProfile): S25ProfileCompatibility {
    val reasons = buildList {
        if (device.apiLevel < minimumApiLevel) add(S25ProfileCompatibility.Reason.API_LEVEL)
        if (device.availableRamBytes < minimumAvailableRamBytes) add(S25ProfileCompatibility.Reason.RAM)
        if (device.availableStorageBytes < minimumFreeStorageBytes) add(S25ProfileCompatibility.Reason.STORAGE)
        if (preferredBackends.none(device.availableBackends::contains)) add(S25ProfileCompatibility.Reason.BACKEND)
    }
    return if (reasons.isEmpty()) S25ProfileCompatibility.Ready else S25ProfileCompatibility.Limited(reasons)
}
