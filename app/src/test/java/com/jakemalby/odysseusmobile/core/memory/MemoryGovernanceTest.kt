package com.jakemalby.odysseusmobile.core.memory

import com.jakemalby.odysseusmobile.core.Memory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryGovernanceTest {
    @Test fun `extracted memory requires approval before it can cross persistence gate`() {
        val candidate = MemoryGovernance.proposeExtraction(
            id = "candidate-1",
            text = "  Prefers local models  ",
            proposedAt = 10,
            confidence = 0.82,
            sourceLabel = "Conversation",
            sourceReference = "chat-7",
        )

        // The candidate type is intentionally not accepted by MemoryPersistenceGate.
        val approved = MemoryGovernance.approve(candidate, approvedAt = 20)
        val persisted = MemoryPersistenceGate.workspaceMemory(approved)

        assertEquals(Memory("candidate-1", "Prefers local models", 10), persisted)
        assertEquals(MemorySourceKind.MODEL_EXTRACTED, MemoryPersistenceGate.governedRecord(approved).provenance.sourceKind)
    }

    @Test fun `rejection produces no persistable memory`() {
        val candidate = MemoryGovernance.proposeExtraction("c", "Do not upload", 1, 1.0, "Chat")
        val rejected = MemoryGovernance.reject(candidate, rejectedAt = 2)

        assertEquals(candidate, rejected.candidate)
        assertEquals(2, rejected.rejectedAt)
        assertTrue(RejectedMemoryCandidate::class.java.declaredFields.none { it.type == ApprovedMemory::class.java })
    }

    @Test fun `recall excludes disabled expired deleted and chat-disabled records`() {
        fun record(id: String, expiresAt: Long? = null, enabled: Boolean = true, deletedAt: Long? = null) =
            GovernedMemoryRecord(
                memory = Memory(id, id, 0),
                provenance = MemoryProvenance(MemorySourceKind.MANUAL, "Brain"),
                approvedAt = 0,
                expiresAt = expiresAt,
                recallEnabled = enabled,
                deletedAt = deletedAt,
            )
        val eligible = record("eligible", expiresAt = 101)
        val records = listOf(eligible, record("expired", expiresAt = 100), record("off", enabled = false), record("deleted", deletedAt = 9))

        assertEquals(listOf(eligible), MemoryRecallPolicy.eligible(records, now = 100))
        assertTrue(MemoryRecallPolicy.eligible(records, now = 100, recallEnabledForChat = false).isEmpty())
    }
}
