package org.hahn.maakmai.data

import kotlinx.coroutines.flow.Flow
import org.hahn.maakmai.data.source.local.BookmarkDao
import org.hahn.maakmai.model.Bookmark
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryRoom @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {
    
    override suspend fun createBookmark(bookmark: Bookmark) {
        bookmarkDao.insertBookmark(bookmark)
    }

    override suspend fun getBookmark(id: UUID): Bookmark? {
        return bookmarkDao.getBookmarkById(id)
    }

    override suspend fun updateBookmark(bookmark: Bookmark): Boolean {
        return bookmarkDao.updateBookmark(bookmark) > 0
    }

    override suspend fun deleteBookmark(id: UUID): Boolean {
        return bookmarkDao.deleteBookmarkById(id) > 0
    }

    override suspend fun getBookmarksByTags(tags: List<String>): List<Bookmark> {
        // Since Room doesn't easily support filtering by list elements,
        // we'll get all bookmarks and filter them in memory
        val allBookmarks = bookmarkDao.getAllBookmarks()
        return if (tags.isEmpty()) {
            allBookmarks
        } else {
            allBookmarks.filter { bookmark ->
                bookmark.tags.any { tag -> tags.contains(tag) }
            }
        }
    }

    override fun getBookmarksStream(): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksStream()
    }
}