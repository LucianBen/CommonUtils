package com.luxu.commonutils

import com.luxu.commonutils.utils.ClipboardCleanup
import com.luxu.commonutils.utils.ClipboardIdentity
import org.junit.Assert.*
import org.junit.Test

class ClipboardCleanupTest {
    private val original = ClipboardIdentity("first-copy", 100)
    @Test fun clearsOnlyAtOrAfterDeadline() {
        val cleanup = ClipboardCleanup(); cleanup.track(original, 1000)
        var clears = 0
        cleanup.clearExpired(30_999, { error("Must not read before expiry") }, { clears++ })
        assertEquals(0, clears)
        cleanup.clearExpired(31_000, { original }, { clears++ })
        cleanup.clearExpired(32_000, { original }, { clears++ })
        assertEquals(1, clears)
    }
    @Test fun newerForeignContentMustSurvive() {
        val cleanup = ClipboardCleanup(); cleanup.track(original, 0)
        cleanup.clearExpired(30_000, { ClipboardIdentity(null, 200) }, { error("Foreign content cleared") })
        cleanup.clearExpired(31_000, { original }, { error("Forgotten copy should not clear anything") })
    }
    @Test fun reCopiedContentWithSameMarkerButNewTimestampMustSurvive() {
        val cleanup = ClipboardCleanup(); cleanup.track(original, 0)
        cleanup.clearExpired(30_000, { original.copy(timestamp = 200) }, { error("Later copy cleared") })
    }
    @Test fun unavailableClipboardIsRetriedWhenFocusReturns() {
        val cleanup = ClipboardCleanup(); cleanup.track(original, 0)
        var clears = 0
        cleanup.clearExpired(30_000, { null }, { error("Cannot blindly clear") })
        cleanup.clearExpired(40_000, { original }, { clears++ })
        assertEquals(1, clears)
    }
    @Test fun secondPasswordCopyRestartsExpiryAndOldTimerDoesNotClearIt() {
        val cleanup = ClipboardCleanup(); cleanup.track(original, 0)
        val newer = ClipboardIdentity("second-copy", 200)
        cleanup.track(newer, 20_000)
        cleanup.clearExpired(30_000, { newer }, { error("Old timer cleared new copy") })
        var clears = 0
        cleanup.clearExpired(50_000, { newer }, { clears++ })
        assertEquals(1, clears)
    }
    @Test fun failedClearCanBeRetried() {
        val cleanup = ClipboardCleanup(); cleanup.track(original, 0)
        assertThrows(SecurityException::class.java) {
            cleanup.clearExpired(30_000, { original }, { throw SecurityException("denied") })
        }
        var clears = 0
        cleanup.clearExpired(40_000, { original }, { clears++ })
        assertEquals(1, clears)
    }
    @Test fun newCopyDuringClearIsNotForgotten() {
        val cleanup = ClipboardCleanup(); cleanup.track(original, 0)
        val newer = ClipboardIdentity("second-copy", 200)
        cleanup.clearExpired(30_000, { original }, { cleanup.track(newer, 30_000) })
        var clears = 0
        cleanup.clearExpired(60_000, { newer }, { clears++ })
        assertEquals(1, clears)
    }
}
