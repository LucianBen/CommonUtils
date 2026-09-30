package com.luxu.commonutils.crypto

import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object DeviceVaultCrypto {
    private const val MAGIC = 0x43555632
    private const val HEADER_SIZE = 16

    fun encrypt(plain: ByteArray, key: SecretKey): ByteArray {
        require(plain.size <= VaultCrypto.MAX_FILE_SIZE - HEADER_SIZE - 16)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        require(cipher.iv.size == 12)
        val header = ByteBuffer.allocate(HEADER_SIZE).putInt(MAGIC).put(cipher.iv).array()
        cipher.updateAAD("com.luxu.commonutils/device-vault".toByteArray(Charsets.UTF_8))
        cipher.updateAAD(header)
        return header + cipher.doFinal(plain)
    }

    fun decrypt(envelope: ByteArray, key: SecretKey): ByteArray {
        require(envelope.size in (HEADER_SIZE + 16)..VaultCrypto.MAX_FILE_SIZE)
        require(ByteBuffer.wrap(envelope).int == MAGIC) { "不支持的本机密码库格式" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key,
            GCMParameterSpec(128, envelope.copyOfRange(4, HEADER_SIZE)))
        cipher.updateAAD("com.luxu.commonutils/device-vault".toByteArray(Charsets.UTF_8))
        cipher.updateAAD(envelope.copyOfRange(0, HEADER_SIZE))
        return cipher.doFinal(envelope, HEADER_SIZE, envelope.size - HEADER_SIZE)
    }
}
