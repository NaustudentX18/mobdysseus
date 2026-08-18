package com.jakemalby.odysseusmobile

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.jakemalby.odysseusmobile.core.vault.AccountDeletionPlan
import com.jakemalby.odysseusmobile.core.vault.AccountDeletionResult
import com.jakemalby.odysseusmobile.core.vault.AndroidKeystoreVaultCrypto
import com.jakemalby.odysseusmobile.core.vault.CachedMetadataKey
import com.jakemalby.odysseusmobile.core.vault.CredentialKey
import com.jakemalby.odysseusmobile.core.vault.CredentialName
import com.jakemalby.odysseusmobile.core.vault.CredentialVault
import com.jakemalby.odysseusmobile.core.vault.CredentialVaultStorage
import com.jakemalby.odysseusmobile.core.vault.ExternalAccessMode
import com.jakemalby.odysseusmobile.core.vault.KeystoreAlias
import com.jakemalby.odysseusmobile.core.vault.ProviderId
import com.jakemalby.odysseusmobile.core.vault.ProviderScope
import com.jakemalby.odysseusmobile.core.vault.SealedSecret
import com.jakemalby.odysseusmobile.core.vault.SecretValue
import com.jakemalby.odysseusmobile.core.vault.VaultAccessGate
import com.jakemalby.odysseusmobile.core.vault.VaultAccountId
import com.jakemalby.odysseusmobile.core.vault.VaultRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

private val defaultProviderAccount = VaultAccountId.of("default")
private val openAiCredential = CredentialKey(ProviderId.OPEN_AI, defaultProviderAccount, CredentialName.API_KEY)

/**
 * Account UI only. There is deliberately no HTTP client or remote execution path in this slice.
 * The existing local model and workspace remain fully usable without configuring an account.
 */
@Composable
internal fun ProviderPanel(localOnly: Boolean) {
    val context = LocalContext.current.applicationContext
    val storage = remember(context) { AppPrivateCredentialVaultStorage(context) }
    val crypto = remember { AndroidKeystoreVaultCrypto() }
    val vault = remember(storage, crypto, localOnly) {
        CredentialVault(
            storage = storage,
            crypto = crypto,
            accessGate = VaultAccessGate {
                if (localOnly) ExternalAccessMode.LOCAL_ONLY else ExternalAccessMode.USER_ENABLED
            },
        )
    }
    var apiKey by remember { mutableStateOf("") }
    var configured by remember(storage) { mutableStateOf(storage.get(openAiCredential) != null) }
    var status by remember { mutableStateOf("") }
    var confirmRemoval by remember { mutableStateOf(false) }

    if (confirmRemoval) {
        AlertDialog(
            onDismissRequest = { confirmRemoval = false },
            title = { Text("Remove provider credential?") },
            text = { Text("The encrypted API key and its Android Keystore key will be permanently removed from this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemoval = false
                    val removed = runCatching { vault.deleteAccount(ProviderId.OPEN_AI, defaultProviderAccount) }
                    configured = storage.get(openAiCredential) != null
                    status = if (removed.isSuccess && !configured) "Credential removed" else "Credential removal failed"
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmRemoval = false }) { Text("Cancel") } },
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Optional model providers", fontWeight = FontWeight.Bold)
            Text(
                "Mobdysseus works offline with local models. Provider accounts are optional and remain unused until remote features are added and explicitly enabled.",
                color = Muted,
            )
            Text(
                if (localOnly) "Local-only mode: remote credential use is centrally blocked."
                else "Remote access is allowed by policy, but this build contains no provider network calls.",
                color = if (localOnly) Success else Muted,
                style = MaterialTheme.typography.bodySmall,
            )

            Text("OpenAI / compatible API key", fontWeight = FontWeight.SemiBold)
            if (configured) {
                Text("Saved securely on this phone ••••••••", color = Success)
                OutlinedButton(onClick = { confirmRemoval = true }) { Text("Remove key") }
            } else {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; status = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = { Text("Encrypted before it is written; it is never shown again after saving.") },
                )
                Button(
                    enabled = apiKey.isNotBlank(),
                    onClick = {
                        val chars = apiKey.trim().toCharArray()
                        val saved = runCatching {
                            vault.deleteAccount(ProviderId.OPEN_AI, defaultProviderAccount)
                            SecretValue.fromUtf8(chars).use { secret ->
                                vault.store(openAiCredential, emptySet(), secret)
                            }
                        }
                        chars.fill('\u0000')
                        apiKey = ""
                        configured = storage.get(openAiCredential) != null
                        status = if (saved.isSuccess && configured) "Credential encrypted and saved" else "Credential could not be saved"
                    },
                ) { Text("Save key") }
            }
            if (status.isNotBlank()) Text(status, color = if (configured) Success else Muted)

            ProviderPlaceholder("OpenAI OAuth", "Optional sign-in placeholder; not connected in this offline build.")
            ProviderPlaceholder("Google OAuth", "Optional sign-in placeholder; not connected in this offline build.")
        }
    }
}

