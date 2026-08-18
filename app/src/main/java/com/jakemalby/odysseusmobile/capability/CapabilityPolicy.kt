package com.jakemalby.odysseusmobile.capability

import java.time.Clock
import java.time.Instant
import java.util.UUID

/**
 * Platform-neutral MOB-003 policy contract.
 *
 * Android adapters may execute only calls admitted here.  This package does
 * not contain Android APIs, network clients, filesystem APIs, reflection, or
 * subprocess support.  That keeps the approval decision testable before an
 * adapter is allowed to cause a real side effect.
 */
enum class CapabilityId {
    READ_PRIVATE_WORKSPACE,
    CREATE_NOTE,
    CREATE_TASK,
    IMPORT_PRIVATE_FILE,
    SHARE_EXPORT,
    CAPTURE_IMAGE,
    POST_NOTIFICATION,
    READ_CALENDAR,
    WRITE_CALENDAR,
    READ_CONTACTS,
    OPEN_SAFE_URL,

    // Reserved deny-list values. They are intentionally never registered.
    SUBPROCESS,
    ARBITRARY_FILESYSTEM,
    SOCKET,
    UNRESTRICTED_FETCH,
    MCP_EXECUTION,
}

enum class RiskLevel { NONE, LOW, MODERATE, HIGH }

enum class SideEffect { NONE, LOCAL_WRITE, EXTERNAL_SHARE, DEVICE_CAPTURE, NETWORK_ACCESS }

enum class DataScope {
    NONE,
    PRIVATE_WORKSPACE,
    USER_SELECTED_FILE,
    USER_SELECTED_MEDIA,
    CALENDAR,
    CONTACTS,
    EXPORTED_CONTENT,
    URL,
}

data class CapabilityDescriptor(
    val id: CapabilityId,
    val rationale: String,
    val risk: RiskLevel,
    val sideEffect: SideEffect,
    val dataScopes: Set<DataScope>,
    val requiresRuntimePermission: Boolean,
    val requiresApproval: Boolean,
)

/** Only these descriptors can be invoked by Android adapters. */
object CapabilityCatalog {
    private val descriptors = listOf(
        CapabilityDescriptor(CapabilityId.READ_PRIVATE_WORKSPACE, "Read the app-private workspace.", RiskLevel.NONE, SideEffect.NONE, setOf(DataScope.PRIVATE_WORKSPACE), false, false),
        CapabilityDescriptor(CapabilityId.CREATE_NOTE, "Save a note in the private workspace.", RiskLevel.LOW, SideEffect.LOCAL_WRITE, setOf(DataScope.PRIVATE_WORKSPACE), false, true),
        CapabilityDescriptor(CapabilityId.CREATE_TASK, "Save a task in the private workspace.", RiskLevel.LOW, SideEffect.LOCAL_WRITE, setOf(DataScope.PRIVATE_WORKSPACE), false, true),
        CapabilityDescriptor(CapabilityId.IMPORT_PRIVATE_FILE, "Copy a user-selected file into app-private storage.", RiskLevel.MODERATE, SideEffect.LOCAL_WRITE, setOf(DataScope.USER_SELECTED_FILE), true, true),
        CapabilityDescriptor(CapabilityId.SHARE_EXPORT, "Share explicitly selected exported content through Android.", RiskLevel.HIGH, SideEffect.EXTERNAL_SHARE, setOf(DataScope.EXPORTED_CONTENT), false, true),
        CapabilityDescriptor(CapabilityId.CAPTURE_IMAGE, "Capture a new image after camera permission.", RiskLevel.MODERATE, SideEffect.DEVICE_CAPTURE, setOf(DataScope.USER_SELECTED_MEDIA), true, true),
        CapabilityDescriptor(CapabilityId.POST_NOTIFICATION, "Post a local reminder notification.", RiskLevel.LOW, SideEffect.LOCAL_WRITE, setOf(DataScope.NONE), true, true),
        CapabilityDescriptor(CapabilityId.READ_CALENDAR, "Read user-authorized calendar entries.", RiskLevel.MODERATE, SideEffect.NONE, setOf(DataScope.CALENDAR), true, true),
        CapabilityDescriptor(CapabilityId.WRITE_CALENDAR, "Create or update user-reviewed calendar entries.", RiskLevel.HIGH, SideEffect.LOCAL_WRITE, setOf(DataScope.CALENDAR), true, true),
        CapabilityDescriptor(CapabilityId.READ_CONTACTS, "Read user-selected contacts.", RiskLevel.MODERATE, SideEffect.NONE, setOf(DataScope.CONTACTS), true, true),
        CapabilityDescriptor(CapabilityId.OPEN_SAFE_URL, "Open an explicitly approved http(s) URL in Android.", RiskLevel.MODERATE, SideEffect.NETWORK_ACCESS, setOf(DataScope.URL), false, true),
    ).associateBy(CapabilityDescriptor::id)

