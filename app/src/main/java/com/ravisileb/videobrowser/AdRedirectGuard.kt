package com.ravisileb.videobrowser

import java.net.URI

class AdRedirectGuard(private val maxRedirectDelayMs: Long) {
    private var lastSafePage: String? = null

    fun markPage(url: String) {
        lastSafePage = url
    }

    fun shouldRecoverRedirect(url: String, elapsedMs: Long): Boolean {
        val target = url.lowercase()
        val blocked = listOf(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "adnxs.com",
            "ads.",
            "click.",
            "tracking."
        )

        if (elapsedMs > maxRedirectDelayMs) return false
        if (lastSafePage == null) return false

        val currentHost = URI(lastSafePage ?: return false).host?.lowercase()
        val targetHost = URI(url).host?.lowercase()

        if (targetHost == null || currentHost == null) return false
        if (currentHost == targetHost) return false
        if (blocked.none { target.contains(it) }) return false

        return true
    }
}
