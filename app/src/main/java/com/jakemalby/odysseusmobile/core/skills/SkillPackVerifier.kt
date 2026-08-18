package com.jakemalby.odysseusmobile.core.skills

import com.jakemalby.odysseusmobile.capability.CapabilityCatalog
import com.jakemalby.odysseusmobile.capability.CapabilityDenyRules
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

sealed interface SkillPackVerification {
    data class Accepted(val pack: VerifiedSkillPack) : SkillPackVerification
    data class Rejected(val errors: List<SkillPackError>) : SkillPackVerification
}

data class SkillPackError(val code: SkillPackErrorCode, val location: String, val detail: String)

enum class SkillPackErrorCode {
    UNSIGNED,
    UNTRUSTED_PUBLISHER,
    INVALID_SIGNATURE,
    MALFORMED,
    UNSUPPORTED_FORMAT,
    OVERBROAD_CAPABILITIES,
}

class VerifiedSkillPack private constructor(val signedPack: SignedSkillPack) {
    val manifest: SkillPackManifest get() = signedPack.manifest

    companion object {
        internal fun accepted(pack: SignedSkillPack) = VerifiedSkillPack(pack)
    }
}

/** Trust is supplied by the caller; packs can never add their own trust roots. */
class SkillPackVerifier(private val trustedPublicKeys: Map<String, PublicKey>) {
    fun verify(pack: SignedSkillPack): SkillPackVerification {
        val errors = validate(pack.manifest).toMutableList()
        if (pack.signature.isBlank()) {
            errors += SkillPackError(SkillPackErrorCode.UNSIGNED, "signature", "A detached Ed25519 signature is required.")
            return SkillPackVerification.Rejected(errors)
        }
        val key = trustedPublicKeys[pack.manifest.publisherKeyId]
        if (key == null) {
            errors += SkillPackError(SkillPackErrorCode.UNTRUSTED_PUBLISHER, "publisherKeyId", "Publisher key is not trusted.")
            return SkillPackVerification.Rejected(errors)
        }
        val signatureBytes = runCatching { Base64.getDecoder().decode(pack.signature) }.getOrNull()
        if (signatureBytes == null || signatureBytes.size != ED25519_SIGNATURE_BYTES) {
            errors += SkillPackError(SkillPackErrorCode.INVALID_SIGNATURE, "signature", "Signature is not valid Base64 Ed25519 data.")
            return SkillPackVerification.Rejected(errors)
        }
        if (errors.isNotEmpty()) return SkillPackVerification.Rejected(errors)

        val valid = runCatching {
            require(key.algorithm.equals("Ed25519", ignoreCase = true) || key.algorithm.equals("EdDSA", ignoreCase = true))
            Signature.getInstance("Ed25519").run {
                initVerify(key)
                update(SkillPackCanonicalizer.bytes(pack.manifest))
                verify(signatureBytes)
            }
        }.getOrDefault(false)
        return if (valid) SkillPackVerification.Accepted(VerifiedSkillPack.accepted(pack))
        else SkillPackVerification.Rejected(listOf(SkillPackError(SkillPackErrorCode.INVALID_SIGNATURE, "signature", "Signature verification failed.")))
    }

