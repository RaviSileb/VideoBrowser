package com.ravisileb.videobrowser

import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserGuardTest {
    @Test
    fun cleanupScript_hides_fixed_or_large_overlays() {
        val script = BrowserGuard.cleanupScript()

        assertTrue(script.contains("getComputedStyle"))
        assertTrue(script.contains("position === 'fixed'"))
        assertTrue(script.contains("window.innerWidth"))
        assertTrue(script.contains("aria-modal"))
    }
}
