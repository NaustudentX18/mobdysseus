package com.jakemalby.odysseusmobile.core.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AES-256-GCM adapter. Key material remains non-exportable in Android Keystore. */
class AndroidKeystoreVaultCrypto : VaultCrypto {
    private val keyStore: KeyStore
        get() = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    override fun seal(alias: KeystoreAlias, plaintext: SecretValue): SealedSecret {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey(alias))
        val encrypted = plaintext.useBytes(cipher::doFinal)
        return SealedSecret(cipher.iv, encrypted)
    }

    override fun open(alias: KeystoreAlias, sealedSecret: SealedSecret): SecretValue {
        val key = keyStore.getKey(alias.value, null) as? SecretKey
            ?: error("Credential encryption key is unavailable")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, sealedSecret.nonce))
        return SecretValue.fromBytes(cipher.doFinal(sealedSecret.ciphertext))
    }

    override fun deleteKey(alias: KeystoreAlias) {
        val store = keyStore
        if (store.containsAlias(alias.value)) store.deleteEntry(alias.value)
    }

    private fun getOrCreateKey(alias: KeystoreAlias): SecretKey {
        (keyStore.getKey(alias.value, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                alias.value,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}
