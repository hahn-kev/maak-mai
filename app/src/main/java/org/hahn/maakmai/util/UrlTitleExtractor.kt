package org.hahn.maakmai.util

import android.net.Uri
import androidx.core.net.toUri

/**
 * Derives a human-readable title from a bare URL when no better title is
 * available from the shared text or Open Graph metadata.
 *
 * Tries the last path segment first, then the hostname without its TLD, and
 * finally the full hostname.
 */
object UrlTitleExtractor {

    fun fromUrl(url: String): String {
        return try {
            val uri = url.toUri()

            // Try last path segment first
            extractLastPathSegment(uri)?.let { segment ->
                if (!isAllNumbers(segment)) {
                    return cleanupTitle(segment)
                }
            }

            // Try hostname without TLD
            uri.host?.let { host ->
                val parts = host.split(".")
                if (parts.size >= 2) {
                    val domain = parts[parts.size - 2]
                    if (!isAllNumbers(domain)) {
                        return cleanupTitle(domain)
                    }
                }
            }

            // Fallback to full hostname
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }

    private fun extractLastPathSegment(uri: Uri): String? {
        return try {
            val path = uri.path ?: return null
            path.split("/").last().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    private fun isAllNumbers(str: String): Boolean {
        return str.all { it.isDigit() || it == '.' || it == ',' }
    }

    private fun cleanupTitle(title: String): String {
        return title
            .replace("[_-]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            .trim()
    }
}