    private fun validate(manifest: SkillPackManifest): List<SkillPackError> {
        val errors = mutableListOf<SkillPackError>()
        fun malformed(location: String, detail: String) {
            errors += SkillPackError(SkillPackErrorCode.MALFORMED, location, detail)
        }

        if (manifest.formatVersion != SkillPackManifest.CURRENT_FORMAT_VERSION) {
            errors += SkillPackError(SkillPackErrorCode.UNSUPPORTED_FORMAT, "formatVersion", "Unsupported skill-pack format.")
        }
        if (!PACK_ID.matches(manifest.id)) malformed("id", "Use a lowercase reverse-domain identifier.")
        if (!SEMVER.matches(manifest.version)) malformed("version", "Use semantic versioning.")
        if (!TOKEN.matches(manifest.publisherKeyId)) malformed("publisherKeyId", "Publisher key identifier is invalid.")
        if (!bounded(manifest.displayName, 1, 80)) malformed("displayName", "Display name must contain 1-80 safe characters.")
        if (!bounded(manifest.description, 1, 500)) malformed("description", "Description must contain 1-500 safe characters.")
        if (manifest.recipes.isEmpty() || manifest.recipes.size > MAX_RECIPES) malformed("recipes", "A pack must contain 1-$MAX_RECIPES recipes.")
        if (manifest.recipes.map { it.id }.distinct().size != manifest.recipes.size) malformed("recipes", "Recipe identifiers must be unique.")

        val prohibited = manifest.allowedCapabilities.filter { CapabilityDenyRules.isDenied(it) || CapabilityCatalog.descriptor(it) == null }
        if (prohibited.isNotEmpty()) {
            errors += SkillPackError(SkillPackErrorCode.OVERBROAD_CAPABILITIES, "allowedCapabilities", "Capabilities are not in the mobile allowlist: ${prohibited.joinToString { it.name }}")
        }

        manifest.recipes.forEachIndexed { recipeIndex, recipe ->
            val path = "recipes[$recipeIndex]"
            if (!TOKEN.matches(recipe.id)) malformed("$path.id", "Recipe identifier is invalid.")
            if (!bounded(recipe.displayName, 1, 80)) malformed("$path.displayName", "Recipe display name is invalid.")
            if (!bounded(recipe.prompt, 1, MAX_PROMPT_LENGTH)) malformed("$path.prompt", "Prompt is empty or too large.")
            if (recipe.inputs.size > MAX_INPUTS || recipe.inputs.map { it.id }.distinct().size != recipe.inputs.size) malformed("$path.inputs", "Input schema is too large or has duplicate identifiers.")
            recipe.inputs.forEachIndexed { inputIndex, input ->
                val inputPath = "$path.inputs[$inputIndex]"
                if (!TOKEN.matches(input.id) || !bounded(input.label, 1, 80)) malformed(inputPath, "Input identifier or label is invalid.")
                if (input.maxLength != null && input.maxLength !in 1..MAX_INPUT_LENGTH) malformed("$inputPath.maxLength", "Input length bound is invalid.")
                if (input.type == SkillInputType.TEXT && input.maxLength == null) malformed("$inputPath.maxLength", "Text input requires a length bound.")
                if (input.type == SkillInputType.CHOICE && (input.choices.isEmpty() || input.choices.size > MAX_CHOICES || input.choices.any { !bounded(it, 1, 100) })) malformed("$inputPath.choices", "Choice input requires a bounded choice list.")
                if (input.type != SkillInputType.CHOICE && input.choices.isNotEmpty()) malformed("$inputPath.choices", "Only choice inputs can declare choices.")
            }
            if (recipe.actions.size > MAX_ACTIONS || recipe.actions.map { it.id }.distinct().size != recipe.actions.size) malformed("$path.actions", "Action list is too large or has duplicate identifiers.")
            recipe.actions.forEachIndexed { actionIndex, action ->
                val actionPath = "$path.actions[$actionIndex]"
                if (!TOKEN.matches(action.id) || !bounded(action.rationale, 1, 240)) malformed(actionPath, "Action identifier or rationale is invalid.")
                if (action.capability !in manifest.allowedCapabilities || CapabilityCatalog.descriptor(action.capability) == null || CapabilityDenyRules.isDenied(action.capability)) {
                    errors += SkillPackError(SkillPackErrorCode.OVERBROAD_CAPABILITIES, "$actionPath.capability", "Action capability is not allowlisted by this pack and Mobdysseus.")
                }
            }
        }

        val used = manifest.recipes.flatMap { recipe -> recipe.actions.map { it.capability } }.toSet()
        if (manifest.allowedCapabilities != used) {
            errors += SkillPackError(SkillPackErrorCode.OVERBROAD_CAPABILITIES, "allowedCapabilities", "Declared capabilities must exactly match those used by actions.")
        }
        if (runCatching { SkillPackCanonicalizer.bytes(manifest).size }.getOrDefault(Int.MAX_VALUE) > MAX_CANONICAL_BYTES) malformed("manifest", "Manifest exceeds the size limit.")
        return errors
    }

    private fun bounded(value: String, minimum: Int, maximum: Int): Boolean =
        value.length in minimum..maximum && value.none { it.code < 0x20 && it != '\n' && it != '\t' }

    private companion object {
        val PACK_ID = Regex("^[a-z0-9]+(?:[.-][a-z0-9]+){1,7}$")
        val TOKEN = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,79}$")
        val SEMVER = Regex("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-[0-9A-Za-z.-]+)?$")
        const val ED25519_SIGNATURE_BYTES = 64
        const val MAX_RECIPES = 64
        const val MAX_INPUTS = 32
        const val MAX_ACTIONS = 32
        const val MAX_CHOICES = 100
        const val MAX_INPUT_LENGTH = 16_384
        const val MAX_PROMPT_LENGTH = 32_768
        const val MAX_CANONICAL_BYTES = 512 * 1024
    }
}

