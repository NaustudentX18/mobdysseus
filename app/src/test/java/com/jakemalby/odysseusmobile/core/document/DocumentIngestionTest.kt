package com.jakemalby.odysseusmobile.core.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.InputStream

class DocumentIngestionTest {
    @Test
    fun extractsUtf8WithStableHashMetadataAndChunks() {
        val bytes = "abcdefghi".toByteArray()
        val result = DocumentIngestor(
            DocumentIngestionLimits(maximumSourceBytes = 100, chunkCharacters = 5, chunkOverlapCharacters = 2),
        ).ingest(
            DocumentSource("notes.txt", "text/plain", bytes.size.toLong(), "saf:opaque-token"),
            bytes.inputStream(),
        ) as DocumentIngestionResult.Accepted

        assertEquals(DocumentFormat.UTF8_TEXT, result.document.format)
        assertEquals(9L, result.document.sourceSizeBytes)
        assertEquals("19cc02f26df43cc571bc9ed7b0c4d29224a3ec229529221725ef76d021c8326f", result.document.sourceSha256)
        assertEquals(
            listOf(
                DocumentChunk(0, 0, 5, "abcde"),
                DocumentChunk(1, 3, 8, "defgh"),
                DocumentChunk(2, 6, 9, "ghi"),
            ),
            result.document.chunks,
        )
        assertEquals("saf:opaque-token", result.document.source.provenanceToken)
    }

    @Test
    fun acceptsMarkdownAndValidJsonByMimeOrExtension() {
        val markdown = accepted("readme.md", null, "# Offline\n")
        assertEquals(DocumentFormat.MARKDOWN, markdown.format)

        val json = accepted("payload", "application/json; charset=utf-8", "{\"ok\":true}")
        assertEquals(DocumentFormat.JSON, json.format)
    }

    @Test
    fun rejectsMalformedJsonAndMalformedUtf8() {
        assertEquals(
            RejectionReason.MalformedJson,
            rejected("bad.json", "application/json", "{oops".toByteArray()),
        )
        assertEquals(
            RejectionReason.MalformedUtf8,
            rejected("bad.txt", "text/plain", byteArrayOf(0xC3.toByte(), 0x28)),
        )
    }

    @Test
    fun rejectsOversizeWithoutReadingUnboundedInput() {
        val result = DocumentIngestor(
            DocumentIngestionLimits(maximumSourceBytes = 4, chunkCharacters = 4, chunkOverlapCharacters = 0),
        ).ingest(DocumentSource("large.txt", "text/plain"), "12345and-more".byteInputStream())

        assertEquals(
            DocumentIngestionResult.Rejected(RejectionReason.Oversized(4, 5)),
            result,
        )
    }

    @Test
    fun chunksNeverSplitUtf16SurrogatePairs() {
        val chunks = DeterministicDocumentChunker.chunk("ab😀cd", maximumCharacters = 3, overlapCharacters = 1)

        assertEquals("ab", chunks[0].text)
        assertTrue(chunks.all { chunk ->
            chunk.text.firstOrNull()?.let(Character::isLowSurrogate) != true &&
                chunk.text.lastOrNull()?.let(Character::isHighSurrogate) != true
        })
        assertTrue(chunks.all { it.text == "ab😀cd".substring(it.startOffsetInclusive, it.endOffsetExclusive) })
    }

    @Test
    fun rejectsKnownUnsupportedFormatsAndConflictingMetadata() {
        assertEquals(
            RejectionReason.UnsupportedType(UnsupportedDocumentFormat.PDF),
            rejected("scan.pdf", "application/pdf", "%PDF".toByteArray()),
        )
        assertEquals(
            RejectionReason.UnsupportedType(UnsupportedDocumentFormat.DOCX),
            rejected("paper.docx", null, byteArrayOf(1)),
        )
        assertEquals(
            RejectionReason.ConflictingType("application/json", "notes.txt"),
            rejected("notes.txt", "application/json", "{}".toByteArray()),
        )
    }

    @Test
    fun rejectsDeclaredSizeMismatchAndSanitizesReadFailures() {
        val mismatch = DocumentIngestor().ingest(
            DocumentSource("a.txt", "text/plain", declaredSizeBytes = 99),
            "hello".byteInputStream(),
        )
        assertEquals(
            DocumentIngestionResult.Rejected(RejectionReason.DeclaredSizeMismatch(99, 5)),
            mismatch,
        )

        val failure = DocumentIngestor().ingest(
            DocumentSource("private.txt", "text/plain"),
            object : InputStream() {
                override fun read(): Int = throw IOException("secret document content")
            },
        ) as DocumentIngestionResult.Rejected
        assertEquals(RejectionReason.ReadFailure("IOException"), failure.reason)
        assertTrue(failure.toString().contains("secret document content").not())
    }

    private fun accepted(name: String, mime: String?, text: String): ExtractedDocument =
        (DocumentIngestor().ingest(DocumentSource(name, mime), text.byteInputStream()) as
            DocumentIngestionResult.Accepted).document

    private fun rejected(name: String, mime: String?, bytes: ByteArray): RejectionReason =
        (DocumentIngestor().ingest(DocumentSource(name, mime), bytes.inputStream()) as
            DocumentIngestionResult.Rejected).reason
}
