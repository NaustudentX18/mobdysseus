package com.jakemalby.odysseusmobile.core.vault

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.UUID

@JvmInline
value class ProviderId private constructor(val value: String) {
    companion object {
        val OPEN_AI = of("openai")
        val ANTHROPIC = of("anthropic")
        val GOOGLE = of("google")
        val OPEN_ROUTER = of("openrouter")

        fun of(value: String): ProviderId {
            val normalized = value.trim().lowercase()
            require(normalized.matches(Regex("[a-z][a-z0-9._-]{1,63}"))) { "Invalid provider ID" }
            return ProviderId(normalized)
        }
    }
}

@JvmInline
value class ProviderScope private constructor(val value: String) {
    companion object {
        fun of(value: String): ProviderScope {
            val normalized = value.trim().lowercase()
            require(normalized.matches(Regex("[a-z][a-z0-9._:-]{0,95}"))) { "Invalid provider scope" }
            return ProviderScope(normalized)
        }
    }
}

@JvmInline
value class VaultAccountId private constructor(val value: String) {
    companion object {
        fun of(value: String): VaultAccountId {
            val normalized = value.trim()
            require(normalized.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,95}"))) { "Invalid account ID" }
            return VaultAccountId(normalized)
        }
    }
}

@JvmInline
value class CredentialName private constructor(val value: String) {
    companion object {
        val API_KEY = of("api-key")
        val ACCESS_TOKEN = of("access-token")
        val REFRESH_TOKEN = of("refresh-token")

        fun of(value: String): CredentialName {
            val normalized = value.trim().lowercase()
            require(normalized.matches(Regex("[a-z][a-z0-9._-]{1,63}"))) { "Invalid credential name" }
            return CredentialName(normalized)
        }
    }
}

data class CredentialKey(
    val providerId: ProviderId,
    val accountId: VaultAccountId,
    val name: CredentialName,
)

/** Plaintext secret with identity equality, redacted rendering and deterministic memory clearing. */
class SecretValue private constructor(private var bytes: ByteArray?) : AutoCloseable {
    companion object {
        fun fromUtf8(chars: CharArray): SecretValue {
            require(chars.isNotEmpty()) { "Secret must not be empty" }
            val encoder = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val encoded = encoder.encode(CharBuffer.wrap(chars))
            val copy = ByteArray(encoded.remaining())
            encoded.get(copy)
            if (encoded.hasArray()) encoded.array().fill(0)
            return SecretValue(copy)
        }

        internal fun fromBytes(bytes: ByteArray): SecretValue {
            require(bytes.isNotEmpty()) { "Secret must not be empty" }
            return SecretValue(bytes.copyOf())
        }
    }

    internal fun <T> useBytes(block: (ByteArray) -> T): T {
        val current = checkNotNull(bytes) { "Secret has been cleared" }
        return block(current)
    }

    val isCleared: Boolean get() = bytes == null

    override fun close() {
        bytes?.fill(0)
        bytes = null
    }

    override fun toString(): String = "SecretValue([REDACTED])"
}

class SealedSecret(
    nonce: ByteArray,
    ciphertext: ByteArray,
) {
    val nonce: ByteArray = nonce.copyOf()
    val ciphertext: ByteArray = ciphertext.copyOf()

    init {
        require(this.nonce.size == 12) { "AES-GCM nonce must be 12 bytes" }
        require(this.ciphertext.isNotEmpty()) { "Ciphertext must not be empty" }
    }

    override fun toString(): String = "SealedSecret([REDACTED])"
}

@JvmInline
value class KeystoreAlias(val value: String) {
    init {
        require(value.matches(Regex("mobdysseus-vault-[a-f0-9-]{36}"))) { "Invalid Keystore alias" }
    }

    companion object {
        fun create(): KeystoreAlias = KeystoreAlias("mobdysseus-vault-${UUID.randomUUID()}")
    }
}

data class VaultRecord(
    val key: CredentialKey,
    val scopes: Set<ProviderScope>,
    val alias: KeystoreAlias,
    val sealedSecret: SealedSecret,
)

@JvmInline
value class CachedMetadataKey private constructor(val value: String) {
    companion object {
        fun of(value: String): CachedMetadataKey {
            val normalized = value.trim().lowercase()
            require(normalized.matches(Regex("[a-z][a-z0-9._-]{1,95}"))) { "Invalid metadata key" }
            return CachedMetadataKey(normalized)
        }
    }
}

