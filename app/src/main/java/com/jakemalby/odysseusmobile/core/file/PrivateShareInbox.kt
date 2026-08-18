package com.jakemalby.odysseusmobile.core.file

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.jakemalby.odysseusmobile.core.SecureWorkspaceStorage
import com.jakemalby.odysseusmobile.core.document.DocumentIngestionResult
import com.jakemalby.odysseusmobile.core.document.DocumentIngestor
import com.jakemalby.odysseusmobile.core.document.DocumentSource
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.UUID

data class StoredSharedFile(
    val displayName: String,
    val canonicalMimeType: String,
    val kind: SharedFileKind,
    val sha256: String,
    val sizeBytes: Long,
    val privatePath: String,
    val receivedAtEpochMillis: Long,
    val sourceAuthority: String,
    val duplicate: Boolean,
    val textPreview: String? = null,
)

/** Copies a transient share URI into bounded, app-private storage before its grant disappears. */
class PrivateShareInbox(private val context: Context) {
    private val inboxDirectory = File(context.filesDir, "share-inbox")
    private val metadataDirectory = File(inboxDirectory, "metadata")

    @Synchronized
    fun import(uri: Uri, declaredMimeType: String?): Result<StoredSharedFile> = runCatching {
        require(uri.scheme == "content") { "Only Android content shares are accepted." }
        val metadata = queryMetadata(uri)
        val decision = ShareImportPolicy.validate(declaredMimeType, context.contentResolver.getType(uri), metadata.name)
        val accepted = when (decision) {
            is ShareFileDecision.Accepted -> decision
            is ShareFileDecision.Rejected -> error(decision.userMessage)
        }
        metadata.size?.let { require(it in 1..accepted.rule.maximumBytes) { "The shared file is empty or too large." } }

        val destinationDirectory = when (accepted.rule.kind) {
            SharedFileKind.IMAGE -> File(context.filesDir, "gallery")
            SharedFileKind.TEXT_DOCUMENT -> File(inboxDirectory, "files")
        }.apply { require(mkdirs() || isDirectory) { "Private import storage is unavailable." } }
        require(metadataDirectory.mkdirs() || metadataDirectory.isDirectory) { "Private import metadata storage is unavailable." }

        val temporary = File(destinationDirectory, ".incoming-${UUID.randomUUID()}.part")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                DigestOutputStream(FileOutputStream(temporary), digest).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        total += count
                        require(total <= accepted.rule.maximumBytes) { "The shared file is too large." }
                        output.write(buffer, 0, count)
                    }
                }
            } ?: error("Android could not open the shared file.")
            require(total > 0) { "The shared file is empty." }
            metadata.size?.let { require(it == total) { "The shared file size changed while it was being imported." } }
            validateContents(temporary, accepted, total)

            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            val target = File(destinationDirectory, "$hash.${accepted.rule.extension}")
            val duplicate = target.isFile
            if (duplicate) temporary.delete() else require(temporary.renameTo(target)) { "The private copy could not be finalized." }
            val receivedAt = System.currentTimeMillis()
            val sourceAuthority = uri.authority?.take(100)?.replace(Regex("[^A-Za-z0-9._-]"), "_") ?: "unknown-provider"
            val textPreview = if (accepted.rule.kind == SharedFileKind.TEXT_DOCUMENT) {
                target.bufferedReader(Charsets.UTF_8).use { it.readText().take(ShareImportPolicy.MAX_SHARED_TEXT_BYTES) }
            } else null
            val stored = StoredSharedFile(
                displayName = accepted.safeDisplayName,
                canonicalMimeType = accepted.rule.canonicalMimeType,
                kind = accepted.rule.kind,
                sha256 = hash,
                sizeBytes = total,
                privatePath = target.absolutePath,
                receivedAtEpochMillis = receivedAt,
                sourceAuthority = sourceAuthority,
                duplicate = duplicate,
                textPreview = textPreview,
            )
            writeMetadata(stored)
            stored
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun validateContents(file: File, accepted: ShareFileDecision.Accepted, size: Long) {
        when (accepted.rule.kind) {
            SharedFileKind.IMAGE -> {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The shared image data is invalid." }
                require(bounds.outMimeType == accepted.rule.canonicalMimeType) { "The shared image data does not match its reported type." }
            }
            SharedFileKind.TEXT_DOCUMENT -> {
                val result = file.inputStream().use { input ->
                    DocumentIngestor().ingest(
                        DocumentSource(accepted.safeDisplayName, accepted.rule.canonicalMimeType, size, "android-share"),
                        input,
                    )
                }
                require(result is DocumentIngestionResult.Accepted) { "The shared text document is not valid UTF-8 or valid JSON." }
            }
        }
    }

    private fun writeMetadata(stored: StoredSharedFile) {
        val json = JSONObject()
            .put("displayName", stored.displayName)
            .put("mimeType", stored.canonicalMimeType)
            .put("kind", stored.kind.name)
            .put("sha256", stored.sha256)
            .put("sizeBytes", stored.sizeBytes)
            .put("privatePath", stored.privatePath)
            .put("receivedAt", stored.receivedAtEpochMillis)
            .put("sourceAuthority", stored.sourceAuthority)
            .toString()
        val target = File(metadataDirectory, "${stored.sha256}.ody")
        val temporary = File(metadataDirectory, ".${stored.sha256}-${UUID.randomUUID()}.part")
        temporary.writeText(SecureWorkspaceStorage.encrypt(json), Charsets.UTF_8)
        if (target.exists()) target.delete()
        require(temporary.renameTo(target)) { "Private import metadata could not be finalized." }
    }

    private fun queryMetadata(uri: Uri): ProviderMetadata {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use ProviderMetadata(null, null)
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            ProviderMetadata(
                name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) cursor.getString(nameIndex) else null,
                size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex).takeIf { it >= 0 } else null,
            )
        } ?: ProviderMetadata(null, null)
    }

    private data class ProviderMetadata(val name: String?, val size: Long?)
}
