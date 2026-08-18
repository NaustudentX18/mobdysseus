package com.jakemalby.odysseusmobile.core.skills

import com.jakemalby.odysseusmobile.capability.CapabilityId
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64

class SkillPackVerifierTest {
    private val trusted = keyPair()
    private val verifier = SkillPackVerifier(mapOf("trusted-key" to trusted.public))

    @Test
    fun acceptsAuthenticBoundedDeclarativePack() {
        val result = verifier.verify(signed(manifest(), trusted))
        assertTrue(result is SkillPackVerification.Accepted)
    }

    @Test
    fun rejectsUnsignedUntrustedAndTamperedPacks() {
        val unsigned = verifier.verify(SignedSkillPack(manifest(), "")) as SkillPackVerification.Rejected
        assertTrue(unsigned.errors.any { it.code == SkillPackErrorCode.UNSIGNED })

        val stranger = keyPair()
        val untrustedManifest = manifest().copy(publisherKeyId = "stranger-key")
        val untrusted = verifier.verify(signed(untrustedManifest, stranger)) as SkillPackVerification.Rejected
        assertTrue(untrusted.errors.any { it.code == SkillPackErrorCode.UNTRUSTED_PUBLISHER })

        val authentic = signed(manifest(), trusted)
        val tampered = authentic.copy(manifest = authentic.manifest.copy(displayName = "Tampered"))
        val rejected = verifier.verify(tampered) as SkillPackVerification.Rejected
        assertEquals(listOf(SkillPackErrorCode.INVALID_SIGNATURE), rejected.errors.map { it.code })
    }

    @Test
    fun rejectsMalformedAndOverbroadPacksEvenWhenSigned() {
        val malformed = manifest().copy(
            id = "../escape",
            recipes = listOf(
                recipe().copy(
                    prompt = "",
                    inputs = listOf(SkillInputField("free", "Free", SkillInputType.TEXT, true)),
                ),
            ),
        )
        val malformedResult = verifier.verify(signed(malformed, trusted)) as SkillPackVerification.Rejected
        assertTrue(malformedResult.errors.any { it.code == SkillPackErrorCode.MALFORMED })

        val overbroad = manifest().copy(
            allowedCapabilities = setOf(CapabilityId.SUBPROCESS),
            recipes = listOf(recipe().copy(actions = listOf(SkillActionDeclaration("run", CapabilityId.SUBPROCESS, "Run a process")))),
        )
        val overbroadResult = verifier.verify(signed(overbroad, trusted)) as SkillPackVerification.Rejected
        assertTrue(overbroadResult.errors.any { it.code == SkillPackErrorCode.OVERBROAD_CAPABILITIES })
    }

    @Test
    fun rejectsUnusedGrantAndActionOutsidePackAllowlist() {
        val unused = manifest().copy(allowedCapabilities = setOf(CapabilityId.CREATE_NOTE, CapabilityId.READ_CALENDAR))
        val unusedResult = verifier.verify(signed(unused, trusted)) as SkillPackVerification.Rejected
        assertTrue(unusedResult.errors.any { it.location == "allowedCapabilities" && it.code == SkillPackErrorCode.OVERBROAD_CAPABILITIES })

        val outside = manifest().copy(allowedCapabilities = emptySet())
        val outsideResult = verifier.verify(signed(outside, trusted)) as SkillPackVerification.Rejected
        assertTrue(outsideResult.errors.any { it.location.endsWith(".capability") })
    }

    @Test
    fun canonicalEncodingIsIndependentOfSetAndDeclarationOrder() {
        val first = manifest().copy(
            allowedCapabilities = linkedSetOf(CapabilityId.READ_CALENDAR, CapabilityId.CREATE_NOTE),
            recipes = listOf(
                recipe("z", CapabilityId.READ_CALENDAR),
                recipe("a", CapabilityId.CREATE_NOTE),
            ),
        )
        val second = first.copy(
            allowedCapabilities = linkedSetOf(CapabilityId.CREATE_NOTE, CapabilityId.READ_CALENDAR),
            recipes = first.recipes.reversed(),
        )
        assertArrayEquals(SkillPackCanonicalizer.bytes(first), SkillPackCanonicalizer.bytes(second))
    }

    private fun manifest(version: String = "1.0.0") = SkillPackManifest(
        id = "com.mobdysseus.notes",
        version = version,
        displayName = "Notes helper",
        description = "Creates a private note after user approval.",
        publisherKeyId = "trusted-key",
        allowedCapabilities = setOf(CapabilityId.CREATE_NOTE),
        recipes = listOf(recipe()),
    )

    private fun recipe(id: String = "save-note", capability: CapabilityId = CapabilityId.CREATE_NOTE) = SkillRecipe(
        id = id,
        displayName = "Save note",
        prompt = "Summarize {{body}} into a private note.",
        inputs = listOf(SkillInputField("body", "Body", SkillInputType.TEXT, true, maxLength = 2_000)),
        actions = listOf(SkillActionDeclaration("save", capability, "Save the reviewed result.")),
    )

    private fun signed(manifest: SkillPackManifest, keyPair: KeyPair): SignedSkillPack {
        val bytes = Signature.getInstance("Ed25519").run {
            initSign(keyPair.private)
            update(SkillPackCanonicalizer.bytes(manifest))
            sign()
        }
        return SignedSkillPack(manifest, Base64.getEncoder().encodeToString(bytes))
    }

    private fun keyPair(): KeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
}

