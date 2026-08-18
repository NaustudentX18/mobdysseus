package com.jakemalby.odysseusmobile.capability

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Extra MOB-003 recovery cases for the future Room-backed audit repository. */
class CapabilityStateReconstructorSafetyTest {
    private val at = Instant.parse("2026-08-13T12:00:00Z")
    private val descriptor = requireNotNull(CapabilityCatalog.descriptor(CapabilityId.CREATE_TASK))

    private fun record(
        id: String,
        state: ExecutionState,
        decision: ApprovalDecision = ApprovalDecision.APPROVED,
        updatedAt: Instant = at,
    ) = PolicyRecord(
        request = PolicyRequest(id, CreateTaskCall("Task $id"), at),
        descriptor = descriptor,
        decision = decision,
        state = state,
        updatedAt = updatedAt,
    )

    @Test
    fun finalStatesRemainHistoricalOnlyAfterRecovery() {
        val snapshot = CapabilityStateReconstructor.reconstruct(
            listOf(
                record("proposed", ExecutionState.PROPOSED, ApprovalDecision.PENDING),
                record("declined", ExecutionState.DECLINED, ApprovalDecision.DECLINED),
                record("denied", ExecutionState.DENIED, ApprovalDecision.DENIED),
                record("completed", ExecutionState.COMPLETED),
                record("failed", ExecutionState.FAILED),
                record("cancelled", ExecutionState.CANCELLED),
            ),
            at.plusSeconds(1),
        )

        assertFalse(snapshot.hasAutoExecutableAction)
        assertEquals(
            List(6) { RecoveredActionDisposition.HISTORICAL },
            snapshot.actions.map(RecoveredCapabilityAction::disposition),
        )
    }

    @Test
    fun duplicateTimestampsCannotCreateAnAutoExecutableAction() {
        // A durable database can have equal timestamp precision for adjacent
        // transitions. Regardless of which equal-time record is selected, the
        // recovery surface must force an approval/review rather than execution.
        val snapshot = CapabilityStateReconstructor.reconstruct(
            listOf(
                record("same-time", ExecutionState.AWAITING_APPROVAL, ApprovalDecision.PENDING),
                record("same-time", ExecutionState.APPROVED),
                record("same-time", ExecutionState.EXECUTING),
            ),
            at.plusSeconds(1),
        )

        assertEquals(1, snapshot.actions.size)
        assertFalse(snapshot.hasAutoExecutableAction)
        assertEquals(
            true,
            snapshot.actions.single().disposition in setOf(
                RecoveredActionDisposition.REQUIRE_APPROVAL,
                RecoveredActionDisposition.REQUIRE_REAPPROVAL,
                RecoveredActionDisposition.REQUIRE_MANUAL_REVIEW,
            ),
        )
    }

    @Test
    fun pendingActionIsRePresentedWithoutAnyExecutionState() {
        val snapshot = CapabilityStateReconstructor.reconstruct(
            listOf(record("pending", ExecutionState.AWAITING_APPROVAL, ApprovalDecision.PENDING)),
            at.plusSeconds(1),
        )

        val action = snapshot.actions.single()
        assertEquals(RecoveredActionDisposition.REQUIRE_APPROVAL, action.disposition)
        assertFalse(snapshot.hasAutoExecutableAction)
        assertEquals(ExecutionState.AWAITING_APPROVAL, action.record.state)
    }
}
