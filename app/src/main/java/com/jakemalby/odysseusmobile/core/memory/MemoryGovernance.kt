package com.jakemalby.odysseusmobile.core.memory

import com.jakemalby.odysseusmobile.core.Memory

enum class MemorySourceKind {
    MANUAL,
    MODEL_EXTRACTED,
    IMPORTED,
}

data class MemoryProvenance(
    val sourceKind: MemorySourceKind,
    val sourceLabel: String,
    val sourceReference: String? = null,
)

/** A model suggestion is deliberately not a [Memory] and cannot cross the persistence gate. */
data class ExtractedMemoryCandidate(
    val id: String,
    val text: String,
    val proposedAt: Long,
    val confidence: Double,
    val provenance: MemoryProvenance,
) {
    init {
        require(text.isNotBlank()) { "A memory candidate cannot be blank" }
        require(confidence in 0.0..1.0) { "Confidence must be between zero and one" }
        require(provenance.sourceKind == MemorySourceKind.MODEL_EXTRACTED) {
            "Extracted candidates must identify a model-extracted source"
        }
    }
}

data class GovernedMemoryRecord(
    val memory: Memory,
    val provenance: MemoryProvenance,
    val confidence: Double? = null,
    val approvedAt: Long,
    val expiresAt: Long? = null,
    val recallEnabled: Boolean = true,
    val deletedAt: Long? = null,
)

/** The only value accepted by [MemoryPersistenceGate]. */
class ApprovedMemory internal constructor(
    internal val record: GovernedMemoryRecord,
)

data class RejectedMemoryCandidate(
    val candidate: ExtractedMemoryCandidate,
    val rejectedAt: Long,
)

object MemoryGovernance {
    fun proposeExtraction(
        id: String,
        text: String,
        proposedAt: Long,
        confidence: Double,
        sourceLabel: String,
        sourceReference: String? = null,
    ): ExtractedMemoryCandidate = ExtractedMemoryCandidate(
        id = id,
        text = text.trim(),
        proposedAt = proposedAt,
        confidence = confidence,
        provenance = MemoryProvenance(
            sourceKind = MemorySourceKind.MODEL_EXTRACTED,
            sourceLabel = sourceLabel,
            sourceReference = sourceReference,
        ),
    )

    fun approve(candidate: ExtractedMemoryCandidate, approvedAt: Long): ApprovedMemory = ApprovedMemory(
        GovernedMemoryRecord(
            memory = Memory(candidate.id, candidate.text, candidate.proposedAt),
            provenance = candidate.provenance,
            confidence = candidate.confidence,
            approvedAt = approvedAt,
        ),
    )

    fun reject(candidate: ExtractedMemoryCandidate, rejectedAt: Long): RejectedMemoryCandidate =
        RejectedMemoryCandidate(candidate, rejectedAt)

    fun createManual(
        id: String,
        text: String,
        createdAt: Long,
        sourceLabel: String = "Brain",
    ): ApprovedMemory {
        require(text.isNotBlank()) { "A manual memory cannot be blank" }
        return ApprovedMemory(
            GovernedMemoryRecord(
                memory = Memory(id, text.trim(), createdAt),
                provenance = MemoryProvenance(MemorySourceKind.MANUAL, sourceLabel),
                approvedAt = createdAt,
            ),
        )
    }
}

object MemoryPersistenceGate {
    fun workspaceMemory(approved: ApprovedMemory): Memory = approved.record.memory

    fun governedRecord(approved: ApprovedMemory): GovernedMemoryRecord = approved.record
}

object MemoryRecallPolicy {
    fun eligible(
        records: List<GovernedMemoryRecord>,
        now: Long,
        recallEnabledForChat: Boolean = true,
    ): List<GovernedMemoryRecord> {
        if (!recallEnabledForChat) return emptyList()
        return records.filter { record ->
            record.deletedAt == null &&
                record.recallEnabled &&
                (record.expiresAt == null || record.expiresAt > now)
        }
    }
}
