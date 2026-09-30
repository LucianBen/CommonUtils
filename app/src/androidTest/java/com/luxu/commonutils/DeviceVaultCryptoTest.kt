package com.luxu.commonutils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.luxu.commonutils.crypto.DeviceVaultCrypto
import java.security.KeyStore
import java.util.UUID
import javax.crypto.KeyGenerator
import org.junit.Assert.*
import org.junit.Test

class DeviceVaultCryptoTest {
    @Test fun keystoreKeyWorksWithoutAuthentication() {
        val alias = "review-test-${UUID.randomUUID()}"
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        try {
            val key = KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false).build())
            }.generateKey()
            assertNull(key.encoded)
            val plain = "test fixture".toByteArray()
            assertArrayEquals(plain, DeviceVaultCrypto.decrypt(DeviceVaultCrypto.encrypt(plain, key), key))
        } finally { if (store.containsAlias(alias)) store.deleteEntry(alias) }
    }
}