@Composable
private fun ProviderPlaceholder(name: String, detail: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Text("Coming later", color = Muted, style = MaterialTheme.typography.labelMedium)
    }
}

/** Ciphertext-only record store in Android's app-private, backup-excluded directory. */
internal class AppPrivateCredentialVaultStorage(context: Context) : CredentialVaultStorage {
    private val file = AtomicFile(File(context.noBackupFilesDir, "provider-credentials.vault"))

    @Synchronized
    override fun put(record: VaultRecord) {
        val records = readRecords().filterNot { it.key == record.key } + record
        writeRecords(records)
    }

    @Synchronized
    override fun get(key: CredentialKey): VaultRecord? = readRecords().firstOrNull { it.key == key }

    @Synchronized
    override fun planAccountDeletion(providerId: ProviderId, accountId: VaultAccountId): AccountDeletionPlan {
        val matching = readRecords().filter { it.key.providerId == providerId && it.key.accountId == accountId }
        return AccountDeletionPlan(
            transactionId = UUID.randomUUID().toString(),
            providerId = providerId,
            accountId = accountId,
            credentialKeys = matching.mapTo(linkedSetOf()) { it.key },
            keystoreAliases = matching.mapTo(linkedSetOf()) { it.alias },
            cachedMetadataKeys = emptySet(),
        )
    }

    @Synchronized
    override fun commitAccountDeletion(plan: AccountDeletionPlan): AccountDeletionResult {
        val before = readRecords()
        val retained = before.filterNot {
            it.key.providerId == plan.providerId && it.key.accountId == plan.accountId && it.key in plan.credentialKeys
        }
        writeRecords(retained)
        return AccountDeletionResult(
            deletedCredentialCount = before.size - retained.size,
            deletedMetadataCount = 0,
            aliasesPendingCleanup = emptySet(),
        )
    }

    private fun readRecords(): List<VaultRecord> {
        if (!file.baseFile.exists()) return emptyList()
        return runCatching {
            val root = JSONObject(file.readFully().toString(Charsets.UTF_8))
            val records = root.getJSONArray("records")
            buildList(records.length()) {
                for (index in 0 until records.length()) add(records.getJSONObject(index).toVaultRecord())
            }
        }.getOrDefault(emptyList())
    }

    private fun writeRecords(records: List<VaultRecord>) {
        val root = JSONObject().put("version", 1).put(
            "records",
            JSONArray().apply { records.forEach { put(it.toJson()) } },
        )
        var stream: java.io.FileOutputStream? = null
        try {
            stream = file.startWrite()
            stream.write(root.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (failure: Throwable) {
            stream?.let(file::failWrite)
            throw failure
        }
    }
}

private fun VaultRecord.toJson(): JSONObject = JSONObject()
    .put("provider", key.providerId.value)
    .put("account", key.accountId.value)
    .put("name", key.name.value)
    .put("scopes", JSONArray().apply { scopes.forEach { put(it.value) } })
    .put("alias", alias.value)
    .put("nonce", Base64.encodeToString(sealedSecret.nonce, Base64.NO_WRAP))
    .put("ciphertext", Base64.encodeToString(sealedSecret.ciphertext, Base64.NO_WRAP))

private fun JSONObject.toVaultRecord(): VaultRecord {
    val scopeArray = getJSONArray("scopes")
    val scopes = buildSet {
        for (index in 0 until scopeArray.length()) add(ProviderScope.of(scopeArray.getString(index)))
    }
    return VaultRecord(
        key = CredentialKey(
            providerId = ProviderId.of(getString("provider")),
            accountId = VaultAccountId.of(getString("account")),
            name = CredentialName.of(getString("name")),
        ),
        scopes = scopes,
        alias = KeystoreAlias(getString("alias")),
        sealedSecret = SealedSecret(
            nonce = Base64.decode(getString("nonce"), Base64.NO_WRAP),
            ciphertext = Base64.decode(getString("ciphertext"), Base64.NO_WRAP),
        ),
    )
}
