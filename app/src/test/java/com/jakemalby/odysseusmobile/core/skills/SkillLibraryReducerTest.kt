package com.jakemalby.odysseusmobile.core.skills

import com.jakemalby.odysseusmobile.capability.CapabilityId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.time.Instant
import java.util.Base64

class SkillLibraryReducerTest {
    private val now = Instant.parse("2026-08-13T12:00:00Z")
    private val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val verifier = SkillPackVerifier(mapOf("trusted" to keys.public))

    @Test
    fun installAndEnableEachRequireApproval() {
        val pack = verified(manifest())
        val proposed = SkillLibraryReducer.reduce(SkillLibraryState(), SkillLibraryCommand.ProposeInstall(pack, now)) as SkillLibraryResult.AwaitingApproval
        assertNull(proposed.state.runnable(pack.manifest.id))

        val installed = SkillLibraryReducer.reduce(proposed.state, SkillLibraryCommand.ResolvePending(pack.manifest.id, true, now)) as SkillLibraryResult.Applied
        assertEquals(SkillStatus.DISABLED, installed.state.installed.getValue(pack.manifest.id).status)
        assertNull(installed.state.runnable(pack.manifest.id))

        val enable = SkillLibraryReducer.reduce(installed.state, SkillLibraryCommand.ProposeEnable(pack.manifest.id, now)) as SkillLibraryResult.AwaitingApproval
        assertNull(enable.state.runnable(pack.manifest.id))
        val enabled = SkillLibraryReducer.reduce(enable.state, SkillLibraryCommand.ResolvePending(pack.manifest.id, true, now)) as SkillLibraryResult.Applied
        assertEquals(pack.manifest.id, enabled.state.runnable(pack.manifest.id)?.pack?.manifest?.id)
        assertEquals(listOf(1L, 2L, 3L, 4L), enabled.state.audit.map { it.sequence })
    }

    @Test
    fun expandedUpdateProvidesPermissionDiffAndWaitsForApproval() {
        val initial = enabledState(verified(manifest()))
        val candidate = verified(
            manifest("2.0.0").copy(
                allowedCapabilities = setOf(CapabilityId.CREATE_NOTE, CapabilityId.READ_CALENDAR),
                recipes = listOf(
                    recipe("note", CapabilityId.CREATE_NOTE),
                    recipe("calendar", CapabilityId.READ_CALENDAR),
                ),
            ),
        )
        val result = SkillLibraryReducer.reduce(initial, SkillLibraryCommand.ProposeUpdate(candidate, now)) as SkillLibraryResult.AwaitingApproval
        val change = result.change as PendingSkillChange.Update
        assertEquals(setOf(CapabilityId.READ_CALENDAR), change.permissionDiff.added)
        assertTrue(change.permissionDiff.expandsScope)
        assertNull(result.state.runnable(candidate.manifest.id))
        assertEquals("1.0.0", result.state.installed.getValue(candidate.manifest.id).pack.manifest.version)
    }

    @Test
    fun reducedPermissionUpdateAppliesWithoutApproval() {
        val broad = verified(
            manifest().copy(
                allowedCapabilities = setOf(CapabilityId.CREATE_NOTE, CapabilityId.READ_CALENDAR),
                recipes = listOf(recipe("note", CapabilityId.CREATE_NOTE), recipe("calendar", CapabilityId.READ_CALENDAR)),
            ),
        )
        val state = enabledState(broad)
        val reduced = verified(manifest("2.0.0"))
        val result = SkillLibraryReducer.reduce(state, SkillLibraryCommand.ProposeUpdate(reduced, now)) as SkillLibraryResult.Applied
        assertEquals("2.0.0", result.state.installed.getValue(reduced.manifest.id).pack.manifest.version)
        assertFalse(result.state.audit.last().permissionDiff!!.expandsScope)
    }

    @Test
    fun disabledDeletedAndPendingSkillsCannotRun() {
        val pack = verified(manifest())
        val state = enabledState(pack)
        val disabled = SkillLibraryReducer.reduce(state, SkillLibraryCommand.Disable(pack.manifest.id, now)) as SkillLibraryResult.Applied
        assertNull(disabled.state.runnable(pack.manifest.id))
        val deleted = SkillLibraryReducer.reduce(disabled.state, SkillLibraryCommand.Delete(pack.manifest.id, now)) as SkillLibraryResult.Applied
        assertNull(deleted.state.runnable(pack.manifest.id))
        assertFalse(pack.manifest.id in deleted.state.installed)
    }

    @Test
    fun installDeclineLeavesNoInstalledSkill() {
        val pack = verified(manifest())
        val proposal = SkillLibraryReducer.reduce(SkillLibraryState(), SkillLibraryCommand.ProposeInstall(pack, now)) as SkillLibraryResult.AwaitingApproval
        val declined = SkillLibraryReducer.reduce(proposal.state, SkillLibraryCommand.ResolvePending(pack.manifest.id, false, now)) as SkillLibraryResult.Applied
        assertTrue(declined.state.installed.isEmpty())
        assertTrue(declined.state.pending.isEmpty())
        assertEquals(SkillAuditOperation.DECLINED, declined.state.audit.last().operation)
    }

    private fun enabledState(pack: VerifiedSkillPack) = SkillLibraryState(
        installed = mapOf(pack.manifest.id to InstalledSkill(pack, SkillStatus.ENABLED)),
    )

    private fun manifest(version: String = "1.0.0") = SkillPackManifest(
        id = "com.mobdysseus.notes",
        version = version,
        displayName = "Notes",
        description = "A private note helper.",
        publisherKeyId = "trusted",
        allowedCapabilities = setOf(CapabilityId.CREATE_NOTE),
        recipes = listOf(recipe("note", CapabilityId.CREATE_NOTE)),
    )

    private fun recipe(id: String, capability: CapabilityId) = SkillRecipe(
        id = id,
        displayName = id,
        prompt = "Process {{text}}",
        inputs = listOf(SkillInputField("text", "Text", SkillInputType.TEXT, true, 1_000)),
        actions = listOf(SkillActionDeclaration("action-$id", capability, "Run the reviewed action.")),
    )

    private fun verified(manifest: SkillPackManifest): VerifiedSkillPack {
        val signature = Signature.getInstance("Ed25519").run {
            initSign(keys.private)
            update(SkillPackCanonicalizer.bytes(manifest))
            sign()
        }
        val result = verifier.verify(SignedSkillPack(manifest, Base64.getEncoder().encodeToString(signature)))
        return (result as SkillPackVerification.Accepted).pack
    }
}
