package com.jakemalby.odysseusmobile.core.skills

import com.jakemalby.odysseusmobile.capability.CapabilityId
import java.time.Instant

enum class SkillStatus { ENABLED, DISABLED }

data class InstalledSkill(
    val pack: VerifiedSkillPack,
    val status: SkillStatus,
    /** Each pack receives a stable private namespace; packs never share one. */
    val dataNamespace: String = "skill:${pack.manifest.id}",
)

sealed interface PendingSkillChange {
    val packId: String

    data class Install(val candidate: VerifiedSkillPack) : PendingSkillChange {
        override val packId = candidate.manifest.id
    }

    data class Update(
        val candidate: VerifiedSkillPack,
        val permissionDiff: PermissionDiff,
    ) : PendingSkillChange {
        override val packId = candidate.manifest.id
    }

    data class Enable(override val packId: String) : PendingSkillChange
}

enum class SkillAuditOperation { INSTALL_PROPOSED, INSTALLED, UPDATE_PROPOSED, UPDATED, ENABLE_PROPOSED, ENABLED, DISABLED, DELETED, DECLINED }

data class SkillAuditRecord(
    val sequence: Long,
    val occurredAt: Instant,
    val packId: String,
    val operation: SkillAuditOperation,
    val capabilities: Set<CapabilityId>,
    val permissionDiff: PermissionDiff? = null,
)

data class SkillLibraryState(
    val installed: Map<String, InstalledSkill> = emptyMap(),
    val pending: Map<String, PendingSkillChange> = emptyMap(),
    val audit: List<SkillAuditRecord> = emptyList(),
) {
    fun runnable(packId: String): InstalledSkill? = installed[packId]?.takeIf { it.status == SkillStatus.ENABLED && packId !in pending }
}

sealed interface SkillLibraryCommand {
    val occurredAt: Instant

    data class ProposeInstall(val pack: VerifiedSkillPack, override val occurredAt: Instant) : SkillLibraryCommand
    data class ProposeUpdate(val pack: VerifiedSkillPack, override val occurredAt: Instant) : SkillLibraryCommand
    data class ProposeEnable(val packId: String, override val occurredAt: Instant) : SkillLibraryCommand
    data class ResolvePending(val packId: String, val approved: Boolean, override val occurredAt: Instant) : SkillLibraryCommand
    data class Disable(val packId: String, override val occurredAt: Instant) : SkillLibraryCommand
    data class Delete(val packId: String, override val occurredAt: Instant) : SkillLibraryCommand
}

sealed interface SkillLibraryResult {
    val state: SkillLibraryState
    data class Applied(override val state: SkillLibraryState) : SkillLibraryResult
    data class AwaitingApproval(override val state: SkillLibraryState, val change: PendingSkillChange) : SkillLibraryResult
    data class Rejected(override val state: SkillLibraryState, val reason: String) : SkillLibraryResult
}

/** Pure state machine: install, enable and expanding updates cannot skip approval. */
object SkillLibraryReducer {
    fun reduce(state: SkillLibraryState, command: SkillLibraryCommand): SkillLibraryResult = when (command) {
        is SkillLibraryCommand.ProposeInstall -> proposeInstall(state, command)
        is SkillLibraryCommand.ProposeUpdate -> proposeUpdate(state, command)
        is SkillLibraryCommand.ProposeEnable -> proposeEnable(state, command)
        is SkillLibraryCommand.ResolvePending -> resolve(state, command)
        is SkillLibraryCommand.Disable -> disable(state, command)
        is SkillLibraryCommand.Delete -> delete(state, command)
    }

    private fun proposeInstall(state: SkillLibraryState, command: SkillLibraryCommand.ProposeInstall): SkillLibraryResult {
        val id = command.pack.manifest.id
        if (id in state.installed || id in state.pending) return SkillLibraryResult.Rejected(state, "Skill is already installed or pending.")
        val pending = PendingSkillChange.Install(command.pack)
        val next = state.withPending(pending).audited(command.occurredAt, id, SkillAuditOperation.INSTALL_PROPOSED, command.pack.manifest.allowedCapabilities)
        return SkillLibraryResult.AwaitingApproval(next, pending)
    }

    private fun proposeUpdate(state: SkillLibraryState, command: SkillLibraryCommand.ProposeUpdate): SkillLibraryResult {
        val id = command.pack.manifest.id
        val current = state.installed[id] ?: return SkillLibraryResult.Rejected(state, "Skill is not installed.")
        if (id in state.pending) return SkillLibraryResult.Rejected(state, "Skill already has a pending change.")
        if (compareVersions(command.pack.manifest.version, current.pack.manifest.version) <= 0) return SkillLibraryResult.Rejected(state, "Update version must be newer.")
        if (command.pack.manifest.publisherKeyId != current.pack.manifest.publisherKeyId) return SkillLibraryResult.Rejected(state, "Publisher identity cannot change during update.")
        val diff = PermissionDiff.between(current.pack.manifest, command.pack.manifest)
        return if (diff.expandsScope) {
            val pending = PendingSkillChange.Update(command.pack, diff)
            val next = state.withPending(pending).audited(command.occurredAt, id, SkillAuditOperation.UPDATE_PROPOSED, command.pack.manifest.allowedCapabilities, diff)
            SkillLibraryResult.AwaitingApproval(next, pending)
        } else {
            val next = state.copy(installed = state.installed + (id to current.copy(pack = command.pack)))
                .audited(command.occurredAt, id, SkillAuditOperation.UPDATED, command.pack.manifest.allowedCapabilities, diff)
            SkillLibraryResult.Applied(next)
        }
    }

