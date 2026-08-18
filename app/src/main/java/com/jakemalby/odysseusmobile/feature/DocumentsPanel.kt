package com.jakemalby.odysseusmobile

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jakemalby.odysseusmobile.core.SecureWorkspaceStorage
import com.jakemalby.odysseusmobile.core.document.DocumentDuplicateDetector
import com.jakemalby.odysseusmobile.core.document.DocumentDuplicateIndex
import com.jakemalby.odysseusmobile.core.document.DocumentFormat
import com.jakemalby.odysseusmobile.core.document.DocumentIngestionResult
import com.jakemalby.odysseusmobile.core.document.DocumentIngestor
import com.jakemalby.odysseusmobile.core.document.DocumentSource
import com.jakemalby.odysseusmobile.core.document.DuplicateDecision
import com.jakemalby.odysseusmobile.core.document.ExtractedDocument
import com.jakemalby.odysseusmobile.core.document.RejectionReason
import com.jakemalby.odysseusmobile.core.retrieval.IndexUpdateResult
import com.jakemalby.odysseusmobile.core.retrieval.LocalLexicalIndex
import com.jakemalby.odysseusmobile.core.retrieval.RetrievalMatch
import com.jakemalby.odysseusmobile.core.retrieval.RetrievalResult
import com.jakemalby.odysseusmobile.core.retrieval.RetrievalSource
import com.jakemalby.odysseusmobile.core.retrieval.SourceChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

private const val MAX_DOCUMENT_BYTES = 16L * 1024L * 1024L

