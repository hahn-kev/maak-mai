package org.hahn.maakmai.data

import kotlinx.coroutines.flow.Flow
import org.hahn.maakmai.model.TagFolder
import java.util.UUID

interface FolderRepository {
    suspend fun createFolder(folder: Folder): Result<Unit>

    suspend fun getFolderByTag(tag: String): Result<Folder>

    suspend fun getAllFolders(): List<TagFolder>

    fun getFoldersStream(): Flow<List<TagFolder>>

    suspend fun updateFolder(folder: Folder): Result<Unit>

    suspend fun deleteFolder(id: UUID): Result<Unit>
}