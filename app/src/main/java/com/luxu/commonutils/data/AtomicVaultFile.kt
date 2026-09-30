package com.luxu.commonutils.data

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import com.luxu.commonutils.crypto.VaultCrypto

class AtomicVaultFile(private val file: File) {
    fun exists() = file.exists()

    fun read(): ByteArray {
        require(file.length() in 1..VaultCrypto.MAX_FILE_SIZE.toLong()) { "密码库文件长度无效" }
        return file.readBytes().also { require(it.size <= VaultCrypto.MAX_FILE_SIZE) }
    }

    /** A failed validation/write leaves the last committed file untouched. */
    fun write(bytes: ByteArray, validate: (ByteArray) -> Unit) {
        val temporary = File(file.parentFile, file.name + ".tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            validate(temporary.readBytes())
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING)
        } finally {
            temporary.delete()
        }
    }
}