@Composable
internal fun DocumentsPanel() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { PrivateDocumentLibrary(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var documents by remember { mutableStateOf<List<StoredDocument>>(emptyList()) }
    var status by remember { mutableStateOf("Loading local document index…") }
    var query by remember { mutableStateOf("") }
    var searchState by remember { mutableStateOf<DocumentSearchState?>(null) }
    var deleteCandidate by remember { mutableStateOf<StoredDocument?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(store) {
        val result = withContext(Dispatchers.IO) { store.load() }
        result.onSuccess {
            documents = it
            status = if (it.isEmpty()) "No documents imported" else "${it.size} private document(s) indexed"
        }.onFailure { status = "Document index could not be opened: ${safeError(it)}" }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                status = "Validating and indexing document…"
                val result = withContext(Dispatchers.IO) { store.import(uri) }
                result.onSuccess { imported ->
                    documents = imported.documents
                    status = imported.message
                    searchState = null
                }.onFailure { status = "Import rejected: ${safeError(it)}" }
                busy = false
            }
        }
    }

    deleteCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete ${candidate.displayName}?") },
            text = { Text("This permanently deletes the private source copy, extracted text, chunks, citations, and document metadata from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteCandidate = null
                    scope.launch {
                        busy = true
                        val result = withContext(Dispatchers.IO) { store.delete(candidate.id) }
                        result.onSuccess {
                            documents = it
                            status = "Deleted ${candidate.displayName} and all derived data"
                            searchState = null
                        }.onFailure { status = "Delete failed: ${safeError(it)}" }
                        busy = false
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } },
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Document library", fontWeight = FontWeight.Bold)
            Text(
                "Import UTF-8 text, Markdown, or JSON into private app storage. Validation, chunking, indexing, and search run entirely on this phone.",
                color = Muted,
            )
            Button(
                onClick = { picker.launch(arrayOf("text/plain", "text/markdown", "text/x-markdown", "application/json")) },
                enabled = !busy,
            ) { Text(if (busy) "Working…" else "Import document") }
            if (status.isNotBlank()) Text(status, color = if (status.startsWith("Import rejected") || status.contains("failed")) MaterialTheme.colorScheme.error else Success, fontSize = 12.sp)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(1_000) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search imported documents") },
                singleLine = true,
            )
            OutlinedButton(
                onClick = {
                    scope.launch {
                        searchState = withContext(Dispatchers.Default) { store.search(documents, query) }
                    }
                },
                enabled = !busy,
            ) { Text("Search locally") }

            when (val search = searchState) {
                is DocumentSearchState.Evidence -> search.matches.forEach { match -> DocumentMatch(match) }
                is DocumentSearchState.NoEvidence -> Text("No evidence: ${search.reason}", color = Muted)
                null -> Unit
            }

            documents.forEach { document ->
                Card(colors = CardDefaults.cardColors(containerColor = PanelRaised)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(document.displayName, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${document.formatLabel} · ${humanBytes(document.sourceSizeBytes)} · ${document.chunks.size} chunk(s)", color = Muted, fontSize = 12.sp)
                            Text("SHA-256 ${document.sourceSha256}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        }
                        IconButton(onClick = { deleteCandidate = document }, enabled = !busy) {
                            Icon(Icons.Outlined.Delete, "Delete ${document.displayName}", tint = Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentMatch(match: RetrievalMatch) {
    Column(Modifier.fillMaxWidth().background(PanelRaised, RoundedCornerShape(10.dp)).padding(10.dp)) {
        Text(
            "[${match.citation.sourceTitle} · chars ${match.citation.startOffset}–${match.citation.endOffset}]",
            color = Coral,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
        Text(match.text, maxLines = 5, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        Text(match.citation.stableId, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private data class StoredChunk(
    val ordinal: Int,
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
)

private data class StoredDocument(
    val id: String,
    val displayName: String,
    val formatLabel: String,
    val sourceSha256: String,
    val sourceSizeBytes: Long,
    val importedAtEpochMillis: Long,
    val privateSourceName: String,
    val chunks: List<StoredChunk>,
)

private data class ImportOutcome(val documents: List<StoredDocument>, val message: String)

private sealed interface DocumentSearchState {
    data class Evidence(val matches: List<RetrievalMatch>) : DocumentSearchState
    data class NoEvidence(val reason: String) : DocumentSearchState
}

/** App-private source files plus a Keystore-encrypted metadata/extracted-text index. */
private class PrivateDocumentLibrary(private val context: Context) {
    private val directory = File(context.filesDir, "documents")
    private val indexFile = File(directory, "document-index.ody")
    private val ingestor = DocumentIngestor()

    fun load(): Result<List<StoredDocument>> = runCatching {
        if (!indexFile.exists()) return@runCatching emptyList()
        decode(SecureWorkspaceStorage.decrypt(indexFile.readText(Charsets.UTF_8)))
            .filter { File(directory, it.privateSourceName).isFile }
            .sortedByDescending { it.importedAtEpochMillis }
    }

    fun import(uri: Uri): Result<ImportOutcome> = runCatching {
        val metadata = sourceMetadata(uri)
        val extracted = context.contentResolver.openInputStream(uri)?.use { input ->
            ingestor.ingest(
                DocumentSource(
                    displayName = metadata.name,
                    declaredMimeType = metadata.mimeType,
                    declaredSizeBytes = metadata.sizeBytes,
                    provenanceToken = "android-saf",
                ),
                input,
            )
        } ?: error("Android could not open that document")
        val document = when (extracted) {
            is DocumentIngestionResult.Accepted -> extracted.document
            is DocumentIngestionResult.Rejected -> error(rejectionMessage(extracted.reason))
        }
        val current = load().getOrThrow()
        when (val duplicate = DocumentDuplicateDetector.evaluate(document.sourceSha256, DocumentDuplicateIndex { hash -> current.firstOrNull { it.sourceSha256 == hash }?.id })) {
            is DuplicateDecision.Duplicate -> {
                val existing = current.first { it.id == duplicate.existingDocumentId }
                return@runCatching ImportOutcome(current, "Already imported as ${existing.displayName}")
            }
            DuplicateDecision.Unique -> Unit
        }

        directory.mkdirs()
        val id = UUID.randomUUID().toString()
        val sourceName = "$id.source"
        val target = File(directory, sourceName)
        try {
            val copiedHash = copyAndHashBounded(uri, target)
            require(copiedHash == document.sourceSha256) { "The selected document changed while it was being imported" }
            val stored = document.toStored(id, sourceName)
            val updated = listOf(stored) + current
            persist(updated)
            ImportOutcome(updated, "Imported ${stored.displayName}; ${stored.chunks.size} chunk(s) indexed")
        } catch (error: Exception) {
            target.delete()
            throw error
        }
    }

    fun delete(documentId: String): Result<List<StoredDocument>> = runCatching {
        val current = load().getOrThrow()
        val document = current.firstOrNull { it.id == documentId } ?: error("Document no longer exists")
        val updated = current.filterNot { it.id == documentId }
        // Remove the encrypted derived index entry before the source, matching the lifecycle plan.
        persist(updated)
        val source = File(directory, document.privateSourceName)
        require(!source.exists() || source.delete()) { "Private source could not be deleted" }
        updated
    }

    fun search(documents: List<StoredDocument>, query: String): DocumentSearchState {
        val index = LocalLexicalIndex()
        documents.forEach { document ->
            val result = index.replaceSource(
                RetrievalSource(document.id, document.displayName),
                document.chunks.map { chunk ->
                    SourceChunk(
                        id = "${document.id}-${chunk.ordinal}",
                        text = chunk.text,
                        startOffset = chunk.startOffset,
                        endOffset = chunk.endOffset,
                    )
                },
            )
            if (result is IndexUpdateResult.Rejected) {
                return DocumentSearchState.NoEvidence("local index rejected ${document.displayName}: ${result.reason.name.lowercase()}")
            }
        }
        return when (val result = index.search(query)) {
            is RetrievalResult.Evidence -> DocumentSearchState.Evidence(result.matches)
            is RetrievalResult.NoEvidence -> DocumentSearchState.NoEvidence(result.reason.name.lowercase().replace('_', ' '))
            is RetrievalResult.Rejected -> DocumentSearchState.NoEvidence(result.reason.name.lowercase().replace('_', ' '))
        }
    }

    private fun persist(documents: List<StoredDocument>) {
        directory.mkdirs()
        val encrypted = SecureWorkspaceStorage.encrypt(encode(documents))
        val temporary = File(directory, "document-index.tmp")
        temporary.writeText(encrypted, Charsets.UTF_8)
        if (!temporary.renameTo(indexFile)) {
            indexFile.delete()
            require(temporary.renameTo(indexFile)) { "Document index could not be committed" }
        }
    }

    private fun sourceMetadata(uri: Uri): SourceMetadata {
        var name: String? = null
        var size: Long? = null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameColumn >= 0 && !cursor.isNull(nameColumn)) name = cursor.getString(nameColumn)
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn)
            }
        }
        return SourceMetadata(
            name = name?.take(512)?.ifBlank { null } ?: "imported-document.txt",
            mimeType = context.contentResolver.getType(uri),
            sizeBytes = size?.takeIf { it >= 0 },
        )
    }

    private fun copyAndHashBounded(uri: Uri, target: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(8_192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    total += count
                    require(total <= MAX_DOCUMENT_BYTES) { "Document exceeds the 16 MiB import limit" }
                    digest.update(buffer, 0, count)
                    output.write(buffer, 0, count)
                }
                output.fd.sync()
            }
        } ?: error("Android could not reopen that document")
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private data class SourceMetadata(val name: String, val mimeType: String?, val sizeBytes: Long?)
}

private fun ExtractedDocument.toStored(id: String, sourceName: String) = StoredDocument(
    id = id,
    displayName = source.displayName,
    formatLabel = when (format) {
        DocumentFormat.UTF8_TEXT -> "Text"
        DocumentFormat.MARKDOWN -> "Markdown"
        DocumentFormat.JSON -> "JSON"
    },
    sourceSha256 = sourceSha256,
    sourceSizeBytes = sourceSizeBytes,
    importedAtEpochMillis = System.currentTimeMillis(),
    privateSourceName = sourceName,
    chunks = chunks.map { StoredChunk(it.ordinal, it.startOffsetInclusive, it.endOffsetExclusive, it.text) },
)

private fun encode(documents: List<StoredDocument>): String = JSONObject().apply {
    put("version", 1)
    put("documents", JSONArray().apply {
        documents.forEach { document ->
            put(JSONObject().apply {
                put("id", document.id)
                put("displayName", document.displayName)
                put("format", document.formatLabel)
                put("sha256", document.sourceSha256)
                put("sizeBytes", document.sourceSizeBytes)
                put("importedAt", document.importedAtEpochMillis)
                put("sourceFile", document.privateSourceName)
                put("chunks", JSONArray().apply {
                    document.chunks.forEach { chunk ->
                        put(JSONObject().apply {
                            put("ordinal", chunk.ordinal)
                            put("start", chunk.startOffset)
                            put("end", chunk.endOffset)
                            put("text", chunk.text)
                        })
                    }
                })
            })
        }
    })
}.toString()

private fun decode(encoded: String): List<StoredDocument> {
    val root = JSONObject(encoded)
    require(root.getInt("version") == 1) { "Unsupported document index version" }
    val array = root.getJSONArray("documents")
    return (0 until array.length()).map { index ->
        val document = array.getJSONObject(index)
        val chunks = document.getJSONArray("chunks")
        StoredDocument(
            id = document.getString("id"),
            displayName = document.getString("displayName"),
            formatLabel = document.getString("format"),
            sourceSha256 = document.getString("sha256"),
            sourceSizeBytes = document.getLong("sizeBytes"),
            importedAtEpochMillis = document.getLong("importedAt"),
            privateSourceName = document.getString("sourceFile"),
            chunks = (0 until chunks.length()).map { chunkIndex ->
                val chunk = chunks.getJSONObject(chunkIndex)
                StoredChunk(chunk.getInt("ordinal"), chunk.getInt("start"), chunk.getInt("end"), chunk.getString("text"))
            },
        )
    }
}

private fun rejectionMessage(reason: RejectionReason): String = when (reason) {
    is RejectionReason.Oversized -> "Document exceeds the ${humanBytes(reason.maximumBytes)} import limit"
    is RejectionReason.DeclaredSizeMismatch -> "Document size changed while Android was reading it"
    RejectionReason.EmptyDocument -> "Document is empty"
    RejectionReason.MalformedUtf8 -> "Document is not valid UTF-8 text"
    RejectionReason.MalformedJson -> "JSON document is malformed"
    is RejectionReason.ConflictingType -> "File name and MIME type disagree"
    is RejectionReason.UnsupportedType -> "${reason.format.name} documents are not supported yet"
    is RejectionReason.ReadFailure -> "Document could not be read (${reason.category})"
}

private fun humanBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MiB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KiB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun safeError(error: Throwable): String = error.message?.take(180)?.ifBlank { null } ?: "unknown error"
