package com.luxu.commonutils

import com.luxu.commonutils.data.AtomicVaultFile
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AtomicVaultFileTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun failedValidationPreservesCommittedBytes() {
        val file = temporary.newFile("vault.dat")
        file.writeBytes(byteArrayOf(1, 2, 3))
        val store = AtomicVaultFile(file)
        assertThrows(Exception::class.java) {
            store.write(byteArrayOf(4, 5)) { error("simulated validation failure") }
        }
        assertArrayEquals(byteArrayOf(1, 2, 3), store.read())
        assertFalse(java.io.File(file.parentFile, "vault.dat.tmp").exists())
    }
    @Test fun successfulWriteReplacesFileAfterVerification() {
        val file = temporary.newFile("vault.dat")
        file.writeBytes(byteArrayOf(1, 2, 3))
        val store = AtomicVaultFile(file)
        store.write(byteArrayOf(4, 5)) {
            assertArrayEquals(byteArrayOf(1, 2, 3), file.readBytes())
            assertArrayEquals(byteArrayOf(4, 5), it)
        }
        assertArrayEquals(byteArrayOf(4, 5), store.read())
    }
    @Test fun failedInitialWriteDoesNotCreateAnEmptyVault() {
        val file = java.io.File(temporary.root, "vault.dat")
        assertThrows(Exception::class.java) { AtomicVaultFile(file).write(byteArrayOf(4)) { error("failure") } }
        assertFalse(file.exists())
    }
}
