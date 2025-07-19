package org.hahn.maakmai.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.hahn.maakmai.model.Bookmark
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryMemory @Inject constructor() : BookmarkRepository {
    private val bookmarks = listOf(
        Bookmark(title = "Blue shirt", description = "desc", url = null, tags = listOf("knitting"), id = UUID.randomUUID()),
        Bookmark(title = "Red shirt", description = "desc", url = null, tags = listOf("knitting"), id = UUID.randomUUID()),
        Bookmark(title = "Green shirt", description = "desc", url = null, tags = listOf("knitting"), id = UUID.randomUUID()),
        Bookmark(title = "Blue mittens", description = "desc", url = null, tags = listOf("mittens", "knitting"), id = UUID.randomUUID()),
        Bookmark(title = "Red mittens", description = "desc", url = null, tags = listOf("mittens", "knitting"), id = UUID.randomUUID()),
        Bookmark(title = "Green mittens", description = "desc", url = null, tags = listOf("mittens", "knitting"), id = UUID.randomUUID()),
        Bookmark(title = "Blue mittens", description = "desc", url = null, tags = listOf("mittens", "crochet"), id = UUID.randomUUID()),
        Bookmark(title = "Red mittens", description = "desc", url = null, tags = listOf("mittens", "crochet"), id = UUID.randomUUID()),
        Bookmark(title = "Green mittens", description = "desc", url = null, tags = listOf("mittens", "crochet"), id = UUID.randomUUID()),
        Bookmark(title = "Blue scarf", description = "desc", url = null, tags = listOf("scarf", "crochet"), id = UUID.randomUUID()),
        Bookmark(title = "Red scarf", description = "desc", url = null, tags = listOf("scarf", "crochet"), id = UUID.randomUUID()),
        Bookmark(title = "Green scarf", description = "desc", url = null, tags = listOf("scarf", "crochet"), id = UUID.randomUUID()),
        Bookmark(title = "Blue sweater", description = "desc", url = null, tags = listOf("sweater", "knitting"), id = UUID.randomUUID()),
        Bookmark(title = "Red sweater", description = "desc", url = null, tags = listOf("sweater", "knitting"), id = UUID.randomUUID()),
        Bookmark(title = "Green sweater", description = "desc", url = null, tags = listOf("sweater", "knitting"), id = UUID.randomUUID()),
        Bookmark(title = "Applesauce", description = "desc", url = null, tags = listOf(), id = UUID.randomUUID()),
    ).associateBy { it.id }.toMutableMap()

    override suspend fun createBookmark(bookmark: Bookmark) {
        bookmarks[bookmark.id] = bookmark
    }

    override suspend fun getBookmark(id: UUID): Bookmark? {
        return bookmarks[id]
    }

    override suspend fun updateBookmark(bookmark: Bookmark): Boolean {
        return if (bookmarks.containsKey(bookmark.id)) {
            bookmarks[bookmark.id] = bookmark
            true
        } else {
            false
        }
    }

    override suspend fun deleteBookmark(id: UUID): Boolean {
        return bookmarks.remove(id) != null
    }

    override suspend fun getBookmarksByTags(tags: List<String>): List<Bookmark> {
        return if (tags.isEmpty()) {
            bookmarks.values.toList()
        } else {
            bookmarks.values.filter { bookmark ->
                bookmark.tags.any { it in tags }
            }
        }
    }

    override fun getBookmarksStream(): Flow<List<Bookmark>> {
        return flow {
            emit(bookmarks.values.toList())
        }
    }


}
