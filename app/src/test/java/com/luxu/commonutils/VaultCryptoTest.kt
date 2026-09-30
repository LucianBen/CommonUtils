package com.luxu.commonutils

import com.luxu.commonutils.crypto.VaultCrypto
import org.junit.Assert.*
import org.junit.Test

class VaultCryptoTest {
    private val key = ByteArray(32) { it.toByte() }
    private val salt = ByteArray(16) { (it + 10).toByte() }
    @Test fun roundTripUsesFreshNonceAndHidesPlaintext() {
        val plain = "账户: test@example.com;密码: SecretPassword".toByteArray()
        val first = VaultCrypto.encrypt(plain, key, salt)
        val second = VaultCrypto.encrypt(plain, key, salt)
        assertFalse(first.contentEquals(second))
        assertArrayEquals(plain, VaultCrypto.decrypt(first, key))
        assertArrayEquals(salt, VaultCrypto.salt(first))
        assertFalse(first.toString(Charsets.UTF_8).contains("SecretPassword"))
    }
    @Test fun everyHeaderAndCiphertextByteIsAuthenticated() {
        val envelope = VaultCrypto.encrypt("secret".toByteArray(), key, salt)
        envelope.indices.forEach { index ->
            val tampered = envelope.clone()
            tampered[index] = (tampered[index].toInt() xor 1).toByte()
            assertThrows(Exception::class.java) { VaultCrypto.decrypt(tampered, key) }
        }
    }
    @Test fun wrongKeyAndTruncatedEnvelopeAreRejected() {
        val envelope = VaultCrypto.encrypt("secret".toByteArray(), key, salt)
        assertThrows(Exception::class.java) { VaultCrypto.decrypt(envelope, ByteArray(32)) }
        assertThrows(Exception::class.java) { VaultCrypto.decrypt(envelope.copyOf(20), key) }
    }
    @Test fun passwordDerivationIsRepeatableAndSalted() {
        val password = "correct horse battery staple".toCharArray()
        val first = VaultCrypto.derive(password, salt)
        assertEquals(32, first.size)
        assertArrayEquals(first, VaultCrypto.derive(password, salt))
        assertFalse(first.contentEquals(VaultCrypto.derive(password, VaultCrypto.newSalt())))
        password.fill('\u0000'); first.fill(0)
    }
}
