package com.jakemalby.odysseusmobile.persistence.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps SQLCipher's random database key wrapped by a non-exportable Android
 * Keystore key. Only the AES-GCM ciphertext is persisted, in no-backup storage.
 */
internal class EncryptedDatabaseKeyStore(context: Context) {
    private val wrappedKeyFile = AtomicFile(File(context.noBackupFilesDir, WRAPPED_KEY_FILE))

    fun loadOrCreate(databaseExists: Boolean): ByteArray = synchronized(lock) {
        if (wrappedKeyFile.baseFile.exists()) return@synchronized unwrap(readEnvelope())
        check(!databaseExists) {
            "The encrypted database key is missing; refusing to replace it while database data exists."
        }

        val databaseKey = ByteArray(DATABASE_KEY_BYTES).also(SecureRandom()::nextBytes)
        try {
            writeEnvelope(wrap(databaseKey))
            databaseKey
        } catch (failure: Throwable) {
            databaseKey.fill(0)
            throw failure
        }
    }

    private fun wrap(databaseKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        cipher.updateAAD(ASSOCIATED_DATA)
        val ciphertext = cipher.doFinal(databaseKey)
        require(cipher.iv.size == GCM_IV_BYTES) { "Unexpected database key IV length." }
        return ByteBuffer.allocate(MAGIC.size + 1 + cipher.iv.size + ciphertext.size)
            .put(MAGIC)
            .put(ENVELOPE_VERSION)
            .put(cipher.iv)
            .put(ciphertext)
            .array()
    }

    private fun unwrap(envelope: ByteArray): ByteArray {
        require(envelope.size == ENVELOPE_BYTES) { "The encrypted database key envelope is invalid." }
        val buffer = ByteBuffer.wrap(envelope)
        val magic = ByteArray(MAGIC.size).also(buffer::get)
        require(magic.contentEquals(MAGIC)) { "The encrypted database key envelope is invalid." }
        require(buffer.get() == ENVELOPE_VERSION) { "The encrypted database key version is unsupported." }
        val iv = ByteArray(GCM_IV_BYTES).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(ASSOCIATED_DATA)
        return cipher.doFinal(ciphertext).also {
            require(it.size == DATABASE_KEY_BYTES) { "The encrypted database key has an invalid length." }
        }
    }

    private fun readEnvelope(): ByteArray = wrappedKeyFile.openRead().use { input ->
        val bytes = input.readBytes()
        require(bytes.size <= MAX_ENVELOPE_BYTES) { "The encrypted database key envelope is too large." }
        bytes
    }

    private fun writeEnvelope(envelope: ByteArray) {
        val output = wrappedKeyFile.startWrite()
        try {
            output.write(envelope)
            wrappedKeyFile.finishWrite(output)
        } catch (failure: Throwable) {
            wrappedKeyFile.failWrite(output)
            throw failure
        }
    }

    private fun wrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
        }.generateKey()
    }

    private companion object {
        const val WRAPPED_KEY_FILE = "mobdysseus-database-key.v1"
        const val KEY_ALIAS = "mobdysseus.database.kek.v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val DATABASE_KEY_BYTES = 32
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BYTES = 16
        const val GCM_TAG_BITS = GCM_TAG_BYTES * 8
        const val ENVELOPE_BYTES = 4 + 1 + GCM_IV_BYTES + DATABASE_KEY_BYTES + GCM_TAG_BYTES
        const val MAX_ENVELOPE_BYTES = 256
        const val ENVELOPE_VERSION: Byte = 1
        val MAGIC = byteArrayOf(0x4d, 0x44, 0x42, 0x4b) // MDBK
        val ASSOCIATED_DATA = "mobdysseus.database.key.v1".toByteArray(Charsets.UTF_8)
        val lock = Any()
    }
}
