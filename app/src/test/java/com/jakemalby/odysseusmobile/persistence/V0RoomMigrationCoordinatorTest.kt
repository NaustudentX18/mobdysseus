package com.jakemalby.odysseusmobile.persistence

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V0RoomMigrationCoordinatorTest {
    @Test
    fun `retains legacy data when migration is rejected`() = runBlocking {
        val repository = FakeRepository()
        var retired = false
        val coordinator = V0RoomMigrationCoordinator(
            repository = repository,
            readLegacyPlaintext = { "{not valid json" },
            retireLegacy = { retired = true },
        )

        val result = coordinator.migrateIfNeeded()

        assertTrue(result is LegacyRoomMigrationResult.Rejected)
        assertEquals(null, repository.snapshot)
        assertFalse(retired)
    }

    @Test
    fun `retires legacy only after repository accepts typed snapshot`() = runBlocking {
        val repository = FakeRepository()
        var retired = false
        val fixture = """{"active":"c","conversations":[{"id":"c","title":"T","messages":[]}],"notes":[],"tasks":[],"memories":[],"settings":{"recipe":"Private quick chat","localOnly":true,"compact":false}}"""
        val coordinator = V0RoomMigrationCoordinator(
            repository = repository,
            readLegacyPlaintext = { fixture },
            retireLegacy = { retired = true },
        )

        assertEquals(LegacyRoomMigrationResult.Migrated(0), coordinator.migrateIfNeeded())
        assertEquals("c", repository.snapshot?.activeConversationId)
        assertTrue(retired)
    }

    private class FakeRepository : WorkspaceRepository {
        var snapshot: WorkspaceSnapshot? = null
        override suspend fun read(): WorkspaceSnapshot? = snapshot
        override suspend fun replace(snapshot: WorkspaceSnapshot) { this.snapshot = snapshot }
        override suspend fun clear() { snapshot = null }
    }
}
