package org.hahn.maakmai.browse

import org.hahn.maakmai.model.Bookmark
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class BookmarkSortTest {
    private val bookmarks = listOf(
        bookmark("Zulu", "https://www.beta.example/page", createdAt = 200),
        bookmark("alpha", "https://gamma.example/page", createdAt = 300),
        bookmark("Bravo", "https://alpha.example/page", createdAt = 100),
        bookmark("No URL", null, createdAt = 400)
    )

    @Test
    fun `created sorting defaults to most recent first`() {
        val sorted = sortBookmarks(bookmarks, BookmarkSort.CREATED_NEWEST)

        assertEquals(listOf("No URL", "alpha", "Zulu", "Bravo"), sorted.map { it.title })
    }

    @Test
    fun `title sorting is case insensitive in both directions`() {
        assertEquals(
            listOf("alpha", "Bravo", "No URL", "Zulu"),
            sortBookmarks(bookmarks, BookmarkSort.TITLE_ASCENDING).map { it.title }
        )
        assertEquals(
            listOf("Zulu", "No URL", "Bravo", "alpha"),
            sortBookmarks(bookmarks, BookmarkSort.TITLE_DESCENDING).map { it.title }
        )
    }

    @Test
    fun `domain sorting ignores www and leaves missing domains last`() {
        assertEquals(
            listOf("Bravo", "Zulu", "alpha", "No URL"),
            sortBookmarks(bookmarks, BookmarkSort.DOMAIN_ASCENDING).map { it.title }
        )
        assertEquals(
            listOf("alpha", "Zulu", "Bravo", "No URL"),
            sortBookmarks(bookmarks, BookmarkSort.DOMAIN_DESCENDING).map { it.title }
        )
    }

    private fun bookmark(title: String, url: String?, createdAt: Long) = Bookmark(
        id = UUID.nameUUIDFromBytes(title.toByteArray()),
        title = title,
        description = "",
        url = url,
        tags = emptyList(),
        createdAt = createdAt
    )
}
