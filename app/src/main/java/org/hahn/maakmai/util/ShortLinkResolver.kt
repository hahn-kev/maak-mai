package org.hahn.maakmai.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves redirect *shorteners* whose real destination is only revealed by
 * following HTTP redirects over the network — e.g. `share.google/<code>`, which
 * 302s to `www.google.com/share.google?q=<code>` and then 301s on to the final
 * page. (Verified: this chain is plain HTTP 3xx, no JavaScript required.)
 *
 * The set of short-link hosts is intentionally small and conservative. Query-based
 * wrappers whose destination is embedded in the URL are handled locally by
 * [GoogleUrlUnwrapper] instead.
 */
object ShortLinkResolver {

    private val SHORT_LINK_HOSTS = setOf("share.google")
    private const val MAX_REDIRECTS = 10
    private const val TIMEOUT_MS = 10000
    private const val USER_AGENT = "Mozilla/5.0 (Android) MaakMai/1.0"

    /** Whether [url]'s host is a known redirect shortener worth resolving. */
    fun isShortLink(url: String): Boolean {
        return try {
            java.net.URI(url).host?.lowercase() in SHORT_LINK_HOSTS
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Follows the redirect chain starting at [url] and returns the final URL.
     * Runs on [Dispatchers.IO]; on any network failure the original [url] is
     * returned unchanged so a captured link is never lost.
     */
    suspend fun resolve(url: String): String = withContext(Dispatchers.IO) {
        try {
            followChain(url) { locationHeaderOf(it) }
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Pure redirect-following: repeatedly asks [nextLocation] for the `Location`
     * of the current URL, resolving relative locations, until there is no further
     * redirect, a loop is detected, or [maxRedirects] is reached.
     */
    fun followChain(
        start: String,
        maxRedirects: Int = MAX_REDIRECTS,
        nextLocation: (String) -> String?
    ): String {
        var current = start
        val seen = HashSet<String>()
        repeat(maxRedirects) {
            if (!seen.add(current)) return current
            val location = nextLocation(current)?.takeIf { it.isNotBlank() } ?: return current
            current = try {
                URL(URL(current), location).toString()
            } catch (e: Exception) {
                return current
            }
        }
        return current
    }

    private fun locationHeaderOf(url: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
            }
            if (connection.responseCode in 300..399) connection.getHeaderField("Location") else null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
