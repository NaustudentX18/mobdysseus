package com.jakemalby.odysseusmobile.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CapabilityPolicyTest {
    private val now = Instant.parse("2026-08-13T10:00:00Z")

    @Test
    fun privateReadCanExecuteWithoutApproval() {
        val policy = policy()
        val outcome = policy.request(ReadWorkspaceCall())
        assertTrue(outcome is PolicyOutcome.Allowed)
        val id = (outcome as PolicyOutcome.Allowed).record.request.requestId
        assertEquals(ExecutionState.EXECUTING, policy.beginExecution(id)?.state)
        assertEquals(ExecutionState.COMPLETED, policy.complete(id)?.state)
    }

    @Test
    fun externalSideEffectCannotExecuteBeforePhysicalApproval() {
        val policy = policy()
        val outcome = policy.request(ShareExportCall("note.md", "content://mobdysseus/note"))
        assertTrue(outcome is PolicyOutcome.RequiresApproval)
        val id = (outcome as PolicyOutcome.RequiresApproval).record.request.requestId
        assertNull(policy.beginExecution(id))
        assertTrue(policy.approve(id, true) is PolicyOutcome.Allowed)
        assertEquals(ExecutionState.EXECUTING, policy.beginExecution(id)?.state)
    }

    @Test
    fun invalidPayloadIsDeniedAndAudited() {
        val policy = policy()
        val outcome = policy.request(OpenSafeUrlCall("file:///data/private"))
        assertTrue(outcome is PolicyOutcome.Rejected)
        assertEquals(ExecutionState.DENIED, (outcome as PolicyOutcome.Rejected).record.state)
        assertEquals(1, policy.auditTrail().size)
    }

    @Test
    fun cancellationPreventsLaterExecution() {
        val policy = policy()
        val outcome = policy.request(CreateTaskCall("Check Mobdysseus")) as PolicyOutcome.RequiresApproval
        val id = outcome.record.request.requestId
        assertEquals(ExecutionState.CANCELLED, policy.cancel(id)?.state)
        assertNull(policy.beginExecution(id))
    }

    private fun policy() = CapabilityExecutionPolicy(clock = Clock.fixed(now, ZoneOffset.UTC))
}
