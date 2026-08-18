package com.jakemalby.odysseusmobile.persistence

/**
 * Coordinates, but does not own, the legacy preference lifecycle.
 *
 * Wire [readLegacyPlaintext] to the existing Keystore decryptor and
 * [retireLegacy] to preference removal. Retirement happens only after Room's
 * transactional [WorkspaceRepository.replace] has returned successfully.
 */
class V0RoomMigrationCoordinator(
    private val repository: WorkspaceRepository,
    private val readLegacyPlaintext: suspend () -> String?,
    private val retireLegacy: suspend () -> Unit,
    private val codec: LegacyWorkspaceMigrator = V0WorkspaceMigrationCodec,
) {
    suspend fun migrateIfNeeded(): LegacyRoomMigrationResult {
        if (repository.read() != null) return LegacyRoomMigrationResult.AlreadyMigrated
        val raw = readLegacyPlaintext() ?: return LegacyRoomMigrationResult.NoLegacyWorkspace
        return when (val result = codec.migrate(raw)) {
            is LegacyMigrationResult.Rejected -> LegacyRoomMigrationResult.Rejected(result.reason)
            is LegacyMigrationResult.Migrated -> {
                repository.replace(result.snapshot)
                retireLegacy()
                LegacyRoomMigrationResult.Migrated(result.sourceVersion)
            }
        }
    }
}

sealed interface LegacyRoomMigrationResult {
    data object AlreadyMigrated : LegacyRoomMigrationResult
    data object NoLegacyWorkspace : LegacyRoomMigrationResult
    data class Migrated(val sourceVersion: Int) : LegacyRoomMigrationResult
    data class Rejected(val reason: String) : LegacyRoomMigrationResult
}
