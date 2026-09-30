package com.luxu.commonutils

import android.content.ClipDescription
import com.luxu.commonutils.utils.CommonUtils
import org.junit.Assert.*
import org.junit.Test

class PasswordClipTest {
    @Test fun passwordClipHasSensitiveFlagAndUniqueOwnershipMetadata() {
        // Test ClipData only. Do not read or modify the device's real clipboard.
        val clip = CommonUtils.passwordClip("test-only-password", "copy-token")
        assertTrue(clip.description.extras!!.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE))
        assertEquals("copy-token", clip.description.extras!!.getString(CommonUtils.COPY_TOKEN))
        assertEquals(1, clip.itemCount)
        assertEquals("test-only-password", clip.getItemAt(0).text.toString())
    }
}