    fun descriptor(id: CapabilityId): CapabilityDescriptor? = descriptors[id]
}

/** Non-negotiable standalone-APK deny rules, evaluated before allowlisting. */
object CapabilityDenyRules {
    private val prohibited = setOf(
        CapabilityId.SUBPROCESS,
        CapabilityId.ARBITRARY_FILESYSTEM,
        CapabilityId.SOCKET,
        CapabilityId.UNRESTRICTED_FETCH,
        CapabilityId.MCP_EXECUTION,
    )

    fun isDenied(id: CapabilityId): Boolean = id in prohibited
}

/**
 * Schema-safe action inputs.  There is no generic JSON/map payload and no
 * call type representing process, arbitrary path, socket, or fetch access.
 */
sealed interface CapabilityCall {
    val capability: CapabilityId
    val summary: String
}

data class ReadWorkspaceCall(override val summary: String = "Read private workspace") : CapabilityCall {
    override val capability = CapabilityId.READ_PRIVATE_WORKSPACE
}
data class CreateNoteCall(val title: String, val body: String) : CapabilityCall {
    override val capability = CapabilityId.CREATE_NOTE
    override val summary = "Create note: ${title.take(80)}"
}
data class CreateTaskCall(val title: String, val dueAt: Instant? = null) : CapabilityCall {
    override val capability = CapabilityId.CREATE_TASK
    override val summary = "Create task: ${title.take(80)}"
}
data class ImportPrivateFileCall(val displayName: String, val mimeType: String, val contentUri: String) : CapabilityCall {
    override val capability = CapabilityId.IMPORT_PRIVATE_FILE
    override val summary = "Import selected file: ${displayName.take(80)}"
}
data class ShareExportCall(val exportName: String, val contentUri: String) : CapabilityCall {
    override val capability = CapabilityId.SHARE_EXPORT
    override val summary = "Share export: ${exportName.take(80)}"
}
data class CaptureImageCall(val destinationLabel: String) : CapabilityCall {
    override val capability = CapabilityId.CAPTURE_IMAGE
    override val summary = "Capture image: ${destinationLabel.take(80)}"
}
data class PostNotificationCall(val title: String, val body: String) : CapabilityCall {
    override val capability = CapabilityId.POST_NOTIFICATION
    override val summary = "Post notification: ${title.take(80)}"
}
data class ReadCalendarCall(val query: String) : CapabilityCall {
    override val capability = CapabilityId.READ_CALENDAR
    override val summary = "Read calendar: ${query.take(80)}"
}
data class WriteCalendarCall(val title: String, val startsAt: Instant, val endsAt: Instant) : CapabilityCall {
    override val capability = CapabilityId.WRITE_CALENDAR
    override val summary = "Create or update calendar event: ${title.take(80)}"
}
data class ReadContactsCall(val query: String) : CapabilityCall {
    override val capability = CapabilityId.READ_CONTACTS
    override val summary = "Read contacts: ${query.take(80)}"
}
data class OpenSafeUrlCall(val url: String) : CapabilityCall {
    override val capability = CapabilityId.OPEN_SAFE_URL
    override val summary = "Open URL: ${url.take(120)}"
}

enum class ApprovalDecision { PENDING, APPROVED, DECLINED, DENIED }
enum class ExecutionState { PROPOSED, AWAITING_APPROVAL, APPROVED, DECLINED, DENIED, EXECUTING, COMPLETED, FAILED, CANCELLED }

data class PolicyRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val call: CapabilityCall,
    val createdAt: Instant,
)

data class PolicyRecord(
    val request: PolicyRequest,
    val descriptor: CapabilityDescriptor?,
    val decision: ApprovalDecision,
    val state: ExecutionState,
    val reason: String? = null,
    val updatedAt: Instant,
)

sealed interface PolicyOutcome {
    data class RequiresApproval(val record: PolicyRecord) : PolicyOutcome
    data class Allowed(val record: PolicyRecord) : PolicyOutcome
    data class Rejected(val record: PolicyRecord) : PolicyOutcome
}

/** In-memory, append-only audit ledger; MOB-002 can replace this with persistence. */
class InMemoryCapabilityLedger {
    private val records = mutableListOf<PolicyRecord>()

    fun append(record: PolicyRecord) { records += record }
    fun latest(requestId: String): PolicyRecord? = records.lastOrNull { it.request.requestId == requestId }
    fun all(): List<PolicyRecord> = records.toList()
}

/**
 * Central admission and state-transition policy. Android executors must call
 * request(), then approve(), then beginExecution() before invoking a platform
 * adapter. Calls never execute from this class.
 */
