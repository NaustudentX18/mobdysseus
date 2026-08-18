package com.jakemalby.odysseusmobile.core.document

import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

/** Formats whose text can be extracted entirely on-device by this module. */
enum class DocumentFormat {
    UTF8_TEXT,
    MARKDOWN,
    JSON,
}

/** Known document types that need a future, format-specific offline parser. */
enum class UnsupportedDocumentFormat {
    PDF,
    DOCX,
    OTHER,
}

data class DocumentSource(
    val displayName: String,
    val declaredMimeType: String? = null,
    val declaredSizeBytes: Long? = null,
    /** Opaque caller-owned provenance token; this module never dereferences it. */
    val provenanceToken: String? = null,
)

data class ExtractedDocument(
    val source: DocumentSource,
    val format: DocumentFormat,
    /** SHA-256 of the exact imported bytes, before BOM removal or text parsing. */
    val sourceSha256: String,
    val sourceSizeBytes: Long,
    val text: String,
    val chunks: List<DocumentChunk>,
)

data class DocumentChunk(
    val ordinal: Int,
    /** UTF-16 string offsets, matching Kotlin String.substring indices. */
    val startOffsetInclusive: Int,
    val endOffsetExclusive: Int,
    val text: String,
)

sealed interface DocumentIngestionResult {
    data class Accepted(val document: ExtractedDocument) : DocumentIngestionResult

    data class Rejected(val reason: RejectionReason) : DocumentIngestionResult
}

sealed interface RejectionReason {
    data class Oversized(val maximumBytes: Long, val observedAtLeastBytes: Long) : RejectionReason
    data class DeclaredSizeMismatch(val declaredBytes: Long, val actualBytes: Long) : RejectionReason
    data object EmptyDocument : RejectionReason
    data object MalformedUtf8 : RejectionReason
    data object MalformedJson : RejectionReason
    data class ConflictingType(val mimeType: String, val fileName: String) : RejectionReason
    data class UnsupportedType(val format: UnsupportedDocumentFormat) : RejectionReason
    data class ReadFailure(val category: String) : RejectionReason
}

data class DocumentIngestionLimits(
    val maximumSourceBytes: Long = 16L * 1024L * 1024L,
    val chunkCharacters: Int = 1_200,
    val chunkOverlapCharacters: Int = 120,
) {
    init {
        require(maximumSourceBytes in 1..Int.MAX_VALUE.toLong())
        require(chunkCharacters > 0)
        require(chunkOverlapCharacters in 0 until chunkCharacters)
    }
}

/**
 * Bounded, deterministic ingestion for formats that require no native or network parser.
 * The input is never logged and is consumed at most once.
 */
