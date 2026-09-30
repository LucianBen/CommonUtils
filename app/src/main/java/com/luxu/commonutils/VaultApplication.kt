package com.luxu.commonutils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.luxu.commonutils.data.VaultRepository
import com.luxu.commonutils.utils.CommonUtils

object VaultSession {
    var repository: VaultRepository? = null
        private set
    fun unlock(repository: VaultRepository) { lock(); this.repository = repository }
    fun lock() { val old = repository; repository = null; old?.close() }
}

class VaultApplication : Application(), Application.ActivityLifecycleCallbacks {
    private var started = 0
    private val handler = Handler(Looper.getMainLooper())
    private val lock = Runnable { if (started == 0) VaultSession.lock() }
    override fun onCreate() { super.onCreate(); registerActivityLifecycleCallbacks(this) }
    override fun onActivityStarted(activity: Activity) { started++; handler.removeCallbacks(lock) }
    override fun onActivityStopped(activity: Activity) {
        started--
        if (started == 0) handler.postDelayed(lock, 200)
    }
    override fun onActivityCreated(activity: Activity, state: Bundle?) {
        val appContext = applicationContext
        activity.window.decorView.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) CommonUtils.clearExpiredClipboard(appContext)
        }
    }
    override fun onActivityPostCreated(activity: Activity, state: Bundle?) {
        if (Build.VERSION.SDK_INT < 35) return
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val root = content.getChildAt(0) ?: return
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val left = root.paddingLeft
        val top = root.paddingTop
        val right = root.paddingRight
        val bottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(left + insets.left, top + insets.top,
                right + insets.right, bottom + insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(root)
    }
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