class CapabilityExecutionPolicy(
    private val ledger: InMemoryCapabilityLedger = InMemoryCapabilityLedger(),
    private val clock: Clock = Clock.systemUTC(),
) {
    fun request(call: CapabilityCall): PolicyOutcome {
        val now = clock.instant()
        val request = PolicyRequest(call = call, createdAt = now)
        val descriptor = CapabilityCatalog.descriptor(call.capability)
        val record = if (CapabilityDenyRules.isDenied(call.capability)) {
            PolicyRecord(request, null, ApprovalDecision.DENIED, ExecutionState.DENIED, "Capability is prohibited in a standalone APK.", now)
        } else if (descriptor == null) {
            PolicyRecord(request, null, ApprovalDecision.DENIED, ExecutionState.DENIED, "Capability is not allowlisted.", now)
        } else if (!isSchemaValid(call)) {
            PolicyRecord(request, descriptor, ApprovalDecision.DENIED, ExecutionState.DENIED, "Call payload is invalid.", now)
        } else if (descriptor.requiresApproval) {
            PolicyRecord(request, descriptor, ApprovalDecision.PENDING, ExecutionState.AWAITING_APPROVAL, null, now)
        } else {
            PolicyRecord(request, descriptor, ApprovalDecision.APPROVED, ExecutionState.APPROVED, null, now)
        }
        ledger.append(record)
        return when (record.state) {
            ExecutionState.AWAITING_APPROVAL -> PolicyOutcome.RequiresApproval(record)
            ExecutionState.APPROVED -> PolicyOutcome.Allowed(record)
            else -> PolicyOutcome.Rejected(record)
        }
    }

    fun approve(requestId: String, approved: Boolean): PolicyOutcome {
        val prior = ledger.latest(requestId) ?: return rejectedUnknown(requestId)
        if (prior.state != ExecutionState.AWAITING_APPROVAL) return PolicyOutcome.Rejected(prior)
        val next = prior.copy(
            decision = if (approved) ApprovalDecision.APPROVED else ApprovalDecision.DECLINED,
            state = if (approved) ExecutionState.APPROVED else ExecutionState.DECLINED,
            reason = if (approved) null else "User declined approval.",
            updatedAt = clock.instant(),
        )
        ledger.append(next)
        return if (approved) PolicyOutcome.Allowed(next) else PolicyOutcome.Rejected(next)
    }

    fun beginExecution(requestId: String): PolicyRecord? = transition(requestId, ExecutionState.APPROVED, ExecutionState.EXECUTING)
    fun complete(requestId: String): PolicyRecord? = transition(requestId, ExecutionState.EXECUTING, ExecutionState.COMPLETED)
    fun fail(requestId: String, reason: String): PolicyRecord? = transition(requestId, ExecutionState.EXECUTING, ExecutionState.FAILED, reason)
    fun cancel(requestId: String): PolicyRecord? {
        val prior = ledger.latest(requestId) ?: return null
        if (prior.state !in setOf(ExecutionState.AWAITING_APPROVAL, ExecutionState.APPROVED)) return null
        val next = prior.copy(state = ExecutionState.CANCELLED, reason = "Cancelled before execution.", updatedAt = clock.instant())
        ledger.append(next)
        return next
    }

    fun auditTrail(): List<PolicyRecord> = ledger.all()

    private fun transition(requestId: String, expected: ExecutionState, target: ExecutionState, reason: String? = null): PolicyRecord? {
        val prior = ledger.latest(requestId) ?: return null
        if (prior.state != expected) return null
        return prior.copy(state = target, reason = reason, updatedAt = clock.instant()).also(ledger::append)
    }

    private fun rejectedUnknown(requestId: String): PolicyOutcome.Rejected {
        val now = clock.instant()
        val record = PolicyRecord(PolicyRequest(requestId, ReadWorkspaceCall(), now), null, ApprovalDecision.DENIED, ExecutionState.DENIED, "Unknown request.", now)
        ledger.append(record)
        return PolicyOutcome.Rejected(record)
    }

    private fun isSchemaValid(call: CapabilityCall): Boolean = when (call) {
        is CreateNoteCall -> call.title.isNotBlank() || call.body.isNotBlank()
        is CreateTaskCall -> call.title.isNotBlank()
        is ImportPrivateFileCall -> call.displayName.isNotBlank() && call.mimeType.isNotBlank() && call.contentUri.startsWith("content:")
        is ShareExportCall -> call.exportName.isNotBlank() && call.contentUri.startsWith("content:")
        is CaptureImageCall -> call.destinationLabel.isNotBlank()
        is PostNotificationCall -> call.title.isNotBlank() || call.body.isNotBlank()
        is ReadCalendarCall -> call.query.isNotBlank()
        is WriteCalendarCall -> call.title.isNotBlank() && call.endsAt > call.startsAt
        is ReadContactsCall -> call.query.isNotBlank()
        is OpenSafeUrlCall -> call.url.startsWith("https://") || call.url.startsWith("http://")
        is ReadWorkspaceCall -> true
    }
}
