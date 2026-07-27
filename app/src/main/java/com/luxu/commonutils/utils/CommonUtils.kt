package com.luxu.commonutils.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.media.VolumeShaper.Operation
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlin.random.Random.Default.nextInt

object CommonUtils {
    // 随机数生成，默认10个
    private var availableChars =
        "abcdefghij!klmnopqrst!uvwxyz0123456789!ABCDEFGHIJ!KLMNOPQRST!UVWXYZ0123456789!"
//        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789~!@#$%^&*()-+_=,."

    private fun getRandom(len: Int) = nextInt(0, len)

    fun getRandomPassword(): String {
        val sb = StringBuilder()
        val len = availableChars.length
        val passwordLength = 10
        for (i in 0 until passwordLength) {
            sb.append(availableChars[getRandom(len)])
        }
        return sb.toString()
    }

    // 复制至粘贴板
    fun copyToClipboard(context: Context, info: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val mClipData = ClipData.newPlainText("", info)
        cm.setPrimaryClip(mClipData)

        Toast.makeText(context, "复制成功！", Toast.LENGTH_SHORT).show()
    }


}