class DocumentIngestor(
    private val limits: DocumentIngestionLimits = DocumentIngestionLimits(),
) {
    fun ingest(source: DocumentSource, input: InputStream): DocumentIngestionResult {
        if (source.declaredSizeBytes != null && source.declaredSizeBytes > limits.maximumSourceBytes) {
            return DocumentIngestionResult.Rejected(
                RejectionReason.Oversized(limits.maximumSourceBytes, source.declaredSizeBytes),
            )
        }
        val classification = classify(source)
        if (classification is TypeClassification.Rejected) {
            return DocumentIngestionResult.Rejected(classification.reason)
        }

        val bytes = try {
            readBounded(input, limits.maximumSourceBytes)
        } catch (_: SourceTooLargeException) {
            return DocumentIngestionResult.Rejected(
                RejectionReason.Oversized(limits.maximumSourceBytes, limits.maximumSourceBytes + 1),
            )
        } catch (error: Exception) {
            return DocumentIngestionResult.Rejected(
                RejectionReason.ReadFailure(error::class.java.simpleName.ifBlank { "IOException" }),
            )
        }

        if (source.declaredSizeBytes != null && source.declaredSizeBytes != bytes.size.toLong()) {
            return DocumentIngestionResult.Rejected(
                RejectionReason.DeclaredSizeMismatch(source.declaredSizeBytes, bytes.size.toLong()),
            )
        }
        if (bytes.isEmpty()) return DocumentIngestionResult.Rejected(RejectionReason.EmptyDocument)

        val decoded = decodeUtf8(bytes)
            ?: return DocumentIngestionResult.Rejected(RejectionReason.MalformedUtf8)
        val text = decoded.removePrefix("\uFEFF")
        if (text.isBlank()) return DocumentIngestionResult.Rejected(RejectionReason.EmptyDocument)

        val format = (classification as TypeClassification.Supported).format
        if (format == DocumentFormat.JSON && !isValidJson(text)) {
            return DocumentIngestionResult.Rejected(RejectionReason.MalformedJson)
        }

        return DocumentIngestionResult.Accepted(
            ExtractedDocument(
                source = source,
                format = format,
                sourceSha256 = sha256(bytes),
                sourceSizeBytes = bytes.size.toLong(),
                text = text,
                chunks = DeterministicDocumentChunker.chunk(
                    text = text,
                    maximumCharacters = limits.chunkCharacters,
                    overlapCharacters = limits.chunkOverlapCharacters,
                ),
            ),
        )
    }

    private fun classify(source: DocumentSource): TypeClassification {
        val mime = source.declaredMimeType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
        val extension = source.displayName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
        val fromMime = when (mime) {
            null, "", "application/octet-stream" -> null
            "text/plain" -> TypeClassification.Supported(DocumentFormat.UTF8_TEXT)
            "text/markdown", "text/x-markdown" -> TypeClassification.Supported(DocumentFormat.MARKDOWN)
            "application/json", "text/json" -> TypeClassification.Supported(DocumentFormat.JSON)
            "application/pdf" -> TypeClassification.Rejected(
                RejectionReason.UnsupportedType(UnsupportedDocumentFormat.PDF),
            )
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ->
                TypeClassification.Rejected(
                    RejectionReason.UnsupportedType(UnsupportedDocumentFormat.DOCX),
                )
            else -> TypeClassification.Rejected(
                RejectionReason.UnsupportedType(UnsupportedDocumentFormat.OTHER),
            )
        }
        val fromExtension = when (extension) {
            "txt" -> TypeClassification.Supported(DocumentFormat.UTF8_TEXT)
            "md", "markdown" -> TypeClassification.Supported(DocumentFormat.MARKDOWN)
            "json" -> TypeClassification.Supported(DocumentFormat.JSON)
            "pdf" -> TypeClassification.Rejected(
                RejectionReason.UnsupportedType(UnsupportedDocumentFormat.PDF),
            )
            "docx" -> TypeClassification.Rejected(
                RejectionReason.UnsupportedType(UnsupportedDocumentFormat.DOCX),
            )
            else -> null
        }

        // A known binary container extension cannot be made safe by a misleading text MIME type.
        if (fromExtension is TypeClassification.Rejected) return fromExtension
        if (fromMime is TypeClassification.Supported && fromExtension is TypeClassification.Supported &&
            fromMime.format != fromExtension.format
        ) {
            return TypeClassification.Rejected(RejectionReason.ConflictingType(mime.orEmpty(), source.displayName))
        }
        return fromMime ?: fromExtension ?: TypeClassification.Rejected(
            RejectionReason.UnsupportedType(UnsupportedDocumentFormat.OTHER),
        )
    }

    private fun readBounded(input: InputStream, maximumBytes: Long): ByteArray {
        val output = ByteArrayOutputStream(minOf(maximumBytes, 8_192).toInt())
        val buffer = ByteArray(8_192)
        var total = 0L
        while (true) {
            val remainingProbeBytes = maximumBytes - total + 1
            val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remainingProbeBytes).toInt())
            if (count < 0) break
            if (count == 0) continue
            total += count
            if (total > maximumBytes) throw SourceTooLargeException()
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun decodeUtf8(bytes: ByteArray): String? = runCatching {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }.getOrNull()

    private fun isValidJson(text: String): Boolean = runCatching {
        val tokenizer = JSONTokener(text)
        tokenizer.nextValue()
        tokenizer.nextClean() == 0.toChar()
    }.getOrDefault(false)

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private sealed interface TypeClassification {
        data class Supported(val format: DocumentFormat) : TypeClassification
        data class Rejected(val reason: RejectionReason) : TypeClassification
    }

    private class SourceTooLargeException : Exception()
}

object DeterministicDocumentChunker {
    fun chunk(text: String, maximumCharacters: Int, overlapCharacters: Int): List<DocumentChunk> {
        require(maximumCharacters > 0)
        require(overlapCharacters in 0 until maximumCharacters)
        if (text.isEmpty()) return emptyList()

        val result = mutableListOf<DocumentChunk>()
        val step = maximumCharacters - overlapCharacters
        var start = 0
        var ordinal = 0
        while (start < text.length) {
            var end = minOf(text.length, start + maximumCharacters)
            if (end < text.length && Character.isHighSurrogate(text[end - 1]) &&
                Character.isLowSurrogate(text[end])
            ) {
                end = if (end - start == 1) end + 1 else end - 1
            }
            result += DocumentChunk(ordinal++, start, end, text.substring(start, end))
            if (end == text.length) break
            start = maxOf(start + 1, end - overlapCharacters)
            if (start < text.length && start > 0 && Character.isLowSurrogate(text[start]) &&
                Character.isHighSurrogate(text[start - 1])
            ) {
                start += 1
            }
        }
        return result
    }
}
