package com.jakemalby.odysseusmobile.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupCodecTest {
    private val codec = EncryptedBackupCodec()

    @Test
    fun `round trips empty binary and unicode content`() {
        listOf(
            byteArrayOf(),
            byteArrayOf(0, 1, 2, -1, 0, 127),
            "Mobdysseus backup — notes, 🧭 and chats".toByteArray(),
        ).forEach { plaintext ->
            val envelope = codec.encrypt(plaintext, "correct horse battery staple".toCharArray())
            val restored = codec.decrypt(envelope, "correct horse battery staple".toCharArray())
            assertArrayEquals(plaintext, restored)
            assertFalse(envelope.contentEquals(plaintext))
        }
    }

    @Test
    fun `same input creates different randomized envelopes`() {
        val plaintext = "private workspace".toByteArray()
        val first = codec.encrypt(plaintext, "passphrase".toCharArray())
        val second = codec.encrypt(plaintext, "passphrase".toCharArray())

        assertFalse(first.contentEquals(second))
        assertArrayEquals(plaintext, codec.decrypt(first, "passphrase".toCharArray()))
        assertArrayEquals(plaintext, codec.decrypt(second, "passphrase".toCharArray()))
    }

    @Test
    fun `wrong passphrase is rejected without leaking details`() {
        val envelope = codec.encrypt("classified".toByteArray(), "right password".toCharArray())

        val error = assertThrows(BackupAuthenticationException::class.java) {
            codec.decrypt(envelope, "wrong password".toCharArray())
        }
        assertTrue(error.message!!.contains("authenticated"))
    }

    @Test
    fun `tampering in every envelope region is rejected`() {
        val envelope = codec.encrypt("classified".toByteArray(), "right password".toCharArray())
        listOf(0, 8, 9, 12, 24, 40, envelope.lastIndex).forEach { position ->
            val corrupt = envelope.copyOf().also { it[position] = (it[position].toInt() xor 1).toByte() }
            assertThrows("position $position", BackupAuthenticationException::class.java) {
                codec.decrypt(corrupt, "right password".toCharArray())
            }
        }
    }

    @Test
    fun `truncated oversized-field and trailing data envelopes are rejected`() {
        val envelope = codec.encrypt("classified".toByteArray(), "right password".toCharArray())
        val hugeCiphertext = envelope.copyOf().also {
            it[20] = 0x7f
            it[21] = -1
            it[22] = -1
            it[23] = -1
        }
        listOf(
            envelope.copyOf(envelope.size - 1),
            envelope + byteArrayOf(0),
            hugeCiphertext,
            byteArrayOf(),
        ).forEach { malformed ->
            assertThrows(BackupAuthenticationException::class.java) {
                codec.decrypt(malformed, "right password".toCharArray())
            }
        }
    }

    @Test
    fun `passphrase arrays are cleared after success and failure`() {
        val encryptionPassword = "clear me".toCharArray()
        val envelope = codec.encrypt("classified".toByteArray(), encryptionPassword)
        assertTrue(encryptionPassword.all { it == '\u0000' })

        val successfulPassword = "clear me".toCharArray()
        codec.decrypt(envelope, successfulPassword)
        assertTrue(successfulPassword.all { it == '\u0000' })

        val failingPassword = "wrong".toCharArray()
        assertThrows(BackupAuthenticationException::class.java) {
            codec.decrypt(envelope, failingPassword)
        }
        assertTrue(failingPassword.all { it == '\u0000' })
    }

    @Test
    fun `empty too-long and unsafe-work-factor inputs are rejected and cleared`() {
        val empty = charArrayOf()
        assertThrows(IllegalArgumentException::class.java) {
            codec.encrypt(byteArrayOf(), empty)
        }

        val tooLong = CharArray(1_025) { 'x' }
        assertThrows(IllegalArgumentException::class.java) {
            codec.encrypt(byteArrayOf(), tooLong)
        }
        assertTrue(tooLong.all { it == '\u0000' })

        val password = "password".toCharArray()
        assertThrows(IllegalArgumentException::class.java) {
            codec.encrypt(byteArrayOf(), password, EncryptedBackupCodec.MIN_ITERATIONS - 1)
        }
        assertTrue(password.all { it == '\u0000' })
    }
}
