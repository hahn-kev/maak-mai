package org.hahn.maakmai.util

import java.net.URI

/**
 * Finds candidate images in a page's HTML for the user to choose from. Declared share
 * images (OpenGraph, Twitter card, JSON-LD) come first, then `<img>` tags in page order.
 */
object PageImages {
    private const val MAX_IMAGES = 60

    private val metaTag = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val linkTag = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val imgTag = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val jsonLdImage = Regex("\"image\"\\s*:\\s*(?:\\{[^}]*?\"url\"\\s*:\\s*)?\"([^\"]+)\"")
    private val shareImageProperties = setOf("og:image", "og:image:url", "og:image:secure_url", "twitter:image", "twitter:image:src")

    fun extract(html: String, pageUrl: String): List<String> {
        val candidates = mutableListOf<String>()

        metaTag.findAll(html).forEach { match ->
            val tag = match.value
            val key = (attribute(tag, "property") ?: attribute(tag, "name"))?.lowercase()
            if (key in shareImageProperties) attribute(tag, "content")?.let(candidates::add)
        }
        linkTag.findAll(html).forEach { match ->
            val tag = match.value
            if (attribute(tag, "rel")?.lowercase() == "image_src") attribute(tag, "href")?.let(candidates::add)
        }
        jsonLdImage.findAll(html).forEach { candidates += it.groupValues[1].replace("\\/", "/") }
        imgTag.findAll(html).forEach { match ->
            imageFromImgTag(match.value)?.let(candidates::add)
        }

        return candidates
            .map(::decodeEntities)
            .mapNotNull { resolve(pageUrl, it) }
            .filter(::looksLikePhoto)
            .distinctBy(::sameImageKey)
            .take(MAX_IMAGES)
    }

    /** The best source for an `<img>`: the largest srcset entry, then lazy-load attributes, then src. */
    private fun imageFromImgTag(tag: String): String? {
        val width = attribute(tag, "width")?.toIntOrNull()
        val height = attribute(tag, "height")?.toIntOrNull()
        // Tracking pixels and spacers
        if ((width != null && width < 32) || (height != null && height < 32)) return null

        val srcset = attribute(tag, "srcset") ?: attribute(tag, "data-srcset")
        largestFromSrcset(srcset)?.let { return it }
        return attribute(tag, "data-src")
            ?: attribute(tag, "data-lazy-src")
            ?: attribute(tag, "src")
    }

    internal fun largestFromSrcset(srcset: String?): String? {
        if (srcset.isNullOrBlank()) return null
        return srcset.split(",")
            .mapNotNull { entry ->
                val parts = entry.trim().split(Regex("\\s+"))
                val url = parts.firstOrNull()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                val descriptor = parts.getOrNull(1)
                val size = descriptor?.dropLast(1)?.toFloatOrNull() ?: 1f
                url to size
            }
            .maxByOrNull { it.second }
            ?.first
    }

    internal fun attribute(tag: String, name: String): String? {
        val match = Regex("\\s$name\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", RegexOption.IGNORE_CASE).find(tag)
            ?: return null
        return match.groupValues.drop(1).firstOrNull { it.isNotEmpty() }?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun resolve(pageUrl: String, src: String): String? {
        if (src.startsWith("data:", ignoreCase = true)) return null
        return try {
            val resolved = URI(pageUrl).resolve(src.replace(" ", "%20"))
            resolved.toString().takeIf { resolved.scheme == "http" || resolved.scheme == "https" }
        } catch (e: Exception) {
            null
        }
    }

    /** Drops vector graphics, icons and other images that are never a bookmark's picture. */
    private fun looksLikePhoto(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        if (path.endsWith(".svg") || path.endsWith(".ico")) return false
        val name = path.substringAfterLast('/')
        return listOf("favicon", "sprite", "pixel", "spacer", "blank.gif").none { it in name }
    }

    // Amazon's image CDN (IMDb, Amazon) encodes resizing in the file name, e.g.
    // "poster._V1_QL75_UX324_.jpg" and "poster._V1_FMjpg_UX1000_.jpg" are the same image.
    private val amazonResizeSuffix = Regex("\\._V1_[^/]*(?=\\.[a-z]+$)", RegexOption.IGNORE_CASE)

    /** Treats resized copies of one image as the same image, so only the first is kept. */
    private fun sameImageKey(url: String): String = amazonResizeSuffix.replace(url, "")

    private fun decodeEntities(value: String): String =
        value.replace("&amp;", "&").replace("&#38;", "&").replace("&quot;", "\"").replace("&#39;", "'")
}
