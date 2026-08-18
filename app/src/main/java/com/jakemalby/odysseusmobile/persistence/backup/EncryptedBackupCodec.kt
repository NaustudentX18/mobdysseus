package com.jakemalby.odysseusmobile.core

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable, versioned Mobdysseus backup encryption.
 *
 * The [CharArray] passed to [encrypt] or [decrypt] is consumed and cleared before the call
 * returns. Callers should therefore create a dedicated passphrase array for each operation.
 * The envelope authenticates its metadata, salt and IV as well as the encrypted payload.
 */
class EncryptedBackupCodec(
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    fun encrypt(
        plaintext: ByteArray,
        passphrase: CharArray,
        iterations: Int = DEFAULT_ITERATIONS,
    ): ByteArray {
        try {
            requirePassphrase(passphrase)
            require(plaintext.size <= MAX_PLAINTEXT_BYTES) { "Backup is too large" }
            require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Invalid KDF work factor" }

            val salt = ByteArray(SALT_BYTES).also(secureRandom::nextBytes)
            val iv = ByteArray(IV_BYTES).also(secureRandom::nextBytes)
            val ciphertextLength = plaintext.size + GCM_TAG_BYTES
            val header = encodeHeader(iterations, salt.size, iv.size, ciphertextLength)
            val authenticatedMetadata = header + salt + iv
            return try {
                val keyBytes = deriveKey(passphrase, salt, iterations)
                try {
                    val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                    cipher.init(
                        Cipher.ENCRYPT_MODE,
                        SecretKeySpec(keyBytes, "AES"),
                        GCMParameterSpec(GCM_TAG_BITS, iv),
                    )
                    cipher.updateAAD(authenticatedMetadata)
                    val ciphertext = cipher.doFinal(plaintext)
                    ByteArrayOutputStream(authenticatedMetadata.size + ciphertext.size).use { output ->
                        output.write(authenticatedMetadata)
                        output.write(ciphertext)
                        output.toByteArray()
                    }
                } finally {
                    keyBytes.fill(0)
                }
            } finally {
                salt.fill(0)
                iv.fill(0)
            }
        } finally {
            passphrase.fill('\u0000')
        }
    }

    /**
     * Decrypts a Mobdysseus backup. Malformed data, tampering and an incorrect passphrase all
     * produce [BackupAuthenticationException] so callers do not expose a useful oracle.
     */
    fun decrypt(envelope: ByteArray, passphrase: CharArray): ByteArray {
        try {
            requirePassphrase(passphrase)
            val parsed = try {
                parse(envelope)
            } catch (_: RuntimeException) {
                throw BackupAuthenticationException()
            }
            return try {
                val keyBytes = deriveKey(passphrase, parsed.salt, parsed.iterations)
                try {
                    val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                    cipher.init(
                        Cipher.DECRYPT_MODE,
                        SecretKeySpec(keyBytes, "AES"),
                        GCMParameterSpec(GCM_TAG_BITS, parsed.iv),
                    )
                    cipher.updateAAD(parsed.authenticatedMetadata)
                    cipher.doFinal(parsed.ciphertext)
                } catch (_: AEADBadTagException) {
                    throw BackupAuthenticationException()
                } catch (_: GeneralSecurityException) {
                    throw BackupAuthenticationException()
                } finally {
                    keyBytes.fill(0)
                }
            } catch (error: BackupAuthenticationException) {
                throw error
            } catch (_: GeneralSecurityException) {
                throw BackupAuthenticationException()
            } finally {
                parsed.salt.fill(0)
                parsed.iv.fill(0)
                parsed.ciphertext.fill(0)
                parsed.authenticatedMetadata.fill(0)
            }
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private fun parse(envelope: ByteArray): ParsedEnvelope {
        require(envelope.size in MIN_ENVELOPE_BYTES..MAX_ENVELOPE_BYTES)
        val input = ByteBuffer.wrap(envelope).order(ByteOrder.BIG_ENDIAN)
        val magic = ByteArray(MAGIC.size).also(input::get)
        require(magic.contentEquals(MAGIC))
        require(input.get().toInt() and 0xff == FORMAT_VERSION)
        require(input.get().toInt() and 0xff == KDF_PBKDF2_SHA256)
        require(input.get().toInt() and 0xff == CIPHER_AES_256_GCM)
        require(input.get().toInt() and 0xff == 0) // reserved flags
        val iterations = input.int
        val saltLength = input.short.toInt() and 0xffff
        val ivLength = input.short.toInt() and 0xffff
        val ciphertextLength = input.int
        require(iterations in MIN_ITERATIONS..MAX_ITERATIONS)
        require(saltLength == SALT_BYTES)
        require(ivLength == IV_BYTES)
        require(ciphertextLength in GCM_TAG_BYTES..(MAX_PLAINTEXT_BYTES + GCM_TAG_BYTES))
        val metadataLength = FIXED_HEADER_BYTES + saltLength + ivLength
        require(metadataLength.toLong() + ciphertextLength.toLong() == envelope.size.toLong())

        val salt = ByteArray(saltLength).also(input::get)
        val iv = ByteArray(ivLength).also(input::get)
        val ciphertext = ByteArray(ciphertextLength).also(input::get)
        return ParsedEnvelope(
            iterations = iterations,
            salt = salt,
            iv = iv,
            ciphertext = ciphertext,
            authenticatedMetadata = envelope.copyOfRange(0, metadataLength),
        )
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, AES_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encodeHeader(
        iterations: Int,
        saltLength: Int,
        ivLength: Int,
        ciphertextLength: Int,
    ): ByteArray = ByteBuffer.allocate(FIXED_HEADER_BYTES).order(ByteOrder.BIG_ENDIAN).apply {
        put(MAGIC)
        put(FORMAT_VERSION.toByte())
        put(KDF_PBKDF2_SHA256.toByte())
        put(CIPHER_AES_256_GCM.toByte())
        put(0) // reserved flags
        putInt(iterations)
        putShort(saltLength.toShort())
        putShort(ivLength.toShort())
        putInt(ciphertextLength)
    }.array()

    private fun requirePassphrase(passphrase: CharArray) {
        require(passphrase.isNotEmpty()) { "Passphrase must not be empty" }
        require(passphrase.size <= MAX_PASSPHRASE_CHARS) { "Passphrase is too long" }
    }

    private data class ParsedEnvelope(
        val iterations: Int,
        val salt: ByteArray,
        val iv: ByteArray,
        val ciphertext: ByteArray,
        val authenticatedMetadata: ByteArray,
    )

    companion object {
        private val MAGIC = byteArrayOf(0x4d, 0x4f, 0x42, 0x44, 0x42, 0x41, 0x4b, 0x00)
        private const val FORMAT_VERSION = 1
        private const val KDF_PBKDF2_SHA256 = 1
        private const val CIPHER_AES_256_GCM = 1
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES_KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val FIXED_HEADER_BYTES = 24
        private const val MIN_ENVELOPE_BYTES = FIXED_HEADER_BYTES + SALT_BYTES + IV_BYTES + GCM_TAG_BYTES
        private const val MAX_PASSPHRASE_CHARS = 1_024
        private const val MAX_PLAINTEXT_BYTES = 64 * 1024 * 1024
        private const val MAX_ENVELOPE_BYTES = MAX_PLAINTEXT_BYTES + MIN_ENVELOPE_BYTES
        const val DEFAULT_ITERATIONS = 310_000
        const val MIN_ITERATIONS = 100_000
        const val MAX_ITERATIONS = 2_000_000
    }
}

class BackupAuthenticationException : Exception("Backup could not be authenticated")
