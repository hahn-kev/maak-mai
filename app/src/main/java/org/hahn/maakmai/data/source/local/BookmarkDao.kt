package org.hahn.maakmai.data.source.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.hahn.maakmai.model.Bookmark
import java.util.UUID

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: UUID): Bookmark?

    @Update
    suspend fun updateBookmark(bookmark: Bookmark): Int

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: UUID): Int

    @Query("SELECT * FROM bookmarks")
    fun getBookmarksStream(): Flow<List<Bookmark>>

    // This is a bit tricky with Room, as we need to check if any tag in the list matches
    // We'll use a simplified approach for now
    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarks(): List<Bookmark>
}
