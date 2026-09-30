package com.luxu.commonutils.utils

internal data class ClipboardIdentity(val token: String?, val timestamp: Long)

/** Main-thread state. Keep only copy metadata, never retain the copied password. */
internal class ClipboardCleanup {
    companion object { const val DELAY_MS = 30_000L }
    private data class Pending(val identity: ClipboardIdentity, val deadline: Long)
    private var pending: Pending? = null

    fun track(identity: ClipboardIdentity, now: Long) {
        require(!identity.token.isNullOrEmpty())
        pending = Pending(identity, now + DELAY_MS)
    }

    fun clearExpired(now: Long, read: () -> ClipboardIdentity?, clear: () -> Unit) {
        val copy = pending ?: return
        if (now < copy.deadline) return
        // Null may mean background access was denied. Retry once the app has focus.
        val current = read() ?: return
        if (current == copy.identity) clear()
        // A callback during clear must not forget a newly copied password.
        if (pending === copy) pending = null
    }
}
