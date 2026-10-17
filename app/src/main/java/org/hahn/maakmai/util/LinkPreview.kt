package org.hahn.maakmai.util

/**
 * The title, description and image for a link, fetched the same way a shared link is
 * enriched on the Add screen: OpenGraph metadata, with known short links resolved to
 * their destination.
 */
data class LinkPreview(
    val url: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    /** A short reason the page could not be fetched, or null when it was fetched. */
    val error: String?
) {
    companion object {
        suspend fun fetch(url: String): LinkPreview =
            from(url, OpenGraphUtils.extractUrlOpenGraphMetadata(url))

        fun from(url: String, openGraph: OpenGraphUtils.OpenGraphMetadata): LinkPreview {
            val resolvedUrl = ShortLinks.resolve(url, openGraph.finalUrl)
            return LinkPreview(
                url = resolvedUrl,
                title = openGraph.title?.ifBlank { null } ?: UrlTitleExtractor.fromUrl(resolvedUrl),
                description = openGraph.description?.ifBlank { null } ?: "",
                imageUrl = openGraph.image?.ifBlank { null },
                error = openGraph.error
            )
        }
    }
}