    private fun proposeEnable(state: SkillLibraryState, command: SkillLibraryCommand.ProposeEnable): SkillLibraryResult {
        val current = state.installed[command.packId] ?: return SkillLibraryResult.Rejected(state, "Skill is not installed.")
        if (current.status == SkillStatus.ENABLED) return SkillLibraryResult.Rejected(state, "Skill is already enabled.")
        if (command.packId in state.pending) return SkillLibraryResult.Rejected(state, "Skill already has a pending change.")
        val pending = PendingSkillChange.Enable(command.packId)
        val next = state.withPending(pending).audited(command.occurredAt, command.packId, SkillAuditOperation.ENABLE_PROPOSED, current.pack.manifest.allowedCapabilities)
        return SkillLibraryResult.AwaitingApproval(next, pending)
    }

    private fun resolve(state: SkillLibraryState, command: SkillLibraryCommand.ResolvePending): SkillLibraryResult {
        val pending = state.pending[command.packId] ?: return SkillLibraryResult.Rejected(state, "No pending change exists.")
        val withoutPending = state.copy(pending = state.pending - command.packId)
        if (!command.approved) {
            val capabilities = when (pending) {
                is PendingSkillChange.Install -> pending.candidate.manifest.allowedCapabilities
                is PendingSkillChange.Update -> pending.candidate.manifest.allowedCapabilities
                is PendingSkillChange.Enable -> state.installed[pending.packId]?.pack?.manifest?.allowedCapabilities.orEmpty()
            }
            return SkillLibraryResult.Applied(withoutPending.audited(command.occurredAt, command.packId, SkillAuditOperation.DECLINED, capabilities))
        }
        val next = when (pending) {
            is PendingSkillChange.Install -> withoutPending.copy(installed = withoutPending.installed + (pending.packId to InstalledSkill(pending.candidate, SkillStatus.DISABLED)))
                .audited(command.occurredAt, pending.packId, SkillAuditOperation.INSTALLED, pending.candidate.manifest.allowedCapabilities)
            is PendingSkillChange.Update -> {
                val prior = withoutPending.installed[pending.packId] ?: return SkillLibraryResult.Rejected(state, "Installed skill disappeared while update was pending.")
                withoutPending.copy(installed = withoutPending.installed + (pending.packId to prior.copy(pack = pending.candidate)))
                    .audited(command.occurredAt, pending.packId, SkillAuditOperation.UPDATED, pending.candidate.manifest.allowedCapabilities, pending.permissionDiff)
            }
            is PendingSkillChange.Enable -> {
                val prior = withoutPending.installed[pending.packId] ?: return SkillLibraryResult.Rejected(state, "Installed skill disappeared while enable was pending.")
                withoutPending.copy(installed = withoutPending.installed + (pending.packId to prior.copy(status = SkillStatus.ENABLED)))
                    .audited(command.occurredAt, pending.packId, SkillAuditOperation.ENABLED, prior.pack.manifest.allowedCapabilities)
            }
        }
        return SkillLibraryResult.Applied(next)
    }

    private fun disable(state: SkillLibraryState, command: SkillLibraryCommand.Disable): SkillLibraryResult {
        val current = state.installed[command.packId] ?: return SkillLibraryResult.Rejected(state, "Skill is not installed.")
        val next = state.copy(
            installed = state.installed + (command.packId to current.copy(status = SkillStatus.DISABLED)),
            pending = state.pending - command.packId,
        ).audited(command.occurredAt, command.packId, SkillAuditOperation.DISABLED, current.pack.manifest.allowedCapabilities)
        return SkillLibraryResult.Applied(next)
    }

    private fun delete(state: SkillLibraryState, command: SkillLibraryCommand.Delete): SkillLibraryResult {
        val current = state.installed[command.packId] ?: return SkillLibraryResult.Rejected(state, "Skill is not installed.")
        val next = state.copy(installed = state.installed - command.packId, pending = state.pending - command.packId)
            .audited(command.occurredAt, command.packId, SkillAuditOperation.DELETED, current.pack.manifest.allowedCapabilities)
        return SkillLibraryResult.Applied(next)
    }

    private fun SkillLibraryState.withPending(change: PendingSkillChange) = copy(pending = pending + (change.packId to change))

    private fun SkillLibraryState.audited(
        at: Instant,
        packId: String,
        operation: SkillAuditOperation,
        capabilities: Set<CapabilityId>,
        diff: PermissionDiff? = null,
    ) = copy(audit = audit + SkillAuditRecord((audit.lastOrNull()?.sequence ?: 0) + 1, at, packId, operation, capabilities, diff))

    private fun compareVersions(left: String, right: String): Int {
        fun parts(value: String) = value.substringBefore('-').split('.').map(String::toLong)
        val a = parts(left)
        val b = parts(right)
        for (index in 0..2) if (a[index] != b[index]) return a[index].compareTo(b[index])
        val aPre = left.substringAfter('-', "")
        val bPre = right.substringAfter('-', "")
        return when {
            aPre == bPre -> 0
            aPre.isEmpty() -> 1
            bPre.isEmpty() -> -1
            else -> aPre.compareTo(bPre)
        }
    }
}

