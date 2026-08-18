package com.jakemalby.odysseusmobile.core.recipe

import com.jakemalby.odysseusmobile.capability.CapabilityCatalog
import com.jakemalby.odysseusmobile.capability.CapabilityId
import java.time.Instant

data class RecipeModelSnapshot(
    val id: String,
    val version: String,
    val capabilities: Set<RecipeModelCapability>,
) {
    init {
        require(id.isNotBlank())
        require(version.isNotBlank())
    }
}

enum class PermissionState { GRANTED, DENIED, NOT_REQUESTED }

/** Version is supplied by the central permission broker and changes when any grant changes. */
data class PermissionEvidence(
    val version: String,
    val states: Map<CapabilityId, PermissionState>,
) {
    init { require(version.isNotBlank()) }

    fun grants(capabilities: Set<CapabilityId>): Boolean =
        capabilities.all { states[it] == PermissionState.GRANTED }

    fun relevantTo(capabilities: Set<CapabilityId>): PermissionEvidence =
        copy(states = states.filterKeys { it in capabilities })
}

data class ToolVersionSnapshot(val id: String, val version: String) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9._-]*")))
        require(version.isNotBlank())
    }
}

enum class RecipeRunMode { DRY_RUN, EXECUTE, SCHEDULED }

data class RecipeRunRequest(
    val runId: String,
    val recipe: RecipeDefinition,
    val input: Map<String, String>,
    val model: RecipeModelSnapshot,
    val requestedCapabilities: Set<CapabilityId>,
    val permissionEvidence: PermissionEvidence,
    val toolVersions: List<ToolVersionSnapshot>,
    val mode: RecipeRunMode,
    val createdAt: Instant,
    val scheduleReference: String? = null,
) {
    init {
        require(runId.isNotBlank())
        require(mode == RecipeRunMode.SCHEDULED || scheduleReference == null)
        require(mode != RecipeRunMode.SCHEDULED || !scheduleReference.isNullOrBlank())
    }
}

enum class RunPreparationCode {
    INVALID_RECIPE,
    UNKNOWN_INPUT,
    MISSING_INPUT,
    INPUT_TOO_LONG,
    MODEL_CAPABILITY_MISSING,
    CAPABILITY_NOT_ALLOWED,
    TOOL_VERSION_MISSING,
}

data class RunPreparationIssue(val code: RunPreparationCode, val subject: String)

enum class RecipeRunStatus { AWAITING_APPROVAL, READY, RUNNING, SUCCEEDED, FAILED, CANCELLED }

data class RecipeIdentitySnapshot(val id: String, val version: Int)

/** Opaque reference only. Private generated content never belongs in run metadata. */
data class RecipeOutputReference(
    val id: String,
    val storageReference: String?,
    val mediaType: String?,
    val byteSize: Long?,
    val sha256: String?,
    val redactedAt: Instant? = null,
) {
    init {
        require(id.isNotBlank())
        require(redactedAt != null || !storageReference.isNullOrBlank())
        require(byteSize == null || byteSize >= 0)
        require(sha256 == null || sha256.matches(Regex("[0-9a-f]{64}")))
        if (redactedAt != null) {
            require(storageReference == null && mediaType == null && byteSize == null && sha256 == null)
        }
    }

    fun redact(at: Instant): RecipeOutputReference = copy(
        storageReference = null,
        mediaType = null,
        byteSize = null,
        sha256 = null,
        redactedAt = at,
    )
}

enum class RecipeFailureCode { MODEL_LOAD, INPUT_UNAVAILABLE, LOCAL_RUNTIME, CAPABILITY_DECLINED, UNKNOWN }

data class RecipeRunRecord(
    val runId: String,
    val recipe: RecipeIdentitySnapshot,
    val model: RecipeModelSnapshot,
    val permissionEvidence: PermissionEvidence,
    val toolVersions: List<ToolVersionSnapshot>,
    val requestedCapabilities: Set<CapabilityId>,
    val mode: RecipeRunMode,
    val status: RecipeRunStatus,
    val createdAt: Instant,
    val startedAt: Instant? = null,
    val finishedAt: Instant? = null,
    val outputReferences: List<RecipeOutputReference> = emptyList(),
    val failureCode: RecipeFailureCode? = null,
    val scheduleReference: String? = null,
) {
    init {
        require(runId.isNotBlank())
        require(toolVersions.map { it.id }.distinct().size == toolVersions.size)
        require(startedAt == null || !startedAt.isBefore(createdAt))
        require(finishedAt == null || !finishedAt.isBefore(startedAt ?: createdAt))
        require(status == RecipeRunStatus.FAILED || failureCode == null)
        require(status == RecipeRunStatus.SUCCEEDED || outputReferences.isEmpty())
    }
}

