package org.hahn.maakmai.util

import org.hahn.maakmai.model.Bookmark
import java.util.UUID

/**
 * Builds the [Bookmark] that is persisted the moment a share arrives, before the
 * user does anything.
 *
 * The share is auto-captured (see the "persist-in-share-handler" flow) so that a
 * link is never lost if the user closes, backs out of, or swipes away the Add
 * screen. The captured bookmark carries the URL verbatim and a sensible title;
 * the Add screen then edits this same record.
 */
object SharedBookmarkFactory {

    fun fromShare(parsed: ShareTextParser.ParsedShare, id: UUID = UUID.randomUUID()): Bookmark {
        val title = parsed.title?.ifBlank { null }
            ?: parsed.url?.let { UrlTitleExtractor.fromUrl(it) }
            ?: ""

        val description = parsed.description
            ?.ifBlank { null }
            ?.takeIf { it != title }
            ?: ""

        return Bookmark(
            id = id,
            title = title,
            description = description,
            // The URL is stored exactly as parsed — no route encode/decode round-trip.
            url = parsed.url,
            tags = emptyList()
        )
    }
}
