package com.jakemalby.odysseusmobile.persistence

import android.content.Context
import com.jakemalby.odysseusmobile.core.Workspace
import com.jakemalby.odysseusmobile.core.WorkspaceStore
import com.jakemalby.odysseusmobile.core.seedWorkspace
import com.jakemalby.odysseusmobile.persistence.database.RoomWorkspaceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Application-facing persistence boundary. Writes are accepted synchronously
 * from the main thread and consumed by one IO coroutine in the same order.
 */
class WorkspacePersistenceController(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val legacyStore = WorkspaceStore(appContext)
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val commands = Channel<Workspace>(Channel.UNLIMITED)
    private val mutableError = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = mutableError.asStateFlow()

    private val repository: WorkspaceRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        RoomWorkspaceRepository(appContext)
    }

    init {
        scope.launch {
            try {
                for (workspace in commands) {
                    runCatching { repository.replace(CoreWorkspaceMapper.toSnapshot(workspace)) }
                        .onSuccess { mutableError.value = null }
                        .onFailure { mutableError.value = it }
                }
            } finally {
                scope.cancel()
            }
        }
    }

    /** Loads Room, importing the old encrypted preference exactly once when required. */
    suspend fun initialize(): Workspace = withContext(ioDispatcher) {
        val migration = V0RoomMigrationCoordinator(
            repository = repository,
            readLegacyPlaintext = { legacyStore.readLegacyPlaintextOrNull() },
            retireLegacy = { legacyStore.retireLegacy() },
        ).migrateIfNeeded()
        if (migration is LegacyRoomMigrationResult.Rejected) {
            error("Legacy workspace migration was rejected: ${migration.reason}")
        }

        repository.read()?.let(CoreWorkspaceMapper::toWorkspace) ?: seedWorkspace().also {
            repository.replace(CoreWorkspaceMapper.toSnapshot(it))
        }
    }

    /** Non-blocking and order preserving; a failed write is surfaced through [error]. */
    fun enqueueSave(workspace: Workspace) {
        check(commands.trySend(workspace).isSuccess) { "Workspace persistence is closed" }
    }

    override fun close() {
        commands.close()
    }
}
