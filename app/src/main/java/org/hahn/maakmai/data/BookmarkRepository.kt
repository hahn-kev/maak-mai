package org.hahn.maakmai.data

import kotlinx.coroutines.flow.Flow
import org.hahn.maakmai.model.Bookmark
import java.util.UUID

interface BookmarkRepository {
    suspend fun createBookmark(bookmark: Bookmark)

    suspend fun getBookmark(id: UUID): Bookmark?

    suspend fun updateBookmark(bookmark: Bookmark): Boolean

    suspend fun deleteBookmark(id: UUID): Boolean

    suspend fun getBookmarksByTags(tags: List<String>): List<Bookmark>

    fun getBookmarksStream(): Flow<List<Bookmark>>
}