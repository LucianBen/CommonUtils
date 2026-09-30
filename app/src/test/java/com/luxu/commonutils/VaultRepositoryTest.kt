package com.luxu.commonutils

import com.luxu.commonutils.data.VaultCodec
import com.luxu.commonutils.data.VaultRepository
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import java.io.File

class VaultRepositoryTest {
    @get:Rule val temporary = TemporaryFolder()
    private var repository: VaultRepository? = null
    @After fun close() { repository?.close() }
    private fun open() = VaultRepository.unlock(temporary.root,
        "correct horse battery staple".toCharArray()).also { repository = it }
    private fun legacy(): File {
        val array = JSONArray()
        listOf("A", "B", "C").forEachIndexed { index, web ->
            array.put(JSONObject().put("id", 1754000000000L + index).put("webName", web)
                .put("account", "账户$web").put("password", "密码$web!?\n")
                .put("time", "2026-09-30").put("remark", "中文备注$web"))
        }
        return File(temporary.root, "need.json").also {
            it.writeText(JSONObject().put("passToken", array).toString())
        }
    }
    @Test fun migrationPreservesFieldsAndOriginalFileAcrossReopen() {
        val file = legacy(); val original = file.readBytes()
        val records = open().load()
        assertEquals(listOf("A", "B", "C"), records.map { it.webName })
        records.forEach {
            assertEquals("账户${it.webName}", it.account)
            assertEquals("密码${it.webName}!?\n", it.password)
            assertEquals("中文备注${it.webName}", it.remark)
            assertEquals("2026-09-30", it.time)
        }
        assertEquals(3, records.map { it.id }.distinct().size)
        assertArrayEquals(original, file.readBytes())
        repository!!.close()
        assertEquals(records, open().load())
    }
    @Test fun filteredEditDeleteAndRepeatedDeleteNeverTouchOtherRecords() {
        legacy(); val repo = open()
        val c = repo.load().single { it.webName == "C" }
        repo.save(c.id, "C edited", "account", "new secret", "notes")
        assertEquals(listOf("A", "B", "C edited"), repo.load().map { it.webName })
        assertEquals(c.id, repo.load().last().id)
        repo.delete(c.id)
        assertThrows(Exception::class.java) { repo.delete(c.id) }
        assertThrows(Exception::class.java) { repo.save(c.id, "gone", "user", "secret", "") }
        assertEquals(listOf("A", "B"), repo.load().map { it.webName })
    }
    @Test fun failedMigrationDoesNotSkipRecordsOrCreateVault() {
        val file = legacy(); val root = JSONObject(file.readText())
        root.getJSONArray("passToken").getJSONObject(1).remove("password")
        file.writeText(root.toString()); val original = file.readBytes()
        assertThrows(Exception::class.java) { open() }
        assertArrayEquals(original, file.readBytes())
        assertFalse(File(temporary.root, "vault.dat").exists())
    }
    @Test fun zeroByteLegacyFileIsNotTreatedAsAnEmptyVault() {
        val legacy = File(temporary.root, "need.json")
        legacy.writeBytes(byteArrayOf())
        assertThrows(Exception::class.java) { open() }
        assertTrue(legacy.exists())
        assertFalse(File(temporary.root, "vault.dat").exists())
    }
    @Test fun failedSavePreservesCommittedBytesAndRecords() {
        val repo = open(); repo.save(null, "A", "account", "secret", "")
        val original = File(temporary.root, "vault.dat").readBytes()
        check(File(temporary.root, "vault.dat.tmp").mkdir())
        assertThrows(Exception::class.java) { repo.delete(repo.load().single().id) }
        assertArrayEquals(original, File(temporary.root, "vault.dat").readBytes())
        assertEquals("A", repo.load().single().webName)
    }
    @Test fun wrongPasswordAndCorruptionFailWithoutFallbackToLegacy() {
        legacy(); val repo = open(); repo.close()
        val file = File(temporary.root, "vault.dat"); val original = file.readBytes()
        assertThrows(Exception::class.java) {
            VaultRepository.unlock(temporary.root, "wrong password".toCharArray())
        }
        assertArrayEquals(original, file.readBytes())
        original[original.lastIndex] = (original.last().toInt() xor 1).toByte()
        file.writeBytes(original)
        assertThrows(Exception::class.java) { open() }
        assertArrayEquals(original, file.readBytes())
    }
    @Test fun closedSessionCannotReadOrWrite() {
        val repo = open(); repo.close()
        assertThrows(Exception::class.java) { repo.load() }
        assertThrows(Exception::class.java) { repo.save(null, "A", "account", "secret", "") }
    }
    @Test fun repeatedNewRecordSubmissionIsIdempotent() {
        val repo = open()
        repo.save(null, "A", "account", "secret", "", "stable-draft-id")
        repo.save(null, "A", "account", "secret", "", "stable-draft-id")
        assertEquals(1, repo.load().size)
        assertThrows(Exception::class.java) {
            repo.save(null, "changed", "account", "secret", "", "stable-draft-id")
        }
        assertEquals("A", repo.load().single().webName)
    }
    @Test fun strictCodecRejectsDuplicateIdsWrongTypesAndInvalidUtf8() {
        val entries = listOf(PasswordBean("same", "A", "user", "secret", "time", ""),
            PasswordBean("same", "B", "user", "secret", "time", ""))
        assertThrows(Exception::class.java) { VaultCodec.decode(VaultCodec.encode(entries)) }
        val root = JSONObject(VaultCodec.encode(entries.take(1)).toString(Charsets.UTF_8))
        root.getJSONArray("passToken").getJSONObject(0).put("password", 123)
        assertThrows(Exception::class.java) { VaultCodec.decode(root.toString().toByteArray()) }
        assertThrows(Exception::class.java) { VaultCodec.decode(byteArrayOf(0xc3.toByte(), 0x28)) }
        assertThrows(Exception::class.java) {
            VaultCodec.decode(VaultCodec.encode(entries.take(1)) + "trailing garbage".toByteArray())
        }
    }
}
