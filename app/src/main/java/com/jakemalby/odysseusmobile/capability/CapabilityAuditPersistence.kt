package com.jakemalby.odysseusmobile.capability

import java.time.Instant

/**
 * Persistence boundary for the MOB-003 audit trail.
 *
 * MOB-002 owns the eventual encrypted database implementation.  This contract
 * deliberately contains policy records, not Android intents, executors, or
 * callbacks: restoring a record must never resume a side effect.
 */
interface CapabilityAuditRepository {
    /** Append an immutable state transition. Implementations must preserve order per request. */
    fun append(record: PolicyRecord)

    /** All transitions for a request, ordered from oldest to newest. */
    fun history(requestId: String): List<PolicyRecord>

    /** Full ordered audit trail, suitable for reconstruction or export. */
    fun all(): List<PolicyRecord>
}

/** Test/reference implementation; production storage is supplied by MOB-002. */
class InMemoryCapabilityAuditRepository : CapabilityAuditRepository {
    private val records = mutableListOf<PolicyRecord>()

    override fun append(record: PolicyRecord) {
        records += record
    }

    override fun history(requestId: String): List<PolicyRecord> =
        records.filter { it.request.requestId == requestId }

    override fun all(): List<PolicyRecord> = records.toList()
}

/**
 * Adapter used by a future persistent policy ledger.  It keeps append-only
 * policy records durable without granting the repository any ability to run a
 * call.  A policy integration persists every result of request/approve/etc.
 */
class DurableCapabilityAuditLedger(
    private val repository: CapabilityAuditRepository,
) {
    fun append(record: PolicyRecord) = repository.append(record)

    fun latest(requestId: String): PolicyRecord? = repository.history(requestId).lastOrNull()

    fun all(): List<PolicyRecord> = repository.all()
}

enum class RecoveredActionDisposition {
    /** Final state retained strictly as audit history. */
    HISTORICAL,

    /** Show the original proposal again; it remains unapproved. */
    REQUIRE_APPROVAL,

    /** A prior approval is stale after process death and must be reconfirmed. */
    REQUIRE_REAPPROVAL,

    /** A side effect may have started; do not retry automatically. */
    REQUIRE_MANUAL_REVIEW,
}

data class RecoveredCapabilityAction(
    val record: PolicyRecord,
    val disposition: RecoveredActionDisposition,
    val recoveryReason: String,
)

data class CapabilityRecoverySnapshot(
    val recoveredAt: Instant,
    val actions: List<RecoveredCapabilityAction>,
) {
    /** This invariant is intentionally explicit for UI and executor callers. */
    val hasAutoExecutableAction: Boolean = false
}

/**
 * Rebuilds a safe post-process-death view from append-only history.
 *
 * In particular, APPROVED and EXECUTING records never become executable on
 * restore.  The caller must obtain a fresh physical confirmation or resolve
 * the uncertain side effect manually before submitting a new policy request.
 */
object CapabilityStateReconstructor {
    fun reconstruct(records: List<PolicyRecord>, recoveredAt: Instant): CapabilityRecoverySnapshot {
        val latestRecords = records
            .groupBy { it.request.requestId }
            .values
            .mapNotNull { history -> history.maxByOrNull { it.updatedAt } }
            .sortedBy { it.updatedAt }

        val actions = latestRecords.map { record ->
            when (record.state) {
                ExecutionState.AWAITING_APPROVAL -> RecoveredCapabilityAction(
                    record,
                    RecoveredActionDisposition.REQUIRE_APPROVAL,
                    "Approval was pending when the app stopped; it has not executed.",
                )
                ExecutionState.APPROVED -> RecoveredCapabilityAction(
                    record,
                    RecoveredActionDisposition.REQUIRE_REAPPROVAL,
                    "Approval is stale after process death; confirm again before a new execution.",
                )
                ExecutionState.EXECUTING -> RecoveredCapabilityAction(
                    record,
                    RecoveredActionDisposition.REQUIRE_MANUAL_REVIEW,
                    "Execution may have begun before process death; it will not be retried automatically.",
                )
                else -> RecoveredCapabilityAction(
                    record,
                    RecoveredActionDisposition.HISTORICAL,
                    "Final policy state retained for audit only.",
                )
            }
        }
        return CapabilityRecoverySnapshot(recoveredAt, actions)
    }

    fun reconstruct(repository: CapabilityAuditRepository, recoveredAt: Instant): CapabilityRecoverySnapshot =
        reconstruct(repository.all(), recoveredAt)
}