sealed interface RunPreparation {
    data class Prepared(val record: RecipeRunRecord) : RunPreparation
    data class Rejected(val issues: List<RunPreparationIssue>) : RunPreparation {
        init { require(issues.isNotEmpty()) }
    }
}

object RecipeRunPreparer {
    fun prepare(request: RecipeRunRequest): RunPreparation {
        val issues = buildList {
            if (RecipeValidator.validate(request.recipe).isNotEmpty()) {
                add(RunPreparationIssue(RunPreparationCode.INVALID_RECIPE, request.recipe.versionedId))
            }
            val schema = request.recipe.inputSchema.associateBy { it.key }
            (request.input.keys - schema.keys).sorted().forEach {
                add(RunPreparationIssue(RunPreparationCode.UNKNOWN_INPUT, it))
            }
            request.recipe.inputSchema.forEach { field ->
                val value = request.input[field.key]
                if (field.required && value.isNullOrBlank()) {
                    add(RunPreparationIssue(RunPreparationCode.MISSING_INPUT, field.key))
                } else if (value != null && value.length > field.maxLength) {
                    add(RunPreparationIssue(RunPreparationCode.INPUT_TOO_LONG, field.key))
                }
            }
            (request.recipe.modelRequirement.capabilities - request.model.capabilities).sortedBy { it.name }.forEach {
                add(RunPreparationIssue(RunPreparationCode.MODEL_CAPABILITY_MISSING, it.name))
            }
            (request.requestedCapabilities - request.recipe.capabilityAllowlist).sortedBy { it.name }.forEach {
                add(RunPreparationIssue(RunPreparationCode.CAPABILITY_NOT_ALLOWED, it.name))
            }
            val providedTools = request.toolVersions.map { it.id }.toSet()
            (request.recipe.requiredTools - providedTools).sorted().forEach {
                add(RunPreparationIssue(RunPreparationCode.TOOL_VERSION_MISSING, it))
            }
        }
        if (issues.isNotEmpty()) return RunPreparation.Rejected(issues)

        val approvalCapabilities = request.requestedCapabilities.filterTo(mutableSetOf()) {
            CapabilityCatalog.descriptor(it)?.requiresApproval == true
        }
        val recordedEvidence = request.permissionEvidence.relevantTo(request.requestedCapabilities)
        val status = if (!recordedEvidence.grants(approvalCapabilities)) {
            RecipeRunStatus.AWAITING_APPROVAL
        } else {
            RecipeRunStatus.READY
        }
        return RunPreparation.Prepared(
            RecipeRunRecord(
                runId = request.runId,
                recipe = RecipeIdentitySnapshot(request.recipe.id, request.recipe.version),
                model = request.model,
                permissionEvidence = recordedEvidence,
                toolVersions = request.toolVersions.sortedBy { it.id },
                requestedCapabilities = request.requestedCapabilities,
                mode = request.mode,
                status = status,
                createdAt = request.createdAt,
                scheduleReference = request.scheduleReference,
            ),
        )
    }
}

sealed interface RecipeRunEvent {
    data class ApprovalReviewed(val evidence: PermissionEvidence) : RecipeRunEvent
    data class Begin(
        val at: Instant,
        val activeModel: RecipeModelSnapshot,
        val activePermissions: PermissionEvidence,
    ) : RecipeRunEvent
    data class Complete(val at: Instant, val outputs: List<RecipeOutputReference>) : RecipeRunEvent
    data class Fail(val at: Instant, val code: RecipeFailureCode) : RecipeRunEvent
    data class Cancel(val at: Instant) : RecipeRunEvent
}

enum class TransitionRejection {
    INVALID_STATE,
    PERMISSION_NOT_GRANTED,
    PERMISSION_CHANGED,
    MODEL_CHANGED,
    TIME_BEFORE_PREVIOUS_EVENT,
    OUTPUT_REQUIRED,
}

