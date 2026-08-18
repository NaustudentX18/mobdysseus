package com.jakemalby.odysseusmobile.core.file

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareImportPolicyTest {
    @Test fun acceptsKnownTypesAndNormalizesNames() {
        val accepted = ShareImportPolicy.validate("image/jpeg", "image/jpeg", "Holiday photo.JPEG") as ShareFileDecision.Accepted
        assertEquals("Holiday photo.jpg", accepted.safeDisplayName)
        assertEquals(SharedFileKind.IMAGE, accepted.rule.kind)
        assertEquals(32L * 1024 * 1024, accepted.rule.maximumBytes)
    }

    @Test fun aliasesAreCompatible() {
        val accepted = ShareImportPolicy.validate("text/x-markdown", "text/markdown", "plan.markdown")
        assertTrue(accepted is ShareFileDecision.Accepted)
    }

    @Test fun rejectsMimeNameMismatchAndUnsafeNames() {
        assertTrue(ShareImportPolicy.validate("image/png", "image/png", "photo.jpg") is ShareFileDecision.Rejected)
        assertTrue(ShareImportPolicy.validate("text/plain", "text/plain", "../secret.txt") is ShareFileDecision.Rejected)
        assertTrue(ShareImportPolicy.validate("application/pdf", "application/pdf", "paper.pdf") is ShareFileDecision.Rejected)
        assertTrue(ShareImportPolicy.validate("image/jpeg", "image/png", "photo.png") is ShareFileDecision.Rejected)
    }

    @Test fun sharedTextIsTrimmedAndBoundedByUtf8Bytes() {
        assertEquals("hello", ShareImportPolicy.validateSharedText("  hello  "))
        assertNull(ShareImportPolicy.validateSharedText("   "))
        assertNull(ShareImportPolicy.validateSharedText("é".repeat(ShareImportPolicy.MAX_SHARED_TEXT_BYTES)))
    }
}
