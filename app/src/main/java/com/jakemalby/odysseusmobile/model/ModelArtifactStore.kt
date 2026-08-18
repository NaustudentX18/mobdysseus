package com.jakemalby.odysseusmobile.model

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * File-only model artifact lifecycle for app-private storage.
 *
 * A model is invisible to consumers until a complete staged artifact has the
 * catalog's exact length and SHA-256, then receives one same-directory atomic
 * move. The caller owns download policy, consent, and catalog authenticity.
 */
class ModelArtifactStore(private val directory: File) {
    init {
        require(directory.exists() || directory.mkdirs()) { "Unable to create model storage" }
        require(directory.isDirectory) { "Model storage is not a directory" }
    }

    fun activeFile(entry: ModelCatalogEntry): File = File(directory, "${entry.artifactKey}.${entry.format.extension}")

    fun stagingFile(entry: ModelCatalogEntry): File = File(directory, ".${entry.artifactKey}.${entry.format.extension}.part")

    /** Replaces only the staging file. The currently active artifact is untouched. */
    fun stage(entry: ModelCatalogEntry, source: InputStream): File {
        val staged = stagingFile(entry)
        val temporary = File(directory, ".${entry.artifactKey}.${entry.format.extension}.writing")
        temporary.delete()
        try {
            FileOutputStream(temporary).use { output ->
                source.copyTo(output)
                output.fd.sync()
            }
            moveAtomically(temporary, staged, replaceExisting = true)
            return staged
        } finally {
            temporary.delete()
        }
    }

    /** Returns a detailed, non-throwing validation result appropriate for UI. */
    fun validateStaged(entry: ModelCatalogEntry): ArtifactValidation {
        val staged = stagingFile(entry)
        if (!staged.isFile) return ArtifactValidation.Missing
        if (staged.length() != entry.byteSize) return ArtifactValidation.WrongSize(entry.byteSize, staged.length())
        return if (Sha256.matches(staged, entry.sha256)) ArtifactValidation.Valid else ArtifactValidation.HashMismatch
    }

    /**
     * Promotes only a validated staged file. Atomic move is mandatory: on a
     * filesystem that cannot provide it, leave the existing active model intact.
     */
    fun activate(entry: ModelCatalogEntry): File {
        check(validateStaged(entry) == ArtifactValidation.Valid) { "Staged model failed validation" }
        val active = activeFile(entry)
        moveAtomically(stagingFile(entry), active, replaceExisting = true)
        return active
    }

    fun discardStaging(entry: ModelCatalogEntry): Boolean = stagingFile(entry).delete()

    fun isActiveValid(entry: ModelCatalogEntry): Boolean {
        val active = activeFile(entry)
        return active.isFile && active.length() == entry.byteSize && Sha256.matches(active, entry.sha256)
    }

    private fun moveAtomically(source: File, target: File, replaceExisting: Boolean) {
        val options = if (replaceExisting) arrayOf(ATOMIC_MOVE, REPLACE_EXISTING) else arrayOf(ATOMIC_MOVE)
        try {
            Files.move(source.toPath(), target.toPath(), *options)
        } catch (error: AtomicMoveNotSupportedException) {
            throw IllegalStateException("Model storage does not support atomic activation", error)
        }
    }
}

sealed interface ArtifactValidation {
    data object Missing : ArtifactValidation
    data class WrongSize(val expectedBytes: Long, val actualBytes: Long) : ArtifactValidation
    data object HashMismatch : ArtifactValidation
    data object Valid : ArtifactValidation
}
