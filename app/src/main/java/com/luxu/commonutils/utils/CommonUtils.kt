package com.luxu.commonutils.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.os.PersistableBundle
import android.os.SystemClock
import android.widget.Toast
import java.util.UUID

object CommonUtils {
    internal const val COPY_TOKEN = "com.luxu.commonutils.COPY_TOKEN"
    private val cleanup = ClipboardCleanup()
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var clearTask: Runnable? = null

    fun getRandomPassword(): String = PasswordGenerator.generate()

    internal fun passwordClip(info: String, token: String): ClipData =
        ClipData.newPlainText("密码", info).apply {
            description.extras = PersistableBundle().apply {
                val sensitiveKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    ClipDescription.EXTRA_IS_SENSITIVE else "android.content.extra.IS_SENSITIVE"
                putBoolean(sensitiveKey, true)
                putString(COPY_TOKEN, token)
            }
        }

    // 复制至粘贴板
    fun copyToClipboard(context: Context, info: String) {
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val token = UUID.randomUUID().toString()
        try {
            cm.setPrimaryClip(passwordClip(info, token))
            val description = cm.primaryClipDescription
            // Use the system-assigned timestamp to distinguish a later re-copy.
            if (description?.extras?.getString(COPY_TOKEN) != token) {
                Toast.makeText(context, "复制未确认，请重试。", Toast.LENGTH_SHORT).show()
                return
            }
            cleanup.track(ClipboardIdentity(token, description.timestamp), SystemClock.elapsedRealtime())
            clearTask?.let(handler::removeCallbacks)
            clearTask = Runnable { clearExpiredClipboard(appContext) }.also {
                handler.postDelayed(it, ClipboardCleanup.DELAY_MS)
            }
            Toast.makeText(context, "已复制，30 秒后尝试清除。", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, "无法访问剪贴板，请重试。", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearExpiredClipboard(context: Context) {
        val cm = context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        try {
            cleanup.clearExpired(SystemClock.elapsedRealtime(), read = {
                cm.primaryClipDescription?.let {
                    ClipboardIdentity(it.extras?.getString(COPY_TOKEN), it.timestamp)
                }
            }, clear = { cm.clearPrimaryClip() })
        } catch (_: SecurityException) {
            // Retain metadata and retry on focus; never clear content without checking it.
        }
    }


}
