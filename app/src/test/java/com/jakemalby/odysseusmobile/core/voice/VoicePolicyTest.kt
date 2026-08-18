package com.jakemalby.odysseusmobile.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoicePolicyTest {
    @Test
    fun `dictation appends to draft for review without sending anything`() {
        assertEquals(
            "Existing draft dictated words",
            VoiceDraftPolicy.appendTranscript("  Existing draft ", " dictated words  "),
        )
    }

    @Test
    fun `blank transcripts and speech are safely ignored`() {
        assertEquals("Keep this", VoiceDraftPolicy.appendTranscript("Keep this", "  "))
        assertFalse(VoiceDraftPolicy.canSpeak("\n  "))
        assertTrue(VoiceDraftPolicy.canSpeak("Readable response"))
    }

    @Test
    fun `every dictation capability state has an explicit user label`() {
        OfflineDictationAvailability.entries.forEach { state ->
            assertTrue(state.label.isNotBlank())
        }
    }
}
