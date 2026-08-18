package com.jakemalby.odysseusmobile.core.retrieval

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLexicalIndexTest {
    @Test
    fun `fixed corpus ranks relevant chunk and preserves exact citation`() {
        val index = LocalLexicalIndex()
        index.replaceSource(
            RetrievalSource("voyage-log", "Voyage log"),
            listOf(
                SourceChunk("p1-a", "The ship left Ithaca before dawn.", 1, "Departure", 0, 34),
                SourceChunk("p7-b", "Cyclops kept the crew inside a stone cave.", 7, "The cave", 812, 854),
            ),
        )
        index.replaceSource(
            RetrievalSource("kitchen", "Kitchen notes"),
            listOf(SourceChunk("recipe", "Bake bread until golden.", 2, "Bread", 40, 64)),
        )

        val result = index.search("Where did the Cyclops keep the crew?") as RetrievalResult.Evidence

        assertEquals("p7-b", result.matches.first().citation.chunkId)
        assertEquals("voyage-log:7:The cave:812-854:p7-b", result.matches.first().citation.stableId)
        assertEquals(7, result.matches.first().citation.pageNumber)
        assertEquals("The cave", result.matches.first().citation.section)
        assertEquals(812, result.matches.first().citation.startOffset)
        assertEquals(854, result.matches.first().citation.endOffset)
    }

    @Test
    fun `same corpus and query produce stable rank order`() {
        fun createIndex() = LocalLexicalIndex().apply {
            replaceSource(
                RetrievalSource("b", "B"),
                listOf(SourceChunk("2", "olive tree", null, null, 20, 30)),
            )
            replaceSource(
                RetrievalSource("a", "A"),
                listOf(SourceChunk("1", "olive tree", null, null, 10, 20)),
            )
        }

        val first = (createIndex().search("olive") as RetrievalResult.Evidence)
            .matches.map { it.citation.stableId }
        val second = (createIndex().search("olive") as RetrievalResult.Evidence)
            .matches.map { it.citation.stableId }

        assertEquals(listOf("a:-:-:10-20:1", "b:-:-:20-30:2"), first)
        assertEquals(first, second)
    }

    @Test
    fun `no evidence is explicit for empty index empty query and unmatched terms`() {
        val index = LocalLexicalIndex()
        assertEquals(
            RetrievalResult.NoEvidence(NoEvidenceReason.EMPTY_INDEX),
            index.search("cyclops"),
        )
        assertEquals(
            RetrievalResult.NoEvidence(NoEvidenceReason.EMPTY_QUERY),
            index.search("  !!!  "),
        )
        index.replaceSource(
            RetrievalSource("source", "Source"),
            listOf(SourceChunk("chunk", "olive tree", 1, null, 0, 10)),
        )
        assertEquals(
            RetrievalResult.NoEvidence(NoEvidenceReason.NO_MATCHING_TERMS),
            index.search("spaceship"),
        )
    }

    @Test
    fun `deleting source removes every result from that source`() {
        val index = LocalLexicalIndex()
        index.replaceSource(
            RetrievalSource("secret", "Secret"),
            listOf(SourceChunk("one", "hidden passphrase", 1, null, 0, 17)),
        )
        assertTrue(index.search("passphrase") is RetrievalResult.Evidence)

        assertTrue(index.deleteSource("secret"))
        assertFalse(index.deleteSource("secret"))
        assertEquals(0, index.chunkCount())
        assertEquals(
            RetrievalResult.NoEvidence(NoEvidenceReason.EMPTY_INDEX),
            index.search("passphrase"),
        )
    }

    @Test
    fun `source replacement is atomic when new content exceeds a bound`() {
        val limits = RetrievalLimits(maxChunksPerSource = 1, maxChunks = 2)
        val index = LocalLexicalIndex(limits)
        assertEquals(
            IndexUpdateResult.Accepted(1),
            index.replaceSource(
                RetrievalSource("log", "Log"),
                listOf(SourceChunk("old", "original evidence", 1, null, 0, 17)),
            ),
        )

        val rejected = index.replaceSource(
            RetrievalSource("log", "Log"),
            listOf(
                SourceChunk("new-1", "replacement one", 2, null, 20, 35),
                SourceChunk("new-2", "replacement two", 3, null, 40, 55),
            ),
        )

        assertEquals(IndexUpdateResult.Rejected(IndexRejection.CHUNK_LIMIT), rejected)
        val surviving = index.search("original") as RetrievalResult.Evidence
        assertEquals("old", surviving.matches.single().citation.chunkId)
        assertEquals(RetrievalResult.NoEvidence(NoEvidenceReason.NO_MATCHING_TERMS), index.search("replacement"))
    }

    @Test
    fun `corpus and query limits reject excess without truncation`() {
        val limits = RetrievalLimits(
            maxCorpusCharacters = 12,
            maxCorpusTokens = 3,
            maxChunkCharacters = 20,
            maxChunkTokens = 3,
            maxQueryCharacters = 8,
            maxQueryTokens = 2,
            maxResults = 2,
        )
        val index = LocalLexicalIndex(limits)

        assertEquals(
            IndexUpdateResult.Rejected(IndexRejection.CORPUS_CHARACTER_LIMIT),
            index.replaceSource(
                RetrievalSource("long", "Long"),
                listOf(SourceChunk("chunk", "thirteen chars", 1, null, 0, 13)),
            ),
        )
        assertEquals(
            RetrievalResult.Rejected(QueryRejection.QUERY_CHARACTER_LIMIT),
            index.search("123456789"),
        )
        assertEquals(
            RetrievalResult.Rejected(QueryRejection.QUERY_TOKEN_LIMIT),
            index.search("a b c"),
        )
        assertEquals(
            RetrievalResult.Rejected(QueryRejection.INVALID_RESULT_LIMIT),
            index.search("a", resultLimit = 3),
        )
    }

    @Test
    fun `invalid citation metadata and duplicate chunk ids are rejected`() {
        val index = LocalLexicalIndex()
        assertEquals(
            IndexUpdateResult.Rejected(IndexRejection.INVALID_CHUNK),
            index.replaceSource(
                RetrievalSource("source", "Source"),
                listOf(SourceChunk("chunk", "text", 0, null, 10, 8)),
            ),
        )
        assertEquals(
            IndexUpdateResult.Rejected(IndexRejection.DUPLICATE_CHUNK_ID),
            index.replaceSource(
                RetrievalSource("source", "Source"),
                listOf(
                    SourceChunk("same", "first", 1, null, 0, 5),
                    SourceChunk("same", "second", 1, null, 6, 12),
                ),
            ),
        )
        assertEquals(0, index.sourceCount())
    }
}
