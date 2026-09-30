package com.luxu.commonutils.data

import com.luxu.commonutils.PasswordBean
import com.luxu.commonutils.crypto.VaultCrypto
import com.luxu.commonutils.crypto.DeviceVaultCrypto
import javax.crypto.SecretKey
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

/** No global JSON or positional CRUD. */
class VaultRepository private constructor(
    private val store: AtomicVaultFile,
    private val key: ByteArray,
    private val salt: ByteArray,
    private var deviceKey: SecretKey? = null
) {
    private var closed = false

    @Synchronized fun close() { closed = true; key.fill(0); deviceKey = null }

    private fun decrypt(bytes: ByteArray) = deviceKey?.let { DeviceVaultCrypto.decrypt(bytes, it) }
        ?: VaultCrypto.decrypt(bytes, key)

    @Synchronized fun load(): List<PasswordBean> {
        check(!closed) { "密码库已锁定" }
        val plain = decrypt(store.read())
        return try { VaultCodec.decode(plain) } finally { plain.fill(0) }
    }

    @Synchronized fun save(id: String?, web: String, account: String, password: String, remark: String,
        newRecordId: String = UUID.randomUUID().toString()) {
        val current = load()
        if (id == null) {
            require(newRecordId.isNotBlank())
            val previous = current.singleOrNull { it.id == newRecordId }
            if (previous != null) {
                require(previous.webName == web && previous.account == account &&
                    previous.password == password && previous.remark == remark) {
                    "记录已保存，请返回列表编辑"
                }
                return
            }
        }
        val next = if (id == null) current + PasswordBean(newRecordId, web,
            account, password, LocalDateTime.now().toString(), remark)
        else {
            require(current.any { it.id == id }) { "记录已不存在，请返回刷新" }
            current.map { if (it.id == id) it.copy(webName = web, account = account,
                password = password, remark = remark) else it }
        }
        commit(next)
    }

    @Synchronized fun delete(id: String) {
        val current = load()
        require(current.any { it.id == id }) { "记录已不存在，请刷新" }
        commit(current.filterNot { it.id == id })
    }

    private fun commit(records: List<PasswordBean>) {
        check(!closed) { "密码库已锁定" }
        val plain = VaultCodec.encode(records)
        val encrypted = try {
            deviceKey?.let { DeviceVaultCrypto.encrypt(plain, it) }
                ?: VaultCrypto.encrypt(plain, key, salt)
        } finally { plain.fill(0) }
        store.write(encrypted) { candidate ->
            val decoded = decrypt(candidate)
            try { check(VaultCodec.decode(decoded) == records) { "写入验证失败" } }
            finally { decoded.fill(0) }
        }
    }

    companion object {
        @Synchronized fun openDevice(directory: File, keyProvider: (Boolean) -> SecretKey): VaultRepository {
            val store = AtomicVaultFile(File(directory, "vault-device.dat"))
            val existing = store.exists()
            check(existing || !exists(directory)) { "发现旧主密码加密库，需要单独迁移；原文件保留" }
            val legacy = File(directory, "need.json")
            val records = if (!existing && legacy.exists()) {
                require(legacy.length() in 1..VaultCrypto.MAX_FILE_SIZE.toLong()) {
                    "旧密码库为空或过大，原文件保留"
                }
                val bytes = legacy.readBytes()
                try { VaultCodec.decode(bytes, legacy = true) } finally { bytes.fill(0) }
            } else emptyList()
            val repository = VaultRepository(store, ByteArray(0), ByteArray(0), keyProvider(!existing))
            try {
                if (existing) repository.load()
                else {
                    repository.commit(records)
                    check(repository.load() == records) { "迁移验证失败" }
                }
                return repository
            } catch (failure: Exception) { repository.close(); throw failure }
        }

        fun exists(directory: File) = File(directory, "vault.dat").exists()

        @Synchronized fun unlock(directory: File, password: CharArray): VaultRepository {
            val store = AtomicVaultFile(File(directory, "vault.dat"))
            val envelope = if (store.exists()) store.read() else null
            val salt = envelope?.let(VaultCrypto::salt) ?: VaultCrypto.newSalt()
            val key = VaultCrypto.derive(password, salt)
            val repository = VaultRepository(store, key, salt)
            try {
                if (envelope != null) repository.load()
                else {
                    val legacy = File(directory, "need.json")
                    val records = if (legacy.exists()) {
                        require(legacy.length() in 1..VaultCrypto.MAX_FILE_SIZE.toLong()) {
                            "旧密码库为空或过大，请检查原文件"
                        }
                        val bytes = legacy.readBytes()
                        try { VaultCodec.decode(bytes, legacy = true) } finally { bytes.fill(0) }
                    } else emptyList()
                    repository.commit(records)
                    check(repository.load() == records) { "迁移验证失败" }
                }
                return repository
            } catch (failure: Exception) {
                repository.close()
                throw failure
            }
        }
    }
}
