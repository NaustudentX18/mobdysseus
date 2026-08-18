package com.jakemalby.odysseusmobile.core.document

/** Read-only boundary used before persisting an imported document. */
fun interface DocumentDuplicateIndex {
    fun documentIdForSha256(sourceSha256: String): String?
}

sealed interface DuplicateDecision {
    data object Unique : DuplicateDecision
    data class Duplicate(val existingDocumentId: String) : DuplicateDecision
}

object DocumentDuplicateDetector {
    fun evaluate(sourceSha256: String, index: DocumentDuplicateIndex): DuplicateDecision {
        require(sourceSha256.matches(Regex("[0-9a-f]{64}")))
        val existingId = index.documentIdForSha256(sourceSha256)
        return if (existingId == null) DuplicateDecision.Unique else DuplicateDecision.Duplicate(existingId)
    }
}

enum class DocumentDeletionTarget {
    RETRIEVAL_INDEX_ENTRIES,
    CHUNKS,
    EXTRACTED_TEXT,
    SOURCE_BLOB,
    DOCUMENT_METADATA,
}

data class DocumentDeletionStep(
    val target: DocumentDeletionTarget,
    val ownerDocumentId: String,
)

/**
 * Persistence implementations execute this order in one transaction where possible.
 * Retrieval entries are removed first so partially completed deletion cannot be retrieved.
 */
data class DocumentDeletionPlan(
    val documentId: String,
    val steps: List<DocumentDeletionStep>,
) {
    init {
        require(documentId.isNotBlank())
        require(steps.isNotEmpty())
        require(steps.all { it.ownerDocumentId == documentId })
        require(steps.first().target == DocumentDeletionTarget.RETRIEVAL_INDEX_ENTRIES)
        require(steps.last().target == DocumentDeletionTarget.DOCUMENT_METADATA)
    }
}

object DocumentDeletionPlanner {
    fun plan(documentId: String): DocumentDeletionPlan {
        require(documentId.isNotBlank())
        return DocumentDeletionPlan(
            documentId = documentId,
            steps = DocumentDeletionTarget.entries.map { DocumentDeletionStep(it, documentId) },
        )
    }
}
