package org.hahn.maakmai.util

import androidx.core.net.toUri

/**
 * Unwraps a Google redirect *query wrapper* — `https://www.google.com/url?q=<dest>`
 * — to the destination URL it carries, so the real link is captured rather than
 * the wrapper.
 *
 * This is a purely local, string-based transform for the one shape whose
 * destination is embedded as a query parameter. Redirect *shorteners* whose
 * destination is only revealed over the network (e.g. `share.google`) are handled
 * separately by [ShortLinkResolver]. The set of wrapper hosts is intentionally
 * small and conservative.
 */
object GoogleUrlUnwrapper {

    private val WRAPPER_HOSTS = setOf("www.google.com", "google.com")

    fun unwrap(url: String): String {
        return try {
            val uri = url.toUri()
            if (uri.host?.lowercase() in WRAPPER_HOSTS && uri.path == "/url") {
                val destination = uri.getQueryParameter("q") ?: uri.getQueryParameter("url")
                if (destination != null && destination.isHttpUrl()) destination else url
            } else {
                url
            }
        } catch (e: Exception) {
            url
        }
    }

    private fun String.isHttpUrl(): Boolean =
        startsWith("http://") || startsWith("https://")
}
