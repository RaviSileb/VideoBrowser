package com.ravisileb.videobrowser

import java.net.URI

object BrowserGuard {
    private val blockedDomains = listOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adnxs.com",
        "googletagmanager.com",
        "google-analytics.com",
        "tpc.googlesyndication.com",
        "pagead2",
    )

    fun shouldBlockUrl(url: String): Boolean {
        val lower = url.lowercase()
        if (blockedDomains.any { lower.contains(it) }) return true

        val host = try {
            URI(url).host?.lowercase()
        } catch (_: Exception) {
            null
        }

        if (host == null) return false

        if (host.contains("prehraj.to")) {
            return lower.contains("download") ||
                lower.contains("stahnete") ||
                lower.contains("google-tv") ||
                lower.contains("google tv") ||
                lower.contains("install") ||
                lower.contains("app") && lower.contains("download") ||
                lower.contains("apk")
        }

        return false
    }

    fun cleanupScript(): String = """
        (() => {
          const removeNode = (node) => {
            if (!node || !node.parentNode) return;
            node.parentNode.removeChild(node);
          };

          const all = Array.from(document.querySelectorAll('a,div,button,span,section,iframe'));
          for (const node of all) {
            const text = (node.textContent || '').toLowerCase();
            const cls = ((node.className || '') + '').toLowerCase();
            const id = (node.id || '').toLowerCase();
            const href = (node.getAttribute('href') || '').toLowerCase();
            const aria = (node.getAttribute('aria-label') || '').toLowerCase();
            const title = (node.getAttribute('title') || '').toLowerCase();
            const role = (node.getAttribute('role') || '').toLowerCase();

            const shouldRemove =
              cls.includes('ad') ||
              id.includes('ad') ||
              cls.includes('overlay') ||
              id.includes('overlay') ||
              cls.includes('modal') ||
              id.includes('modal') ||
              cls.includes('download') ||
              id.includes('download') ||
              href.includes('download') ||
              href.includes('apk') ||
              aria.includes('close') ||
              title.includes('close') ||
              role.includes('close') ||
              text.includes('stáhněte') ||
              text.includes('download') ||
              text.includes('google tv') ||
              text.includes('zde') ||
              (text.includes('x') && text.trim().length <= 3);

            if (shouldRemove) {
              removeNode(node);
            }
          }

          const video = document.querySelector('video');
          if (video) {
            video.controls = true;
            video.setAttribute('playsinline', 'true');
          }
        })();
    """.trimIndent()
}
