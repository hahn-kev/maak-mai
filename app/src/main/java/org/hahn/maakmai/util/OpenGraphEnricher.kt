package org.hahn.maakmai.util

/**
 * Merges asynchronously-fetched OpenGraph metadata into the fields already shown
 * on the Add screen.
 *
 * Enrichment only ever *fills in* or *improves* fields — it never gates or clears
 * them, and it never overwrites a value the user has already edited. A failed or
 * empty fetch therefore leaves the text-derived title (and everything else)
 * untouched. The URL is deliberately not part of this merge: it is captured up
 * front and must never be nulled by metadata.
 */
object OpenGraphEnricher {

    data class Fields(
        val title: String,
        val description: String,
        val imageUri: String?
    )

    data class Edited(
        val title: Boolean = false,
        val description: Boolean = false,
        val image: Boolean = false
    )

    fun enrich(
        current: Fields,
        ogTitle: String?,
        ogDescription: String?,
        ogImage: String?,
        edited: Edited
    ): Fields {
        val title = if (edited.title) current.title else ogTitle.orCurrent(current.title)
        val description =
            if (edited.description) current.description else ogDescription.orCurrent(current.description)
        // Only fill an image slot the user hasn't touched and that is still empty;
        // never replace an image the user picked or one already present.
        val imageUri = if (edited.image || current.imageUri != null) {
            current.imageUri
        } else {
            ogImage?.ifBlank { null } ?: current.imageUri
        }

        return Fields(title = title, description = description, imageUri = imageUri)
    }

    private fun String?.orCurrent(current: String): String =
        this?.ifBlank { null } ?: current
}
