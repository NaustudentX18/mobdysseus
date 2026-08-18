package com.jakemalby.odysseusmobile.core.file

import java.util.Locale

enum class SharedFileKind { TEXT_DOCUMENT, IMAGE }

data class ShareFileRule(
    val canonicalMimeType: String,
    val kind: SharedFileKind,
    val extension: String,
    val maximumBytes: Long,
)

sealed interface ShareFileDecision {
    data class Accepted(val safeDisplayName: String, val rule: ShareFileRule) : ShareFileDecision
    data class Rejected(val userMessage: String) : ShareFileDecision
}

/** Pure validation policy applied before a shared content URI is opened. */
object ShareImportPolicy {
    const val MAX_SHARED_TEXT_BYTES = 128 * 1024
    private const val MAX_NAME_CHARACTERS = 128

    private val rules = mapOf(
        "text/plain" to ShareFileRule("text/plain", SharedFileKind.TEXT_DOCUMENT, "txt", 16L * 1024 * 1024),
        "text/markdown" to ShareFileRule("text/markdown", SharedFileKind.TEXT_DOCUMENT, "md", 16L * 1024 * 1024),
        "text/x-markdown" to ShareFileRule("text/markdown", SharedFileKind.TEXT_DOCUMENT, "md", 16L * 1024 * 1024),
        "application/json" to ShareFileRule("application/json", SharedFileKind.TEXT_DOCUMENT, "json", 16L * 1024 * 1024),
        "text/json" to ShareFileRule("application/json", SharedFileKind.TEXT_DOCUMENT, "json", 16L * 1024 * 1024),
        "image/jpeg" to ShareFileRule("image/jpeg", SharedFileKind.IMAGE, "jpg", 32L * 1024 * 1024),
        "image/png" to ShareFileRule("image/png", SharedFileKind.IMAGE, "png", 32L * 1024 * 1024),
        "image/webp" to ShareFileRule("image/webp", SharedFileKind.IMAGE, "webp", 32L * 1024 * 1024),
    )

    fun validate(declaredMimeType: String?, providerMimeType: String?, displayName: String?): ShareFileDecision {
        val declared = normalizeMime(declaredMimeType)
        val provider = normalizeMime(providerMimeType)
        if (declared != null && provider != null && !equivalentMime(declared, provider)) {
            return ShareFileDecision.Rejected("The sending app reported conflicting file types.")
        }
        val rule = rules[provider ?: declared]
            ?: return ShareFileDecision.Rejected("Only text, Markdown, JSON, JPEG, PNG, or WebP files can be shared in.")
        val rawName = displayName?.trim().orEmpty()
        if (rawName.isEmpty() || rawName.length > MAX_NAME_CHARACTERS || rawName == "." || rawName == ".." ||
            rawName.any { it == '/' || it == '\\' || it.code < 32 || it.code == 127 }
        ) {
            return ShareFileDecision.Rejected("The shared file has an unsafe or missing name.")
        }
        val expectedExtensions = when (rule.canonicalMimeType) {
            "text/plain" -> setOf("txt")
            "text/markdown" -> setOf("md", "markdown")
            "application/json" -> setOf("json")
            "image/jpeg" -> setOf("jpg", "jpeg")
            else -> setOf(rule.extension)
        }
        val suppliedExtension = rawName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (suppliedExtension.isNotEmpty() && suppliedExtension !in expectedExtensions) {
            return ShareFileDecision.Rejected("The shared file name does not match its reported type.")
        }
        val normalizedBase = rawName.substringBeforeLast('.', rawName)
            .replace(Regex("[^A-Za-z0-9 _().-]"), "_")
            .trim(' ', '.')
            .take(96)
            .ifBlank { "shared-file" }
        return ShareFileDecision.Accepted("$normalizedBase.${rule.extension}", rule)
    }

    fun validateSharedText(text: CharSequence?): String? {
        val value = text?.toString()?.trim().orEmpty()
        if (value.isEmpty()) return null
        return value.takeIf { it.toByteArray(Charsets.UTF_8).size <= MAX_SHARED_TEXT_BYTES }
    }

    private fun normalizeMime(value: String?): String? = value
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf { it.isNotEmpty() && it != "application/octet-stream" }

    private fun equivalentMime(first: String, second: String): Boolean =
        first == second || rules[first]?.canonicalMimeType == rules[second]?.canonicalMimeType
}
