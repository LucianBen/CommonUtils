package com.luxu.commonutils.data

import com.luxu.commonutils.PasswordBean
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

object VaultCodec {
    fun decode(bytes: ByteArray, legacy: Boolean = false): List<PasswordBean> {
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        val parser = JSONTokener(text)
        val root = parser.nextValue() as? JSONObject ?: error("密码库根对象无效")
        require(parser.nextClean() == '\u0000') { "密码库包含多余内容" }
        val array = root.getJSONArray("passToken")
        val records = (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            if (legacy) require(obj.get("id") is Number) { "旧记录 ID 无效" }
            fun field(name: String): String = (obj.get(name) as? String)
                ?: error("密码记录字段无效")
            PasswordBean(if (legacy) UUID.randomUUID().toString() else field("id"),
                field("webName"), field("account"), field("password"), field("time"), field("remark"))
        }
        require(records.all { it.id.isNotBlank() } && records.map { it.id }.distinct().size == records.size) {
            "密码库包含重复或空 ID"
        }
        return records
    }

    fun encode(records: List<PasswordBean>): ByteArray {
        val array = JSONArray()
        records.forEach { record ->
            array.put(JSONObject().put("id", record.id).put("webName", record.webName)
                .put("account", record.account).put("password", record.password)
                .put("time", record.time).put("remark", record.remark))
        }
        return JSONObject().put("passToken", array).toString().toByteArray(Charsets.UTF_8)
    }
}
