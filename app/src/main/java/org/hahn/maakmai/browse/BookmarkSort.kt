package org.hahn.maakmai.browse

import org.hahn.maakmai.model.Bookmark
import java.net.URI
import java.util.Locale

enum class BookmarkSort {
    CREATED_NEWEST,
    CREATED_OLDEST,
    TITLE_ASCENDING,
    TITLE_DESCENDING,
    DOMAIN_ASCENDING,
    DOMAIN_DESCENDING
}

fun sortBookmarks(bookmarks: List<Bookmark>, sort: BookmarkSort): List<Bookmark> {
    val titleAscending = compareBy<Bookmark, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
        .thenBy { it.id.toString() }
    val titleDescending = compareBy<Bookmark, String>(String.CASE_INSENSITIVE_ORDER.reversed()) { it.title }
        .thenBy { it.id.toString() }
    val domainAscending = compareBy<Bookmark, String?>(nullsLast(String.CASE_INSENSITIVE_ORDER)) {
        bookmarkDomain(it.url)
    }.then(titleAscending)
    val domainDescending = compareBy<Bookmark, String?>(nullsLast(String.CASE_INSENSITIVE_ORDER.reversed())) {
        bookmarkDomain(it.url)
    }.then(titleAscending)

    val comparator = when (sort) {
        BookmarkSort.CREATED_NEWEST -> compareByDescending<Bookmark> { it.createdAt }.then(titleAscending)
        BookmarkSort.CREATED_OLDEST -> compareBy<Bookmark> { it.createdAt }.then(titleAscending)
        BookmarkSort.TITLE_ASCENDING -> titleAscending
        BookmarkSort.TITLE_DESCENDING -> titleDescending
        BookmarkSort.DOMAIN_ASCENDING -> domainAscending
        BookmarkSort.DOMAIN_DESCENDING -> domainDescending
    }

    return bookmarks.sortedWith(comparator)
}

internal fun bookmarkDomain(url: String?): String? {
    val value = url?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val uriValue = if ("://" in value) value else "https://$value"
    return runCatching {
        URI(uriValue).host
            ?.removePrefix("www.")
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotEmpty() }
    }.getOrNull()
}
