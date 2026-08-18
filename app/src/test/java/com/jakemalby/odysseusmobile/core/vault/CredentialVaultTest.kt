package com.jakemalby.odysseusmobile.core.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialVaultTest {
    private val account = VaultAccountId.of("personal")
    private val key = CredentialKey(ProviderId.OPEN_AI, account, CredentialName.API_KEY)
    private val alias = KeystoreAlias("mobdysseus-vault-00000000-0000-0000-0000-000000000001")

    @Test
    fun `secret is redacted identity-only and cleared`() {
        val chars = "super-secret-key".toCharArray()
        val first = SecretValue.fromUtf8(chars)
        val second = SecretValue.fromUtf8(chars)

        assertEquals("SecretValue([REDACTED])", first.toString())
        assertNotEquals(first, second)
        first.close()
        assertTrue(first.isCleared)
    }

    @Test
    fun `local-only gate denies remote use before storage or decryption`() {
        val storage = FakeStorage()
        val crypto = FakeCrypto()
        val vault = CredentialVault(
            storage,
            crypto,
            VaultAccessGate { ExternalAccessMode.LOCAL_ONLY },
            aliasFactory = { alias },
        )

        val result = vault.open(key, VaultAccessPurpose.REMOTE_AUTHENTICATION) { "must not run" }

        assertEquals(VaultOpenResult.Denied(VaultDenialReason.LOCAL_ONLY_MODE), result)
        assertEquals(0, storage.reads)
        assertEquals(0, crypto.opens)
    }

    @Test
    fun `opened plaintext is cleared immediately after callback`() {
        val storage = FakeStorage()
        val crypto = FakeCrypto()
        val vault = CredentialVault(
            storage,
            crypto,
            VaultAccessGate { ExternalAccessMode.USER_ENABLED },
            aliasFactory = { alias },
        )
        SecretValue.fromUtf8("key-value".toCharArray()).use {
            vault.store(key, setOf(ProviderScope.of("models.read")), it)
        }
        var observed: SecretValue? = null

        val result = vault.open(key, VaultAccessPurpose.REMOTE_AUTHENTICATION) {
            observed = it
            it.useBytes { bytes -> bytes.size }
        }

        assertEquals(VaultOpenResult.Opened(9), result)
        assertTrue(observed!!.isCleared)
    }

    @Test
    fun `delete account atomically includes credentials metadata and key cleanup`() {
        val storage = FakeStorage()
        val crypto = FakeCrypto()
        val vault = CredentialVault(
            storage,
            crypto,
            VaultAccessGate { ExternalAccessMode.LOCAL_ONLY },
            aliasFactory = { alias },
        )
        SecretValue.fromUtf8("key-value".toCharArray()).use { vault.store(key, emptySet(), it) }
        storage.metadata += CachedMetadataKey.of("models-cache")

        val result = vault.deleteAccount(ProviderId.OPEN_AI, account)

        assertEquals(1, result.deletedCredentialCount)
        assertEquals(1, result.deletedMetadataCount)
        assertTrue(result.aliasesPendingCleanup.isEmpty())
        assertTrue(storage.records.isEmpty())
        assertTrue(storage.metadata.isEmpty())
        assertEquals(setOf(alias), crypto.deletedAliases)
        assertTrue(storage.committedAtomically)
    }

    @Test
    fun `failed key cleanup is returned for safe retry without restoring records`() {
        val storage = FakeStorage()
        val crypto = FakeCrypto(failDelete = true)
        val vault = CredentialVault(storage, crypto, VaultAccessGate { ExternalAccessMode.LOCAL_ONLY }) { alias }
        SecretValue.fromUtf8("key-value".toCharArray()).use { vault.store(key, emptySet(), it) }

        val result = vault.deleteAccount(ProviderId.OPEN_AI, account)

        assertEquals(setOf(alias), result.aliasesPendingCleanup)
        assertTrue(storage.records.isEmpty())
    }

    private class FakeCrypto(private val failDelete: Boolean = false) : VaultCrypto {
        var opens = 0
        val deletedAliases = mutableSetOf<KeystoreAlias>()

        override fun seal(alias: KeystoreAlias, plaintext: SecretValue): SealedSecret =
            plaintext.useBytes { bytes -> SealedSecret(ByteArray(12) { 7 }, bytes.map { (it.toInt() xor 0x55).toByte() }.toByteArray()) }

        override fun open(alias: KeystoreAlias, sealedSecret: SealedSecret): SecretValue {
            opens++
            return SecretValue.fromBytes(sealedSecret.ciphertext.map { (it.toInt() xor 0x55).toByte() }.toByteArray())
        }

        override fun deleteKey(alias: KeystoreAlias) {
            if (failDelete) error("simulated failure")
            deletedAliases += alias
        }
    }

    private class FakeStorage : CredentialVaultStorage {
        val records = mutableMapOf<CredentialKey, VaultRecord>()
        val metadata = mutableSetOf<CachedMetadataKey>()
        var reads = 0
        var committedAtomically = false

        override fun put(record: VaultRecord) {
            records[record.key] = record
        }

        override fun get(key: CredentialKey): VaultRecord? {
            reads++
            return records[key]
        }

        override fun planAccountDeletion(providerId: ProviderId, accountId: VaultAccountId): AccountDeletionPlan {
            val selected = records.values.filter { it.key.providerId == providerId && it.key.accountId == accountId }
            return AccountDeletionPlan(
                transactionId = "tx-1",
                providerId = providerId,
                accountId = accountId,
                credentialKeys = selected.mapTo(mutableSetOf()) { it.key },
                keystoreAliases = selected.mapTo(mutableSetOf()) { it.alias },
                cachedMetadataKeys = metadata.toSet(),
            )
        }

        override fun commitAccountDeletion(plan: AccountDeletionPlan): AccountDeletionResult {
            plan.credentialKeys.forEach(records::remove)
            plan.cachedMetadataKeys.forEach(metadata::remove)
            committedAtomically = true
            return AccountDeletionResult(plan.credentialKeys.size, plan.cachedMetadataKeys.size, emptySet())
        }
    }
}
