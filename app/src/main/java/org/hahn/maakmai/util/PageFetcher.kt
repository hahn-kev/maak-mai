package org.hahn.maakmai.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Fetches a page's HTML. It asks as MaakMai first. Some sites (IMDb, for one) only serve
 * pages to known link-preview crawlers and answer everyone else with an empty bot
 * challenge, so a failed or empty response is retried once with a link-preview agent.
 */
object PageFetcher {
    private const val TAG = "PageFetcher"
    private const val TIMEOUT_MS = 10000
    private const val USER_AGENT = "Mozilla/5.0 (Android) MaakMai/1.0"
    private const val LINK_PREVIEW_USER_AGENT = "WhatsApp/2.23"

    /** Anything shorter than this is a challenge or error page, not the real document. */
    private const val MIN_PAGE_LENGTH = 512

    data class Page(
        /** The page's HTML, or null if neither attempt returned a usable page. */
        val html: String?,
        /** Where the request landed after redirects, even when [html] is null. */
        val finalUrl: String?
    )

    suspend fun fetch(url: String): Page = withContext(Dispatchers.IO) {
        val first = fetchWith(url, USER_AGENT)
        if (first.html != null) return@withContext first
        val retry = fetchWith(url, LINK_PREVIEW_USER_AGENT)
        if (retry.html != null) retry else first.copy(finalUrl = first.finalUrl ?: retry.finalUrl)
    }

    private fun fetchWith(url: String, userAgent: String): Page {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
                instanceFollowRedirects = true
            }
            try {
                val responseCode = connection.responseCode
                // The connection URL reflects the final destination after redirects.
                val finalUrl = connection.url?.toString()
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "HTTP $responseCode for $url as $userAgent")
                    return Page(html = null, finalUrl = finalUrl)
                }
                val html = connection.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
                Page(html = html.takeIf { it.length >= MIN_PAGE_LENGTH }, finalUrl = finalUrl)
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching $url: ${e.message}", e)
            Page(html = null, finalUrl = null)
        }
    }
}
