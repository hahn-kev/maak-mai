package org.hahn.maakmai.util

import android.text.Html

/**
 * Utility class for extracting Open Graph metadata from URLs.
 */
object OpenGraphUtils {

    /**
     * Data class representing Open Graph metadata.
     */
    data class OpenGraphMetadata(
        val title: String? = null,
        val description: String? = null,
        val url: String? = null,
        val image: String? = null,
        val siteName: String? = null,
        /**
         * The URL actually landed on after following redirects, as reported by the
         * connection. Distinct from [url] (the `og:url` meta tag). Used to resolve
         * redirect shorteners without a separate network round-trip.
         */
        val finalUrl: String? = null
    )

    /**
     * Fetches Open Graph metadata from a URL.
     * 
     * @param url The URL to fetch metadata from
     * @return OpenGraphMetadata object containing the extracted metadata
     */
    suspend fun getOpenGraphMetadata(url: String): OpenGraphMetadata {
        val page = PageFetcher.fetch(url)
        // Still report where we landed so redirect shorteners resolve even when the
        // destination doesn't serve a usable page (e.g. a bot challenge).
        val html = page.html ?: return OpenGraphMetadata(finalUrl = page.finalUrl)
        return parseOpenGraphMetadata(html).copy(finalUrl = page.finalUrl)
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
    fun parseOpenGraphMetadata(html: String): OpenGraphMetadata {
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
            val value = it.groupValues[1].ifEmpty { it.groupValues[2] }
            decodeHtmlEntities(value)
        }
    }

    /**
     * Decodes HTML entities in a string.
     * 
     * @param input The string containing HTML entities
     * @return The decoded string
     */
    private fun decodeHtmlEntities(input: String): String {
        return Html.fromHtml(input, Html.FROM_HTML_MODE_LEGACY).toString()
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

        return matchResult?.groupValues?.get(1)?.trim()?.let { decodeHtmlEntities(it) }
    }
}
