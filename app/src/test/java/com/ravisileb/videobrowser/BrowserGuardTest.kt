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

    @Test
    fun cleanupScript_keeps_main_content_nodes() {
        val script = BrowserGuard.cleanupScript()

        assertTrue(script.contains("isPageLikelyAdOverlayPage"))
        assertTrue(script.contains("tagName === 'BODY'"))
        assertTrue(script.contains("tagName === 'HTML'"))
        assertTrue(script.contains("tagName === 'MAIN'"))
    }

    @Test
    fun bombuj_media_pages_skip_cleanup_by_default() {
        val url = "https://serialy.bombuj.si/serial/silo-2023-3x7"

        assertTrue(!BrowserGuard.shouldRunAdCleanup(url, pageSafeMode = false, adCleanupDisabled = false))
        assertTrue(BrowserGuard.isBombujMediaPage(url))
    }

    @Test
    fun prehraj_media_pages_skip_cleanup_by_default() {
        val url = "https://prehraj.to/serial/silo"

        assertTrue(!BrowserGuard.shouldRunAdCleanup(url, pageSafeMode = false, adCleanupDisabled = false))
        assertTrue(BrowserGuard.isPrehrajMediaPage(url))
    }
}
