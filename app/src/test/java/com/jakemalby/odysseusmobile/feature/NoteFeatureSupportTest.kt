package com.jakemalby.odysseusmobile

import com.jakemalby.odysseusmobile.core.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteFeatureSupportTest {
    private val notes = listOf(
        Note("1", "Galaxy setup", "Run the local model offline", 0),
        Note("2", "Garden", "Plant tomatoes this weekend", 0),
    )

    @Test fun `search matches all terms across title and body without case sensitivity`() {
        assertEquals(listOf(notes[0]), NoteFeatureSupport.search(notes, "GALAXY offline"))
        assertEquals(notes, NoteFeatureSupport.search(notes, "  "))
    }

    @Test fun `markdown export keeps fenced code as inert text`() {
        val exported = NoteFeatureSupport.exportMarkdown(Note("1", "Code", "```sh\necho safe\n```", 0))
        assertEquals("# Code\n\n```sh\necho safe\n```\n", exported)
    }

    @Test fun `filename is filesystem safe and bounded`() {
        val filename = NoteFeatureSupport.exportFilename(Note("1", "../ My: private / note? ", "", 0))
        assertTrue(filename.endsWith("-1970-01-01.md"))
        assertFalse(filename.contains('/'))
        assertFalse(filename.contains(':'))
        assertTrue(filename.length <= 64 + 15)
    }
}
