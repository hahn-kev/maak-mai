package org.hahn.maakmai.util

import android.util.Patterns

/**
 * Finds every link in a block of pasted text, for bulk add. Text around the links is
 * ignored. Each link is returned once, in the order it first appears.
 */
object LinkTextParser {

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    private const val TRAILING_PUNCTUATION = ".,;:!?'\"*"

    fun extractUrls(text: String): List<String> {
        val urls = LinkedHashSet<String>()
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            val url = clean(text.substring(matcher.start(), matcher.end())) ?: continue
            urls.add(url)
        }
        return urls.toList()
    }

    private fun clean(match: String): String? {
        var url = match
        // Sentence punctuation and the closing bracket of "(see https://x.com)" or a
        // Markdown link are not part of the link.
        while (url.isNotEmpty()) {
            val last = url.last()
            url = when {
                last in TRAILING_PUNCTUATION -> url.dropLast(1)
                last == ')' && url.count { it == ')' } > url.count { it == '(' } -> url.dropLast(1)
                last == ']' && url.count { it == ']' } > url.count { it == '[' } -> url.dropLast(1)
                else -> break
            }
        }
        if (url.isEmpty()) return null
        // Bare domains such as "example.com/page" need a scheme to be fetched and opened.
        val withScheme = if (SCHEME.containsMatchIn(url)) url else "https://$url"
        return GoogleUrlUnwrapper.unwrap(withScheme)
    }
}
