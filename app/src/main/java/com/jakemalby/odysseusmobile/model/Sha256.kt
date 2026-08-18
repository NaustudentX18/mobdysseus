package com.jakemalby.odysseusmobile.model

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Streaming SHA-256 helpers. No model bytes are loaded wholly into memory. */
object Sha256 {
    private val CANONICAL_PATTERN = Regex("[0-9a-f]{64}")

    fun isCanonical(value: String): Boolean = value.matches(CANONICAL_PATTERN)

    fun of(file: File): String = file.inputStream().use(::of)

    fun of(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    fun matches(file: File, expected: String): Boolean =
        isCanonical(expected) && of(file) == expected
}
