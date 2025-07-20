package org.hahn.maakmai.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Utility class for extracting Open Graph metadata from URLs.
 */
object OpenGraphUtils {
    private const val TAG = "OpenGraphUtils"
    private const val TIMEOUT_MS = 10000
    private const val USER_AGENT = "Mozilla/5.0 (Android) MaakMai/1.0"

    /**
     * Data class representing Open Graph metadata.
     */
    data class OpenGraphMetadata(
        val title: String? = null,
        val description: String? = null,
        val url: String? = null,
        val image: String? = null,
        val siteName: String? = null
    )

    /**
     * Fetches Open Graph metadata from a URL.
     * 
     * @param url The URL to fetch metadata from
     * @return OpenGraphMetadata object containing the extracted metadata
     */
    suspend fun getOpenGraphMetadata(url: String): OpenGraphMetadata = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                instanceFollowRedirects = true
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "HTTP error code: $responseCode for URL: $url")
                return@withContext OpenGraphMetadata()
            }

            val html = connection.inputStream.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }

            parseOpenGraphMetadata(html)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Open Graph metadata: ${e.message}", e)
            OpenGraphMetadata()
        }
    }

    /**
     * Extracts title from a URL using Open Graph metadata.
     * Falls back to extracting from URL if Open Graph metadata is not available.
     * 
     * @param url The URL to extract title from
     * @return The extracted title or null if extraction failed
     */
    suspend fun extractUrlOpenGraphMetadata(url: String): OpenGraphMetadata {
        return getOpenGraphMetadata(url)
    }

    /**
     * Parses HTML content to extract Open Graph metadata.
     * 
     * @param html The HTML content to parse
     * @return OpenGraphMetadata object containing the extracted metadata
     */
    private fun parseOpenGraphMetadata(html: String): OpenGraphMetadata {
        val title = extractMetaTag(html, "og:title")
        val description = extractMetaTag(html, "og:description")
        val url = extractMetaTag(html, "og:url")
        val image = extractMetaTag(html, "og:image")
        val siteName = extractMetaTag(html, "og:site_name")
        
        // If no Open Graph title, try regular title tag
        val finalTitle = title ?: extractTitleTag(html)
        
        return OpenGraphMetadata(
            title = finalTitle,
            description = description,
            url = url,
            image = image,
            siteName = siteName
        )
    }

    /**
     * Extracts a meta tag value from HTML content.
     * 
     * @param html The HTML content to parse
     * @param property The meta property to extract (e.g., "og:title")
     * @return The extracted value or null if not found
     */
    private fun extractMetaTag(html: String, property: String): String? {
        val regex = Regex("<meta\\s+(?:property=[\"']$property[\"']\\s+content=[\"']([^\"']*)[\"']|content=[\"']([^\"']*)[\"']\\s+property=[\"']$property[\"'])", RegexOption.IGNORE_CASE)
        val matchResult = regex.find(html)
        
        return matchResult?.let {
            it.groupValues[1].ifEmpty { it.groupValues[2] }
        }
    }

    /**
     * Extracts the title tag value from HTML content.
     * 
     * @param html The HTML content to parse
     * @return The extracted title or null if not found
     */
    private fun extractTitleTag(html: String): String? {
        val regex = Regex("<title>([^<]*)</title>", RegexOption.IGNORE_CASE)
        val matchResult = regex.find(html)
        
        return matchResult?.groupValues?.get(1)?.trim()
    }
}