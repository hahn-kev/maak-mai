package org.hahn.maakmai.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.hahn.maakmai.model.Bookmark
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class BookmarkRepositoryMemory : BookmarkRepository {
    private val bookmarks = mutableMapOf<UUID, Bookmark>()

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
