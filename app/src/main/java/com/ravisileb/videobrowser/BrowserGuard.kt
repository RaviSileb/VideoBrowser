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
          const hideNode = (node) => {
            if (!node || !(node instanceof Element)) return;
            try {
              node.style.display = 'none';
              node.style.visibility = 'hidden';
              node.style.opacity = '0';
              node.style.pointerEvents = 'none';
            } catch (e) {
              // ignore style errors on detached nodes
            }
            node.setAttribute('aria-hidden', 'true');
            if (node.parentNode) {
              node.parentNode.removeChild(node);
            }
          };

          const shouldHideOverlay = (node) => {
            if (!node || !(node instanceof Element)) return false;

            const text = (node.textContent || '').trim().toLowerCase();
            const cls = ((node.className || '') + '').toLowerCase();
            const id = (node.id || '').toLowerCase();
            const href = (node.getAttribute('href') || '').toLowerCase();
            const aria = (node.getAttribute('aria-label') || '').toLowerCase();
            const title = (node.getAttribute('title') || '').toLowerCase();
            const role = (node.getAttribute('role') || '').toLowerCase();
            const style = window.getComputedStyle(node);
            const rect = node.getBoundingClientRect();

            const isFixed = style.position === 'fixed' || style.position === 'sticky';
            const isModal =
              role.includes('dialog') ||
              node.hasAttribute('aria-modal') ||
              cls.includes('modal') ||
              id.includes('modal') ||
              cls.includes('overlay') ||
              id.includes('overlay');
            const isAdLike =
              cls.includes('ad') ||
              id.includes('ad') ||
              cls.includes('download') ||
              id.includes('download') ||
              href.includes('download') ||
              href.includes('apk') ||
              aria.includes('install') ||
              aria.includes('download') ||
              title.includes('install') ||
              title.includes('download') ||
              text.includes('stáhněte') ||
              text.includes('download') ||
              text.includes('google tv') ||
              text.includes('zde') ||
              text.includes('close');
            const isWhiteLayer =
              (style.backgroundColor === 'rgb(255, 255, 255)' ||
              style.backgroundColor === 'white' ||
              style.backgroundColor.includes('255, 255, 255')) &&
              rect.width > window.innerWidth * 0.6 &&
              rect.height > window.innerHeight * 0.35;
            const hasHighZ = Number.parseInt(style.zIndex || '0', 10) > 5;
            const hasLargeFixedPanel = isFixed && rect.width > window.innerWidth * 0.55 && rect.height > window.innerHeight * 0.12;
            const isSmallCloseButton = text.includes('x') && text.trim().length <= 3;

            return isModal || isAdLike || isWhiteLayer || (hasHighZ && hasLargeFixedPanel) || isSmallCloseButton;
          };

          const all = Array.from(document.querySelectorAll('a,div,button,span,section,aside,header,footer,iframe'));
          for (const node of all) {
            if (shouldHideOverlay(node)) {
              hideNode(node);
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
