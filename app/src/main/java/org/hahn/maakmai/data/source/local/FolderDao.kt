package org.hahn.maakmai.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.hahn.maakmai.data.Folder
import java.util.UUID

@Dao
interface FolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder)

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: UUID): Folder?

    @Query("SELECT * FROM folders WHERE tag = :tag")
    suspend fun getFolderByTag(tag: String): Folder?

    @Update
    suspend fun updateFolder(folder: Folder): Int

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: UUID): Int

    @Query("SELECT * FROM folders")
    fun getFoldersStream(): Flow<List<Folder>>

    @Query("SELECT * FROM folders")
    suspend fun getAllFolders(): List<Folder>
}
