package com.jakemalby.odysseusmobile.core.recipe

import com.jakemalby.odysseusmobile.capability.CapabilityId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeRunTest {
    private val created = Instant.parse("2026-08-13T01:00:00Z")
    private val model = RecipeModelSnapshot("local-model", "2", setOf(RecipeModelCapability.TEXT))
    private val permission = PermissionEvidence(
        version = "permissions-1",
        states = mapOf(CapabilityId.CREATE_TASK to PermissionState.GRANTED),
    )

    @Test
    fun preparationRecordsVersionsAndRequiresApprovalWhenGrantIsAbsent() {
        val request = request(
            permissions = PermissionEvidence("permissions-0", emptyMap()),
            tools = listOf(ToolVersionSnapshot("local-search", "3")),
        )
        val prepared = RecipeRunPreparer.prepare(request) as RunPreparation.Prepared

        assertEquals(RecipeRunStatus.AWAITING_APPROVAL, prepared.record.status)
        assertEquals("deep-work@1", "${prepared.record.recipe.id}@${prepared.record.recipe.version}")
        assertEquals("2", prepared.record.model.version)
        assertEquals(listOf(ToolVersionSnapshot("local-search", "3")), prepared.record.toolVersions)
        assertEquals("permissions-0", prepared.record.permissionEvidence.version)
    }

    @Test
    fun preparationRejectsInputsModelCapabilityAndCapabilityEscalationTogether() {
        val rejected = RecipeRunPreparer.prepare(
            request(
                input = mapOf("goal" to "", "surprise" to "value"),
                selectedModel = model.copy(capabilities = setOf(RecipeModelCapability.AUDIO)),
                requestedCapabilities = setOf(CapabilityId.SHARE_EXPORT),
            ),
        ) as RunPreparation.Rejected

        assertEquals(
            setOf(
                RunPreparationCode.UNKNOWN_INPUT,
                RunPreparationCode.MISSING_INPUT,
                RunPreparationCode.MODEL_CAPABILITY_MISSING,
                RunPreparationCode.CAPABILITY_NOT_ALLOWED,
            ),
            rejected.issues.map { it.code }.toSet(),
        )
    }

    @Test
    fun beginRejectsPermissionOrModelVersionChanges() {
        val ready = preparedReady()

        assertEquals(
            TransitionRejection.PERMISSION_CHANGED,
            (RecipeRunStateMachine.transition(
                ready,
                RecipeRunEvent.Begin(created.plusSeconds(1), model, permission.copy(version = "permissions-2")),
            ) as RecipeRunTransition.Rejected).reason,
        )
        assertEquals(
            TransitionRejection.MODEL_CHANGED,
            (RecipeRunStateMachine.transition(
                ready,
                RecipeRunEvent.Begin(created.plusSeconds(1), model.copy(version = "3"), permission),
            ) as RecipeRunTransition.Rejected).reason,
        )
        assertEquals(RecipeRunStatus.READY, ready.status)
        assertNull(ready.startedAt)
    }

    @Test
    fun deterministicRunCompletesWithOpaqueOutputMetadataThenRedactsIt() {
        val ready = preparedReady()
        val startedAt = created.plusSeconds(2)
        val running = (RecipeRunStateMachine.transition(
            ready,
            RecipeRunEvent.Begin(startedAt, model, permission),
        ) as RecipeRunTransition.Applied).record
        val output = RecipeOutputReference(
            id = "output-1",
            storageReference = "private-output/output-1",
            mediaType = "text/markdown",
            byteSize = 42,
            sha256 = "a".repeat(64),
        )
        val finishedAt = startedAt.plusSeconds(3)
        val completed = (RecipeRunStateMachine.transition(
            running,
            RecipeRunEvent.Complete(finishedAt, listOf(output)),
        ) as RecipeRunTransition.Applied).record

        assertEquals(RecipeRunStatus.SUCCEEDED, completed.status)
        assertEquals(finishedAt, completed.finishedAt)
        assertEquals("private-output/output-1", completed.outputReferences.single().storageReference)

        val redactedAt = finishedAt.plusSeconds(1)
        val redacted = RecipeRunHistoryRedactor.redactOutputs(completed, redactedAt)
        assertNull(redacted.outputReferences.single().storageReference)
        assertNull(redacted.outputReferences.single().sha256)
        assertEquals(redactedAt, redacted.outputReferences.single().redactedAt)
        assertEquals(completed.recipe, redacted.recipe)
        assertEquals(completed.model, redacted.model)
    }

    @Test
    fun failAndCancelAreTerminalAndRejectFurtherTransitions() {
        val running = (RecipeRunStateMachine.transition(
            preparedReady(),
            RecipeRunEvent.Begin(created.plusSeconds(1), model, permission),
        ) as RecipeRunTransition.Applied).record
        val failed = (RecipeRunStateMachine.transition(
            running,
            RecipeRunEvent.Fail(created.plusSeconds(2), RecipeFailureCode.LOCAL_RUNTIME),
        ) as RecipeRunTransition.Applied).record

        assertEquals(RecipeRunStatus.FAILED, failed.status)
        assertEquals(RecipeFailureCode.LOCAL_RUNTIME, failed.failureCode)
        assertEquals(
            TransitionRejection.INVALID_STATE,
            (RecipeRunStateMachine.transition(
                failed,
                RecipeRunEvent.Cancel(created.plusSeconds(3)),
            ) as RecipeRunTransition.Rejected).reason,
        )

        val cancelled = (RecipeRunStateMachine.transition(
            preparedReady(),
            RecipeRunEvent.Cancel(created.plusSeconds(1)),
        ) as RecipeRunTransition.Applied).record
        assertEquals(RecipeRunStatus.CANCELLED, cancelled.status)
        assertTrue(cancelled.outputReferences.isEmpty())
    }

    private fun preparedReady(): RecipeRunRecord =
        (RecipeRunPreparer.prepare(request()) as RunPreparation.Prepared).record

    private fun request(
        input: Map<String, String> = mapOf("goal" to "Ship Mobdysseus", "context" to "Offline"),
        selectedModel: RecipeModelSnapshot = model,
        requestedCapabilities: Set<CapabilityId> = setOf(CapabilityId.CREATE_TASK),
        permissions: PermissionEvidence = permission,
        tools: List<ToolVersionSnapshot> = emptyList(),
    ) = RecipeRunRequest(
        runId = "run-1",
        recipe = BuiltInRecipes.deepWork,
        input = input,
        model = selectedModel,
        requestedCapabilities = requestedCapabilities,
        permissionEvidence = permissions,
        toolVersions = tools,
        mode = RecipeRunMode.EXECUTE,
        createdAt = created,
    )
}
