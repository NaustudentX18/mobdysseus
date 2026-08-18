package com.jakemalby.odysseusmobile.core.document

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentLifecycleTest {
    private val hash = "a".repeat(64)

    @Test
    fun duplicateDetectorReturnsExistingOwnerWithoutMutatingIndex() {
        var calls = 0
        val decision = DocumentDuplicateDetector.evaluate(hash) {
            calls += 1
            "document-7"
        }

        assertEquals(DuplicateDecision.Duplicate("document-7"), decision)
        assertEquals(1, calls)
        assertEquals(
            DuplicateDecision.Unique,
            DocumentDuplicateDetector.evaluate("b".repeat(64)) { null },
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateDetectorRejectsNonCanonicalHashes() {
        DocumentDuplicateDetector.evaluate("NOT-A-HASH") { null }
    }

    @Test
    fun deletionPlanMakesRetrievalImpossibleBeforeRemovingStoredContent() {
        val plan = DocumentDeletionPlanner.plan("document-7")

        assertEquals(DocumentDeletionTarget.entries, plan.steps.map { it.target })
        assertEquals("document-7", plan.documentId)
        assertEquals(setOf("document-7"), plan.steps.map { it.ownerDocumentId }.toSet())
    }
}