enum class VaultAccessPurpose {
    LOCAL_STORAGE,
    LOCAL_ACCOUNT_DISPLAY,
    REMOTE_AUTHENTICATION,
}

enum class ExternalAccessMode { LOCAL_ONLY, USER_ENABLED }

sealed interface VaultAccessDecision {
    data object Allowed : VaultAccessDecision
    data class Denied(val reason: VaultDenialReason) : VaultAccessDecision
}

enum class VaultDenialReason { LOCAL_ONLY_MODE }

/** Central policy boundary. Callers cannot obtain plaintext without passing this gate. */
class VaultAccessGate(private val mode: () -> ExternalAccessMode) {
    fun decide(purpose: VaultAccessPurpose): VaultAccessDecision =
        if (purpose == VaultAccessPurpose.REMOTE_AUTHENTICATION && mode() == ExternalAccessMode.LOCAL_ONLY) {
            VaultAccessDecision.Denied(VaultDenialReason.LOCAL_ONLY_MODE)
        } else {
            VaultAccessDecision.Allowed
        }
}

sealed interface VaultOpenResult<out T> {
    data class Opened<T>(val value: T) : VaultOpenResult<T>
    data object NotFound : VaultOpenResult<Nothing>
    data class Denied(val reason: VaultDenialReason) : VaultOpenResult<Nothing>
}

data class AccountDeletionPlan(
    val transactionId: String,
    val providerId: ProviderId,
    val accountId: VaultAccountId,
    val credentialKeys: Set<CredentialKey>,
    val keystoreAliases: Set<KeystoreAlias>,
    val cachedMetadataKeys: Set<CachedMetadataKey>,
)

data class AccountDeletionResult(
    val deletedCredentialCount: Int,
    val deletedMetadataCount: Int,
    val aliasesPendingCleanup: Set<KeystoreAlias>,
)

interface VaultCrypto {
    fun seal(alias: KeystoreAlias, plaintext: SecretValue): SealedSecret
    fun open(alias: KeystoreAlias, sealedSecret: SealedSecret): SecretValue
    fun deleteKey(alias: KeystoreAlias)
}

/** Implementations must commit credential and cached-metadata deletion atomically. */
interface CredentialVaultStorage {
    fun put(record: VaultRecord)
    fun get(key: CredentialKey): VaultRecord?
    fun planAccountDeletion(providerId: ProviderId, accountId: VaultAccountId): AccountDeletionPlan
    fun commitAccountDeletion(plan: AccountDeletionPlan): AccountDeletionResult
}

class CredentialVault(
    private val storage: CredentialVaultStorage,
    private val crypto: VaultCrypto,
    private val accessGate: VaultAccessGate,
    private val aliasFactory: () -> KeystoreAlias = KeystoreAlias::create,
) {
    fun store(key: CredentialKey, scopes: Set<ProviderScope>, secret: SecretValue) {
        val alias = aliasFactory()
        val sealed = crypto.seal(alias, secret)
        storage.put(VaultRecord(key, scopes.toSet(), alias, sealed))
    }

    fun <T> open(
        key: CredentialKey,
        purpose: VaultAccessPurpose,
        consume: (SecretValue) -> T,
    ): VaultOpenResult<T> {
        when (val decision = accessGate.decide(purpose)) {
            is VaultAccessDecision.Denied -> return VaultOpenResult.Denied(decision.reason)
            VaultAccessDecision.Allowed -> Unit
        }
        val record = storage.get(key) ?: return VaultOpenResult.NotFound
        val secret = crypto.open(record.alias, record.sealedSecret)
        return secret.use { VaultOpenResult.Opened(consume(it)) }
    }

    fun deleteAccount(providerId: ProviderId, accountId: VaultAccountId): AccountDeletionResult {
        val plan = storage.planAccountDeletion(providerId, accountId)
        val result = storage.commitAccountDeletion(plan)
        val pending = buildSet {
            plan.keystoreAliases.forEach { alias ->
                runCatching { crypto.deleteKey(alias) }.onFailure { add(alias) }
            }
        }
        return result.copy(aliasesPendingCleanup = result.aliasesPendingCleanup + pending)
    }
}
