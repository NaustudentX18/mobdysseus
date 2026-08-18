package com.jakemalby.odysseusmobile.capability

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CapabilityAuditPersistenceTest {
    private val created = Instant.parse("2026-08-13T00:00:00Z")

    @Test
    fun repositoryPreservesAppendOnlyRequestHistory() {
        val repository = InMemoryCapabilityAuditRepository()
        val request = PolicyRequest("request-1", CreateTaskCall("Call Mum"), created)
        val descriptor = requireNotNull(CapabilityCatalog.descriptor(CapabilityId.CREATE_TASK))
        val pending = PolicyRecord(request, descriptor, ApprovalDecision.PENDING, ExecutionState.AWAITING_APPROVAL, updatedAt = created)
        val approved = pending.copy(decision = ApprovalDecision.APPROVED, state = ExecutionState.APPROVED, updatedAt = created.plusSeconds(1))

        repository.append(pending)
        repository.append(approved)

        assertEquals(listOf(pending, approved), repository.history("request-1"))
        assertEquals(approved, DurableCapabilityAuditLedger(repository).latest("request-1"))
    }

    @Test
    fun recoveryNeverMakesPendingOrApprovedActionsExecutable() {
        val descriptor = requireNotNull(CapabilityCatalog.descriptor(CapabilityId.CREATE_TASK))
        val pendingRequest = PolicyRequest("pending", CreateTaskCall("Pending"), created)
        val approvedRequest = PolicyRequest("approved", CreateTaskCall("Approved"), created)
        val executingRequest = PolicyRequest("executing", CreateTaskCall("Executing"), created)
        val snapshot = CapabilityStateReconstructor.reconstruct(
            listOf(
                PolicyRecord(pendingRequest, descriptor, ApprovalDecision.PENDING, ExecutionState.AWAITING_APPROVAL, updatedAt = created),
                PolicyRecord(approvedRequest, descriptor, ApprovalDecision.APPROVED, ExecutionState.APPROVED, updatedAt = created),
                PolicyRecord(executingRequest, descriptor, ApprovalDecision.APPROVED, ExecutionState.EXECUTING, updatedAt = created),
            ),
            created.plusSeconds(5),
        )

        assertFalse(snapshot.hasAutoExecutableAction)
        assertEquals(
            listOf(
                RecoveredActionDisposition.REQUIRE_APPROVAL,
                RecoveredActionDisposition.REQUIRE_REAPPROVAL,
                RecoveredActionDisposition.REQUIRE_MANUAL_REVIEW,
            ),
            snapshot.actions.map(RecoveredCapabilityAction::disposition),
        )
    }
}
