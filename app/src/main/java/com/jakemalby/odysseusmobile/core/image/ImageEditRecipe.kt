package com.jakemalby.odysseusmobile.core.image

import java.security.MessageDigest
import java.util.Locale

data class NormalizedCrop(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    init {
        require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f) {
            "Crop bounds must be normalized between 0 and 1"
        }
        require(left < right && top < bottom) { "Crop must have a positive area" }
    }
}

/** A non-destructive description of edits to apply to an original image. */
data class ImageEditRecipe(
    val quarterTurnsClockwise: Int = 0,
    val crop: NormalizedCrop = NormalizedCrop(),
    val brightness: Float = 0f,
    val contrast: Float = 1f,
) {
    init {
        require(quarterTurnsClockwise in 0..3) { "Quarter turns must be between 0 and 3" }
        require(brightness in -1f..1f) { "Brightness must be between -1 and 1" }
        require(contrast in 0f..2f) { "Contrast must be between 0 and 2" }
    }

    fun rotateClockwise(): ImageEditRecipe = copy(quarterTurnsClockwise = (quarterTurnsClockwise + 1) % 4)

    fun deterministicOutputName(
        originalName: String,
        sourceIdentity: String = originalName,
        extension: String = originalName.substringAfterLast('.', "jpg"),
    ): String {
        val base = originalName.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9_-]+"), "-").trim('-').ifBlank { "image" }.take(48)
        val safeExtension = extension.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "").ifBlank { "jpg" }.take(5)
        val signature = listOf(
            sourceIdentity, quarterTurnsClockwise,
            crop.left.toRawBits(), crop.top.toRawBits(), crop.right.toRawBits(), crop.bottom.toRawBits(),
            brightness.toRawBits(), contrast.toRawBits(),
        ).joinToString(":")
        val suffix = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
            .take(6).joinToString("") { "%02x".format(Locale.ROOT, it) }
        return "$base-edit-$suffix.$safeExtension"
    }
}
