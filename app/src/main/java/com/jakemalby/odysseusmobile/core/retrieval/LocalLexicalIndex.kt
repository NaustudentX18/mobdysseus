package com.jakemalby.odysseusmobile.core.retrieval

import java.util.Locale
import kotlin.math.ln

/**
 * Hard limits for the in-memory retrieval index.
 *
 * Importers should split documents before calling [LocalLexicalIndex.replaceSource]. Keeping the
 * limits in this pure Kotlin layer means an unexpectedly large or hostile document cannot bypass
 * them through a future UI or persistence implementation.
 */
data class RetrievalLimits(
    val maxSources: Int = 256,
    val maxChunks: Int = 10_000,
    val maxChunksPerSource: Int = 2_000,
    val maxCorpusCharacters: Int = 8_000_000,
    val maxCorpusTokens: Int = 1_000_000,
    val maxChunkCharacters: Int = 16_000,
    val maxChunkTokens: Int = 2_000,
    val maxSourceTitleCharacters: Int = 512,
    val maxSectionCharacters: Int = 512,
    val maxQueryCharacters: Int = 1_000,
    val maxQueryTokens: Int = 64,
    val maxResults: Int = 20,
) {
    init {
        require(
            listOf(
                maxSources,
                maxChunks,
                maxChunksPerSource,
                maxCorpusCharacters,
                maxCorpusTokens,
                maxChunkCharacters,
                maxChunkTokens,
                maxSourceTitleCharacters,
                maxSectionCharacters,
                maxQueryCharacters,
                maxQueryTokens,
                maxResults,
            ).all { it > 0 },
        ) { "Retrieval limits must be positive" }
    }
}

data class RetrievalSource(
    val id: String,
    val title: String,
)

/** Offsets are character offsets in the original source, with an exclusive end offset. */
data class SourceChunk(
    val id: String,
    val text: String,
    val pageNumber: Int? = null,
    val section: String? = null,
    val startOffset: Int,
    val endOffset: Int,
)

data class Citation(
    val sourceId: String,
    val sourceTitle: String,
    val chunkId: String,
    val pageNumber: Int?,
    val section: String?,
    val startOffset: Int,
    val endOffset: Int,
) {
    /** A human-readable, deterministic identity suitable for saved chat citation references. */
    val stableId: String
        get() = buildString {
            append(sourceId)
            append(':')
            append(pageNumber ?: "-")
            append(':')
            append(section ?: "-")
            append(':')
            append(startOffset)
            append('-')
            append(endOffset)
            append(':')
            append(chunkId)
        }
}

data class RetrievalMatch(
    val text: String,
    val citation: Citation,
    val score: Double,
)

sealed interface IndexUpdateResult {
    data class Accepted(val indexedChunks: Int) : IndexUpdateResult
    data class Rejected(val reason: IndexRejection) : IndexUpdateResult
}

enum class IndexRejection {
    INVALID_SOURCE,
    INVALID_CHUNK,
    DUPLICATE_CHUNK_ID,
    SOURCE_LIMIT,
    CHUNK_LIMIT,
    CORPUS_CHARACTER_LIMIT,
    CORPUS_TOKEN_LIMIT,
}

sealed interface RetrievalResult {
    data class Evidence(val matches: List<RetrievalMatch>) : RetrievalResult
    data class NoEvidence(val reason: NoEvidenceReason) : RetrievalResult
    data class Rejected(val reason: QueryRejection) : RetrievalResult
}

enum class NoEvidenceReason { EMPTY_QUERY, EMPTY_INDEX, NO_MATCHING_TERMS }

enum class QueryRejection { QUERY_CHARACTER_LIMIT, QUERY_TOKEN_LIMIT, INVALID_RESULT_LIMIT }

