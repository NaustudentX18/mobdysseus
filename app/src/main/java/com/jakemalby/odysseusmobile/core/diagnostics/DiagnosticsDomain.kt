package com.jakemalby.odysseusmobile.core.diagnostics

/** Components whose health can be reported without inspecting private content. */
enum class DiagnosticComponent {
    DATABASE,
    MODEL,
    RETRIEVAL_INDEX,
    PERMISSIONS,
    STORAGE,
}

enum class HealthLevel {
    HEALTHY,
    DEGRADED,
    UNAVAILABLE,
    UNKNOWN,
}

/**
 * Allow-listed, content-free health reasons. Exception messages must never be mapped here.
 */
enum class HealthReason {
    NONE,
    NOT_CHECKED,
    INITIALIZING,
    PERMISSION_DENIED,
    NO_MODEL_INSTALLED,
    MODEL_NOT_LOADED,
    MODEL_INCOMPATIBLE,
    DATABASE_LOCKED,
    DATABASE_MIGRATION_REQUIRED,
    DATABASE_INTEGRITY_CHECK_FAILED,
    INDEX_EMPTY,
    INDEX_STALE,
    INDEX_REBUILDING,
    STORAGE_LOW,
    STORAGE_FULL,
    TEMPORARILY_BUSY,
}

data class ComponentHealth(
    val component: DiagnosticComponent,
    val level: HealthLevel,
    val reason: HealthReason,
) {
    init {
        require((level == HealthLevel.HEALTHY) == (reason == HealthReason.NONE)) {
            "Healthy components must use NONE; unhealthy components must use a safe reason"
        }
        require(reason == HealthReason.NONE || reason in component.allowedReasons) {
            "Health reason does not apply to this component"
        }
    }

    companion object {
        fun healthy(component: DiagnosticComponent) =
            ComponentHealth(component, HealthLevel.HEALTHY, HealthReason.NONE)
    }
}

private val DiagnosticComponent.allowedReasons: Set<HealthReason>
    get() {
        val lifecycleReasons = setOf(
            HealthReason.NOT_CHECKED,
            HealthReason.INITIALIZING,
            HealthReason.TEMPORARILY_BUSY,
        )
        val componentReasons = when (this) {
            DiagnosticComponent.DATABASE -> setOf(
                HealthReason.DATABASE_LOCKED,
                HealthReason.DATABASE_MIGRATION_REQUIRED,
                HealthReason.DATABASE_INTEGRITY_CHECK_FAILED,
            )
            DiagnosticComponent.MODEL -> setOf(
                HealthReason.NO_MODEL_INSTALLED,
                HealthReason.MODEL_NOT_LOADED,
                HealthReason.MODEL_INCOMPATIBLE,
            )
            DiagnosticComponent.RETRIEVAL_INDEX -> setOf(
                HealthReason.INDEX_EMPTY,
                HealthReason.INDEX_STALE,
                HealthReason.INDEX_REBUILDING,
            )
            DiagnosticComponent.PERMISSIONS -> setOf(HealthReason.PERMISSION_DENIED)
            DiagnosticComponent.STORAGE -> setOf(
                HealthReason.STORAGE_LOW,
                HealthReason.STORAGE_FULL,
            )
        }
        return lifecycleReasons + componentReasons
    }

enum class LocalFeature {
    CHAT,
    MODEL_INFERENCE,
    DOCUMENT_IMPORT,
    RETRIEVAL,
    VOICE_INPUT,
    TEXT_TO_SPEECH,
    CAMERA_IMPORT,
    CALENDAR_READ,
    CALENDAR_WRITE,
    CONTACT_PICKER,
    BACKUP_EXPORT,
    BACKUP_RESTORE,
}

/** User-actionable, allow-listed reasons for a local feature being unavailable. */
enum class FeatureUnavailableReason {
    COMPONENT_UNAVAILABLE,
    PERMISSION_REQUIRED,
    MODEL_REQUIRED,
    STORAGE_REQUIRED,
    DEVICE_UNSUPPORTED,
    DISABLED_BY_USER,
    TEMPORARILY_BUSY,
}

sealed interface FeatureAvailability {
    data object Available : FeatureAvailability
    data class Unavailable(val reason: FeatureUnavailableReason) : FeatureAvailability
}

data class FeatureHealth(
    val feature: LocalFeature,
    val availability: FeatureAvailability,
)

enum class StorageCleanupTarget {
    MODEL_DOWNLOAD_TEMPORARIES,
    DOCUMENT_IMPORT_TEMPORARIES,
    RETRIEVAL_REBUILD_ARTIFACTS,
    EXPIRED_AUDIO_CAPTURES,
    EXPIRED_CAMERA_CAPTURES,
    ORPHANED_PRIVATE_BLOBS,
}

enum class CleanupAvailabilityReason {
    READY,
    NOTHING_TO_CLEAN,
    CURRENTLY_IN_USE,
    SCAN_REQUIRED,
}

/**
 * Aggregate cleanup information only. It intentionally carries neither names nor paths.
 * Execution code must re-resolve the allow-listed target inside app-private storage.
 */
data class StorageCleanupCandidate(
    val target: StorageCleanupTarget,
    val estimatedBytes: Long,
    val itemCount: Int,
    val availabilityReason: CleanupAvailabilityReason,
) {
    init {
        require(estimatedBytes >= 0)
        require(itemCount >= 0)
        if (availabilityReason == CleanupAvailabilityReason.NOTHING_TO_CLEAN) {
            require(estimatedBytes == 0L && itemCount == 0)
        }
    }

    val canClean: Boolean
        get() = availabilityReason == CleanupAvailabilityReason.READY && itemCount > 0
}

enum class PermissionGrantState {
    GRANTED,
    DENIED,
    NOT_REQUESTED,
    NOT_APPLICABLE,
}

enum class AppPermission {
    CAMERA,
    MICROPHONE,
    NOTIFICATIONS,
    CALENDAR_READ,
    CALENDAR_WRITE,
}

data class PermissionHealth(
    val permission: AppPermission,
    val state: PermissionGrantState,
)

/**
 * Complete input accepted by the redacted exporter. Every value is an enum or a bounded
 * aggregate; private content, arbitrary identifiers, exception text and filesystem paths
 * are unrepresentable by this type.
 */
data class RedactedDiagnosticsSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val generatedAtEpochSeconds: Long,
    val componentHealth: List<ComponentHealth>,
    val featureHealth: List<FeatureHealth>,
    val permissionHealth: List<PermissionHealth>,
    val cleanupCandidates: List<StorageCleanupCandidate>,
    val installedModelCount: Int,
    val indexedDocumentCount: Int,
    val databaseRecordCount: Long,
    val appPrivateBytesUsed: Long,
    val appPrivateBytesAvailable: Long,
) {
    init {
        require(schemaVersion == CURRENT_SCHEMA_VERSION)
        require(generatedAtEpochSeconds >= 0)
        require(installedModelCount >= 0)
        require(indexedDocumentCount >= 0)
        require(databaseRecordCount >= 0)
        require(appPrivateBytesUsed >= 0)
        require(appPrivateBytesAvailable >= 0)
        require(componentHealth.map { it.component }.distinct().size == componentHealth.size)
        require(featureHealth.map { it.feature }.distinct().size == featureHealth.size)
        require(permissionHealth.map { it.permission }.distinct().size == permissionHealth.size)
        require(cleanupCandidates.map { it.target }.distinct().size == cleanupCandidates.size)
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
