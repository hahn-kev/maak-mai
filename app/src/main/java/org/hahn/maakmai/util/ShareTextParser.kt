package org.hahn.maakmai.util

import android.util.Patterns

/**
 * Parses the raw extras of an incoming `ACTION_SEND` share into the fields the
 * add-bookmark flow needs.
 *
 * The key behaviour is that a URL is located *within* the shared text rather than
 * requiring the whole text to be a URL. Common share sources (Google, Reddit, X,
 * WhatsApp, YouTube) send a title and a URL together — e.g. `"Page Title\nURL"` or
 * `"Title URL"` — and the URL must still be captured.
 */
object ShareTextParser {

    private val WHITESPACE = Regex("\\s+")

    data class ParsedShare(
        val url: String?,
        val title: String?,
        val description: String?
    )

    /**
     * @param text the `EXTRA_TEXT` of the share (the payload that usually holds the URL)
     * @param subject the `EXTRA_SUBJECT`, used as the description and as a title fallback
     * @param titleExtra the `EXTRA_TITLE`, used as a title fallback
     */
    fun parse(text: String?, subject: String? = null, titleExtra: String? = null): ParsedShare {
        val trimmedText = text?.trim().orEmpty()

        var url: String? = null
        var derivedTitle: String? = null

        if (trimmedText.isNotEmpty()) {
            val matcher = Patterns.WEB_URL.matcher(trimmedText)
            if (matcher.find()) {
                url = trimmedText.substring(matcher.start(), matcher.end())
                // Whatever text surrounds the URL is the best title candidate.
                derivedTitle = (trimmedText.substring(0, matcher.start()) + " " +
                        trimmedText.substring(matcher.end()))
                    .collapseWhitespace()
            } else {
                // No URL in the text — keep the text itself as the title rather
                // than discarding it.
                derivedTitle = trimmedText.collapseWhitespace()
            }
        }

        val title = derivedTitle
            ?: titleExtra?.trim()?.ifBlank { null }
            ?: subject?.trim()?.ifBlank { null }

        return ParsedShare(
            url = url,
            title = title,
            description = subject?.trim()?.ifBlank { null }
        )
    }

    private fun String.collapseWhitespace(): String? =
        replace(WHITESPACE, " ").trim().ifBlank { null }
}
