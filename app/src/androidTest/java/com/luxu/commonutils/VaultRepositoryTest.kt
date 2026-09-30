package com.luxu.commonutils

import androidx.test.platform.app.InstrumentationRegistry
import com.luxu.commonutils.data.VaultRepository
import com.luxu.commonutils.data.VaultCodec
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import java.io.File
import java.util.UUID

class VaultRepositoryTest {
    private lateinit var directory: File
    private var repository: VaultRepository? = null
    private val password get() = "correct horse battery staple".toCharArray()
    @Before fun prepare() {
        // Unique cache subdirectory; never use filesDir or the user's password files.
        directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "vault-test-${UUID.randomUUID()}")
        check(directory.mkdirs())
    }
    @After fun cleanup() { repository?.close(); directory.deleteRecursively() }
    private fun unlock(): VaultRepository = VaultRepository.unlock(directory, password).also { repository = it }
    private fun legacy(): File {
        val array = JSONArray()
        listOf("A", "B", "C").forEachIndexed { index, web ->
            array.put(JSONObject().put("id", 1754000000000L + index).put("webName", web)
                .put("account", "账户$web").put("password", "密码$web!?\n")
                .put("time", "2026-09-30").put("remark", "中文备注$web"))
        }
        return File(directory, "need.json").also { it.writeText(JSONObject().put("passToken", array).toString()) }
    }
    @Test fun legacyMigrationRetainsOriginalAndEveryField() {
        val legacy = legacy(); val original = legacy.readBytes()
        val records = unlock().load()
        assertEquals(listOf("A", "B", "C"), records.map { it.webName })
        records.forEach { assertEquals("密码${it.webName}!?\n", it.password); assertEquals("中文备注${it.webName}", it.remark) }
        assertArrayEquals(original, legacy.readBytes())
        assertFalse(File(directory, "vault.dat").readText().contains("账户"))
        repository!!.close()
        assertEquals(records, unlock().load())
    }
    @Test fun filteredEditAndDeleteUseIdentityAndNeverResurrect() {
        legacy(); val repo = unlock()
        val c = repo.load().single { it.webName == "C" }
        repo.save(c.id, "C edited", "account", "new secret", "notes")
        assertEquals(listOf("A", "B", "C edited"), repo.load().map { it.webName })
        assertEquals(c.id, repo.load().last().id)
        repo.delete(c.id)
        assertEquals(listOf("A", "B"), repo.load().map { it.webName })
        assertThrows(Exception::class.java) { repo.delete(c.id) }
        assertEquals(listOf("A", "B"), repo.load().map { it.webName })
    }
    @Test fun malformedLegacyAbortsWithoutCreatingVault() {
        val file = legacy()
        val root = JSONObject(file.readText())
        root.getJSONArray("passToken").getJSONObject(1).remove("password")
        file.writeText(root.toString()); val original = file.readBytes()
        assertThrows(Exception::class.java) { unlock() }
        assertArrayEquals(original, file.readBytes())
        assertFalse(File(directory, "vault.dat").exists())
    }
    @Test fun wrongPasswordAndTamperingCannotOverwriteVault() {
        val repo = unlock(); repo.save(null, "A", "account", "secret", "")
        repo.close()
        val file = File(directory, "vault.dat"); val original = file.readBytes()
        assertThrows(Exception::class.java) { VaultRepository.unlock(directory, "wrong password".toCharArray()) }
        assertArrayEquals(original, file.readBytes())
        original[original.lastIndex] = (original.last().toInt() xor 1).toByte()
        file.writeBytes(original)
        assertThrows(Exception::class.java) { unlock() }
        assertArrayEquals(original, file.readBytes())
    }
    @Test fun closedSessionRejectsReadsAndWrites() {
        val repo = unlock(); repo.close()
        assertThrows(Exception::class.java) { repo.load() }
        assertThrows(Exception::class.java) { repo.save(null, "A", "account", "secret", "") }
    }
    @Test fun duplicateIdsAndWrongFieldTypesFailStrictly() {
        val entries = listOf(PasswordBean("same", "A", "account", "secret", "time", ""),
            PasswordBean("same", "B", "account", "secret", "time", ""))
        assertThrows(Exception::class.java) { VaultCodec.decode(VaultCodec.encode(entries)) }
        val root = JSONObject(VaultCodec.encode(entries.take(1)).toString(Charsets.UTF_8))
        root.getJSONArray("passToken").getJSONObject(0).put("password", 123)
        assertThrows(Exception::class.java) { VaultCodec.decode(root.toString().toByteArray()) }
    }
    @Test fun failedWriteKeepsPreviousVaultAndRecords() {
        val repo = unlock(); repo.save(null, "A", "account", "secret", "")
        val original = File(directory, "vault.dat").readBytes()
        // Simulate inability to open the staging file.
        check(File(directory, "vault.dat.tmp").mkdir())
        assertThrows(Exception::class.java) { repo.delete(repo.load().single().id) }
        assertArrayEquals(original, File(directory, "vault.dat").readBytes())
        assertEquals("A", repo.load().single().webName)
    }
}
