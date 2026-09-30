package com.luxu.commonutils

import com.luxu.commonutils.crypto.DeviceVaultCrypto
import com.luxu.commonutils.data.VaultRepository
import java.io.File
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DeviceVaultTest {
    @get:Rule val folder = TemporaryFolder()
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

    @Test fun roundTripUsesFreshNonceAndRejectsTampering() {
        val plain = "test data".toByteArray()
        val first = DeviceVaultCrypto.encrypt(plain, key)
        assertArrayEquals(plain, DeviceVaultCrypto.decrypt(first, key))
        assertFalse(first.contentEquals(DeviceVaultCrypto.encrypt(plain, key)))
        for (index in listOf(0, 4, first.lastIndex)) {
            val bad = first.clone().apply { this[index] = (this[index].toInt() xor 1).toByte() }
            assertThrows(Exception::class.java) { DeviceVaultCrypto.decrypt(bad, key) }
        }
    }

    @Test fun autoOpenRetainsLegacyAndPersistsStableIdChanges() {
        val directory = folder.newFolder()
        val legacy = File(directory, "need.json")
        legacy.writeText("""{"passToken":[{"id":1,"webName":"site","account":"account","password":"secret","time":"time","remark":"remark"}]}""")
        val original = legacy.readBytes()
        val repository = VaultRepository.openDevice(directory) { allowed -> assertTrue(allowed); key }
        val record = repository.load().single()
        repository.save(record.id, "updated", record.account, record.password, record.remark)
        repository.close()
        val reopened = VaultRepository.openDevice(directory) { allowed -> assertFalse(allowed); key }
        assertEquals("updated", reopened.load().single().webName)
        reopened.delete(record.id)
        assertTrue(reopened.load().isEmpty())
        reopened.close()
        assertArrayEquals(original, legacy.readBytes())
        assertFalse(File(directory, "vault.dat").exists())
    }

    @Test fun badLegacyOrExistingMasterVaultNeverCreatesDeviceVault() {
        val directory = folder.newFolder()
        File(directory, "need.json").writeText("broken")
        assertThrows(Exception::class.java) {
            VaultRepository.openDevice(directory) { fail("must not request key"); key }
        }
        File(directory, "vault.dat").writeBytes(byteArrayOf(1, 2, 3))
        assertThrows(Exception::class.java) {
            VaultRepository.openDevice(directory) { fail("must not request key"); key }
        }
        assertFalse(File(directory, "vault-device.dat").exists())
        assertArrayEquals(byteArrayOf(1, 2, 3), File(directory, "vault.dat").readBytes())
    }

    @Test fun missingOrWrongKeyNeverOverwritesExistingFile() {
        val directory = folder.newFolder()
        VaultRepository.openDevice(directory) { key }.close()
        val file = File(directory, "vault-device.dat")
        val original = file.readBytes()
        assertThrows(Exception::class.java) {
            VaultRepository.openDevice(directory) { allowed -> assertFalse(allowed); error("missing") }
        }
        assertThrows(Exception::class.java) {
            VaultRepository.openDevice(directory) { SecretKeySpec(ByteArray(32), "AES") }
        }
        assertArrayEquals(original, file.readBytes())
    }
}
