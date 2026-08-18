package com.jakemalby.odysseusmobile.core.skills

import com.jakemalby.odysseusmobile.capability.CapabilityId

/**
 * A deliberately non-executable skill description. A pack can describe prompts,
 * typed inputs and capability references, but cannot carry source, bytecode,
 * shell commands, arbitrary parameters, filesystem paths, or network requests.
 */
data class SkillPackManifest(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val id: String,
    val version: String,
    val displayName: String,
    val description: String,
    val publisherKeyId: String,
    val allowedCapabilities: Set<CapabilityId>,
    val recipes: List<SkillRecipe>,
) {
    companion object { const val CURRENT_FORMAT_VERSION = 1 }
}

data class SkillRecipe(
    val id: String,
    val displayName: String,
    val prompt: String,
    val inputs: List<SkillInputField> = emptyList(),
    val actions: List<SkillActionDeclaration> = emptyList(),
)

/** Only bounded scalar inputs are representable in a skill pack. */
data class SkillInputField(
    val id: String,
    val label: String,
    val type: SkillInputType,
    val required: Boolean,
    val maxLength: Int? = null,
    val choices: List<String> = emptyList(),
)

enum class SkillInputType { TEXT, INTEGER, BOOLEAN, DATE_TIME, CHOICE }

/**
 * This is an intent declaration, not an invocation payload. The capability
 * broker must still construct and approve a typed CapabilityCall at run time.
 */
data class SkillActionDeclaration(
    val id: String,
    val capability: CapabilityId,
    val rationale: String,
)

data class SignedSkillPack(
    val manifest: SkillPackManifest,
    /** Base64-encoded Ed25519 signature over SkillPackCanonicalizer.bytes(manifest). */
    val signature: String,
)

data class PermissionDiff(
    val added: Set<CapabilityId>,
    val removed: Set<CapabilityId>,
    val unchanged: Set<CapabilityId>,
) {
    val expandsScope: Boolean get() = added.isNotEmpty()

    companion object {
        fun between(old: SkillPackManifest, new: SkillPackManifest): PermissionDiff {
            val before = old.allowedCapabilities
            val after = new.allowedCapabilities
            return PermissionDiff(after - before, before - after, before intersect after)
        }
    }
}

