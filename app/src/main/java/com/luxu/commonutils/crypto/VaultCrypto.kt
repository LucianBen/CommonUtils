package com.luxu.commonutils.crypto

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Binary v1: magic/version, iterations, salt(16), nonce(12), GCM ciphertext/tag. */
object VaultCrypto {
    const val ITERATIONS = 600_000
    private const val MAGIC = 0x43555631
    private const val HEADER_SIZE = 36
    const val MAX_FILE_SIZE = 16 * 1024 * 1024
    private val random = SecureRandom()

    fun newSalt() = ByteArray(16).also(random::nextBytes)

    fun derive(password: CharArray, salt: ByteArray): ByteArray {
        require(password.isNotEmpty() && password.size <= 1024)
        require(salt.size == 16)
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally { spec.clearPassword() }
    }

    fun salt(envelope: ByteArray): ByteArray {
        require(envelope.size in (HEADER_SIZE + 16)..MAX_FILE_SIZE) { "密码库长度无效" }
        val header = ByteBuffer.wrap(envelope)
        require(header.int == MAGIC && header.int == ITERATIONS) { "不支持的密码库版本" }
        return ByteArray(16).also(header::get)
    }

    fun encrypt(plain: ByteArray, key: ByteArray, salt: ByteArray): ByteArray {
        require(key.size == 32 && salt.size == 16)
        require(plain.size <= MAX_FILE_SIZE - HEADER_SIZE - 16)
        val nonce = ByteArray(12).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_SIZE).putInt(MAGIC).putInt(ITERATIONS)
            .put(salt).put(nonce).array()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD("com.luxu.commonutils/vault".toByteArray(Charsets.UTF_8))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    fun decrypt(envelope: ByteArray, key: ByteArray): ByteArray {
        salt(envelope)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, envelope.copyOfRange(24, HEADER_SIZE)))
        cipher.updateAAD("com.luxu.commonutils/vault".toByteArray(Charsets.UTF_8))
        cipher.updateAAD(envelope.copyOfRange(0, HEADER_SIZE))
        return cipher.doFinal(envelope, HEADER_SIZE, envelope.size - HEADER_SIZE)
    }
}