sealed interface RecipeRunTransition {
    data class Applied(val record: RecipeRunRecord) : RecipeRunTransition
    data class Rejected(val reason: TransitionRejection) : RecipeRunTransition
}

object RecipeRunStateMachine {
    fun transition(record: RecipeRunRecord, event: RecipeRunEvent): RecipeRunTransition = when (event) {
        is RecipeRunEvent.ApprovalReviewed -> reviewApproval(record, event)
        is RecipeRunEvent.Begin -> begin(record, event)
        is RecipeRunEvent.Complete -> finish(record, event.at) {
            if (event.outputs.isEmpty()) return@finish RecipeRunTransition.Rejected(TransitionRejection.OUTPUT_REQUIRED)
            RecipeRunTransition.Applied(
                record.copy(status = RecipeRunStatus.SUCCEEDED, finishedAt = event.at, outputReferences = event.outputs),
            )
        }
        is RecipeRunEvent.Fail -> finish(record, event.at) {
            RecipeRunTransition.Applied(
                record.copy(status = RecipeRunStatus.FAILED, finishedAt = event.at, failureCode = event.code),
            )
        }
        is RecipeRunEvent.Cancel -> cancel(record, event.at)
    }

    private fun reviewApproval(
        record: RecipeRunRecord,
        event: RecipeRunEvent.ApprovalReviewed,
    ): RecipeRunTransition {
        if (record.status != RecipeRunStatus.AWAITING_APPROVAL) return rejectedState()
        val relevant = event.evidence.relevantTo(record.requestedCapabilities)
        val approvalCapabilities = record.requestedCapabilities.filterTo(mutableSetOf()) {
            CapabilityCatalog.descriptor(it)?.requiresApproval == true
        }
        if (!relevant.grants(approvalCapabilities)) {
            return RecipeRunTransition.Rejected(TransitionRejection.PERMISSION_NOT_GRANTED)
        }
        return RecipeRunTransition.Applied(record.copy(permissionEvidence = relevant, status = RecipeRunStatus.READY))
    }

    private fun begin(record: RecipeRunRecord, event: RecipeRunEvent.Begin): RecipeRunTransition {
        if (record.status != RecipeRunStatus.READY) return rejectedState()
        if (event.at.isBefore(record.createdAt)) return rejectedTime()
        if (event.activeModel != record.model) {
            return RecipeRunTransition.Rejected(TransitionRejection.MODEL_CHANGED)
        }
        if (event.activePermissions.relevantTo(record.requestedCapabilities) != record.permissionEvidence) {
            return RecipeRunTransition.Rejected(TransitionRejection.PERMISSION_CHANGED)
        }
        return RecipeRunTransition.Applied(record.copy(status = RecipeRunStatus.RUNNING, startedAt = event.at))
    }

    private inline fun finish(
        record: RecipeRunRecord,
        at: Instant,
        result: () -> RecipeRunTransition,
    ): RecipeRunTransition {
        if (record.status != RecipeRunStatus.RUNNING) return rejectedState()
        if (at.isBefore(record.startedAt ?: record.createdAt)) return rejectedTime()
        return result()
    }

    private fun cancel(record: RecipeRunRecord, at: Instant): RecipeRunTransition {
        if (record.status !in setOf(
                RecipeRunStatus.AWAITING_APPROVAL,
                RecipeRunStatus.READY,
                RecipeRunStatus.RUNNING,
            )
        ) return rejectedState()
        if (at.isBefore(record.startedAt ?: record.createdAt)) return rejectedTime()
        return RecipeRunTransition.Applied(record.copy(status = RecipeRunStatus.CANCELLED, finishedAt = at))
    }

    private fun rejectedState() = RecipeRunTransition.Rejected(TransitionRejection.INVALID_STATE)
    private fun rejectedTime() = RecipeRunTransition.Rejected(TransitionRejection.TIME_BEFORE_PREVIOUS_EVENT)
}

object RecipeRunHistoryRedactor {
    fun redactOutputs(record: RecipeRunRecord, at: Instant): RecipeRunRecord {
        require(record.status == RecipeRunStatus.SUCCEEDED)
        require(!at.isBefore(record.finishedAt ?: record.createdAt))
        return record.copy(outputReferences = record.outputReferences.map { it.redact(at) })
    }
}
