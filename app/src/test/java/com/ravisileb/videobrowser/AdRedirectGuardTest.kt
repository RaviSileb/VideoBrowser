package com.ravisileb.videobrowser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRedirectGuardTest {
    @Test
    fun shouldRecoverQuickExternalRedirect() {
        val guard = AdRedirectGuard(4000L)
        guard.markPage("https://example.com/video/watch?id=123")

        assertTrue(guard.shouldRecoverRedirect("https://ads.doubleclick.net/ads?foo=bar", 1500L))
    }

    @Test
    fun shouldNotRecoverWhenRedirectTakesTooLong() {
        val guard = AdRedirectGuard(2000L)
        guard.markPage("https://example.com/video/watch?id=123")

        assertFalse(guard.shouldRecoverRedirect("https://ads.doubleclick.net/ads?foo=bar", 5000L))
    }

    @Test
    fun shouldNotRecoverWhenHostStaysTheSame() {
        val guard = AdRedirectGuard(4000L)
        guard.markPage("https://example.com/video/watch?id=123")

        assertFalse(guard.shouldRecoverRedirect("https://example.com/other-page", 1500L))
    }
}
