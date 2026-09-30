package com.luxu.commonutils.base

import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.WindowManager
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.luxu.commonutils.utils.KeyboardUtils
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

open class BaseActivity : AppCompatActivity() {
    protected val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    protected fun showError(message: String) {
        if (!isFinishing && !isDestroyed) AlertDialog.Builder(this).setMessage(message)
            .setPositiveButton("确定", null).show()
    }
    override fun onDestroy() { uiScope.cancel(); super.onDestroy() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars = true
    }
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        //获取当前获得焦点的View
        //获取当前获得焦点的View
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            //调用方法判断是否需要隐藏键盘
            KeyboardUtils.hideKeyboard(ev, view, this)
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
