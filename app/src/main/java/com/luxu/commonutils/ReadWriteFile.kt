package com.luxu.commonutils

import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


object ReadWriteFile {
    private lateinit var filePath: String
    private lateinit var jsonArray: JSONArray

    /*
    *   读取已经存在的json数据
    * */
    fun readJsonData(filePath: String): ArrayList<PasswordBean> {
        this.filePath = filePath
        var br: BufferedReader? = null
        try {
            br = BufferedReader(InputStreamReader(FileInputStream(File(filePath))))
            var line: String?
            val sb = StringBuilder()
            while (br.readLine().also { line = it } != null) {
                sb.append(line)
            }
            br.close()

            val json = sb.toString()
            jsonArray = JSONObject(json).getJSONArray("passToken")
            val pbList: ArrayList<PasswordBean> = arrayListOf()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                try {
                    pbList.add(
                        PasswordBean(
                            obj.getInt("id"),
                            obj.getString("webName"),
                            obj.getString("account"),
                            obj.getString("password"),
                            obj.getString("time"),
                            obj.getString("remark")
                        )
                    )
                }catch (e:Exception){}

            }
            return pbList
        } catch (e: Exception) {
//            println(e)
        } finally {
            br?.close()
        }
        return ArrayList()
    }

    /*
    *   写入新的json数据
    * */
    fun writeNewJsonData(webName: String, account: String, password: String, remark: String) {
        //获取时间戳
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val time = current.format(formatter)
        val curTime = ChronoUnit.NANOS.between(Instant.EPOCH, Instant.now()) / 1000000

        // 读取现有 JSON 文件内容
        checkFileExist(filePath)
        var existingContent = File(filePath).readText()
        if (TextUtils.isEmpty(existingContent)) existingContent = "{}"
        // 将现有 JSON 内容解析为 JSONObject
        val jsonObject = JSONObject(existingContent)
        // 如果 "passToken" 字段不存在，则创建一个空的 JSONArray
        val passToken = jsonObject.optJSONArray("passToken") ?: JSONArray()

        // 创建要添加的新 JSON 对象
        val newJsonObject = JSONObject()
        newJsonObject.put("id", curTime)
        newJsonObject.put("webName", webName)
        newJsonObject.put("account", account)
        newJsonObject.put("password", password)
        newJsonObject.put("time", time)
        newJsonObject.put("remark", remark)

        passToken.put(newJsonObject)
        jsonObject.put("passToken", passToken)

        write2File(filePath, jsonObject.toString())

    }

    /*
    *   修改json数据
    * */
    fun changeJsonData(
        pos: Int,
        webName: String, account: String,
        password: String, remark: String
    ) {
        val curTime = ChronoUnit.NANOS.between(Instant.EPOCH, Instant.now()) / 1000000
        // 将更新后的 JSON 对象写回到文件
        jsonArray.getJSONObject(pos)
            .put("id", curTime)
            .put("webName", webName)
            .put("account", account)
            .put("password", password)
            .put("remark", remark)

        val newJsonObject = JSONObject()
        newJsonObject.put("passToken", jsonArray)

        // 将更新后的 JSON 对象写回到文件
        write2File(filePath, newJsonObject.toString())

    }

    /*
     *   删除json数据
     * */
    fun deleteJsonData(pos: Int) {
        jsonArray.remove(pos)

        val newJsonObject = JSONObject()
        newJsonObject.put("passToken", jsonArray)

        // 将更新后的 JSON 对象写回到文件
        write2File(filePath, newJsonObject.toString())

    }

    fun checkFileExist(path: String) {
        try {
            val offsetFile = File(path)
            //判断路径是否存在，不存在则创建
            if (!offsetFile.parentFile?.exists()!!) {
                //创建父路径
                offsetFile.parentFile?.mkdirs()
            }
            //判断文件是否存在
            if (!offsetFile.exists()) {
                offsetFile.createNewFile()
            }
        } catch (_: IOException) {

        }

    }

    fun write2File(filePath: String, text: String) {
        FileWriter(filePath).use { fileWriter ->
            fileWriter.write(text)
        }

    }

}