/**
 * Deterministic, entirely local lexical retrieval suitable as the offline half of hybrid RAG.
 *
 * This class deliberately stores no Android objects and performs no I/O. Callers own persistence
 * and can reconstruct the index after launch by replacing each source. Public operations are
 * synchronized so indexing and chat retrieval cannot observe a partially replaced source.
 */
class LocalLexicalIndex(
    private val limits: RetrievalLimits = RetrievalLimits(),
) {
    private data class IndexedChunk(
        val text: String,
        val citation: Citation,
        val termFrequency: Map<String, Int>,
        val tokenCount: Int,
    )

    private val chunksBySource = linkedMapOf<String, List<IndexedChunk>>()

    @Synchronized
    fun replaceSource(source: RetrievalSource, chunks: List<SourceChunk>): IndexUpdateResult {
        if (!validIdentifier(source.id) || source.title.isBlank() ||
            source.title.length > limits.maxSourceTitleCharacters
        ) {
            return IndexUpdateResult.Rejected(IndexRejection.INVALID_SOURCE)
        }
        if (chunks.size > limits.maxChunksPerSource) {
            return IndexUpdateResult.Rejected(IndexRejection.CHUNK_LIMIT)
        }
        if (chunks.map { it.id }.toSet().size != chunks.size) {
            return IndexUpdateResult.Rejected(IndexRejection.DUPLICATE_CHUNK_ID)
        }

        val indexed = ArrayList<IndexedChunk>(chunks.size)
        for (chunk in chunks) {
            if (!validChunk(chunk)) {
                return IndexUpdateResult.Rejected(IndexRejection.INVALID_CHUNK)
            }
            val tokens = tokenize(chunk.text)
            if (tokens.size > limits.maxChunkTokens) {
                return IndexUpdateResult.Rejected(IndexRejection.INVALID_CHUNK)
            }
            indexed += IndexedChunk(
                text = chunk.text,
                citation = Citation(
                    sourceId = source.id,
                    sourceTitle = source.title,
                    chunkId = chunk.id,
                    pageNumber = chunk.pageNumber,
                    section = chunk.section,
                    startOffset = chunk.startOffset,
                    endOffset = chunk.endOffset,
                ),
                termFrequency = tokens.groupingBy { it }.eachCount(),
                tokenCount = tokens.size,
            )
        }

        // Calculate the complete prospective state before mutation, making replacement atomic.
        val retained = chunksBySource.filterKeys { it != source.id }.values.flatten()
        val projectedSources = chunksBySource.size + if (source.id in chunksBySource) 0 else 1
        if (projectedSources > limits.maxSources) {
            return IndexUpdateResult.Rejected(IndexRejection.SOURCE_LIMIT)
        }
        if (retained.size + indexed.size > limits.maxChunks) {
            return IndexUpdateResult.Rejected(IndexRejection.CHUNK_LIMIT)
        }
        if (retained.sumOf { it.text.length.toLong() } + indexed.sumOf { it.text.length.toLong() } >
            limits.maxCorpusCharacters.toLong()
        ) {
            return IndexUpdateResult.Rejected(IndexRejection.CORPUS_CHARACTER_LIMIT)
        }
        if (retained.sumOf { it.tokenCount.toLong() } + indexed.sumOf { it.tokenCount.toLong() } >
            limits.maxCorpusTokens.toLong()
        ) {
            return IndexUpdateResult.Rejected(IndexRejection.CORPUS_TOKEN_LIMIT)
        }

        chunksBySource[source.id] = indexed
        return IndexUpdateResult.Accepted(indexed.size)
    }

    @Synchronized
    fun deleteSource(sourceId: String): Boolean = chunksBySource.remove(sourceId) != null

    @Synchronized
    fun clear() = chunksBySource.clear()

    @Synchronized
    fun sourceCount(): Int = chunksBySource.size

    @Synchronized
    fun chunkCount(): Int = chunksBySource.values.sumOf { it.size }

    @Synchronized
    fun search(query: String, resultLimit: Int = minOf(DEFAULT_RESULT_LIMIT, limits.maxResults)): RetrievalResult {
        if (resultLimit !in 1..limits.maxResults) {
            return RetrievalResult.Rejected(QueryRejection.INVALID_RESULT_LIMIT)
        }
        if (query.length > limits.maxQueryCharacters) {
            return RetrievalResult.Rejected(QueryRejection.QUERY_CHARACTER_LIMIT)
        }
        val queryTokens = tokenize(query)
        if (queryTokens.isEmpty()) {
            return RetrievalResult.NoEvidence(NoEvidenceReason.EMPTY_QUERY)
        }
        if (queryTokens.size > limits.maxQueryTokens) {
            return RetrievalResult.Rejected(QueryRejection.QUERY_TOKEN_LIMIT)
        }
        val corpus = chunksBySource.values.flatten()
        if (corpus.isEmpty()) {
            return RetrievalResult.NoEvidence(NoEvidenceReason.EMPTY_INDEX)
        }

        val terms = queryTokens.toSet()
        val averageLength = corpus.sumOf { it.tokenCount }.toDouble() / corpus.size
        val documentFrequency = terms.associateWith { term ->
            corpus.count { term in it.termFrequency }
        }
        val matches = corpus.mapNotNull { chunk ->
            val score = bm25Score(chunk, terms, documentFrequency, corpus.size, averageLength)
            if (score <= 0.0) null else RetrievalMatch(chunk.text, chunk.citation, score)
        }.sortedWith(
            compareByDescending<RetrievalMatch> { it.score }
                .thenBy { it.citation.sourceId }
                .thenBy { it.citation.pageNumber ?: Int.MAX_VALUE }
                .thenBy { it.citation.startOffset }
                .thenBy { it.citation.chunkId },
        ).take(resultLimit)

        return if (matches.isEmpty()) {
            RetrievalResult.NoEvidence(NoEvidenceReason.NO_MATCHING_TERMS)
        } else {
            RetrievalResult.Evidence(matches)
        }
    }

    private fun validChunk(chunk: SourceChunk): Boolean =
        validIdentifier(chunk.id) &&
            chunk.text.isNotBlank() &&
            chunk.text.length <= limits.maxChunkCharacters &&
            (chunk.pageNumber == null || chunk.pageNumber > 0) &&
            (chunk.section == null || chunk.section.length <= limits.maxSectionCharacters) &&
            chunk.startOffset >= 0 &&
            chunk.endOffset > chunk.startOffset

    private fun validIdentifier(value: String): Boolean =
        value.isNotBlank() && value.length <= MAX_IDENTIFIER_CHARACTERS

    private fun bm25Score(
        chunk: IndexedChunk,
        queryTerms: Set<String>,
        documentFrequency: Map<String, Int>,
        documentCount: Int,
        averageLength: Double,
    ): Double {
        val lengthNormalization = 1.0 - BM25_B + BM25_B * (chunk.tokenCount / averageLength)
        return queryTerms.sumOf { term ->
            val frequency = chunk.termFrequency[term] ?: return@sumOf 0.0
            val documentsWithTerm = documentFrequency.getValue(term)
            val inverseDocumentFrequency = ln(
                1.0 + (documentCount - documentsWithTerm + 0.5) / (documentsWithTerm + 0.5),
            )
            inverseDocumentFrequency *
                (frequency * (BM25_K1 + 1.0)) /
                (frequency + BM25_K1 * lengthNormalization)
        }
    }

    private fun tokenize(value: String): List<String> = TOKEN_PATTERN.findAll(value)
        .map { it.value.lowercase(Locale.ROOT) }
        .toList()

    private companion object {
        const val BM25_K1 = 1.2
        const val BM25_B = 0.75
        const val DEFAULT_RESULT_LIMIT = 5
        const val MAX_IDENTIFIER_CHARACTERS = 256
        val TOKEN_PATTERN = Regex("[\\p{L}\\p{N}]+")
    }
}
