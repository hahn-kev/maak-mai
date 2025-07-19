package org.hahn.maakmai.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.hahn.maakmai.data.source.local.FolderDao
import org.hahn.maakmai.model.TagFolder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryRoom @Inject constructor(
    private val folderDao: FolderDao
) : FolderRepository {
    
    override suspend fun createFolder(folder: Folder): Result<Unit> {
        return try {
            folderDao.insertFolder(folder)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFolderByTag(tag: String): Result<Folder> {
        return try {
            val folder = folderDao.getFolderByTag(tag)
            if (folder != null) {
                Result.success(folder)
            } else {
                Result.failure(NoSuchElementException("Folder with tag $tag not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllFolders(): List<TagFolder> {
        return buildTagFolders(folderDao.getAllFolders())
    }

    override fun getFoldersStream(): Flow<List<TagFolder>> {
        return folderDao.getFoldersStream().map { folders ->
            buildTagFolders(folders)
        }
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> {
        return try {
            val result = folderDao.updateFolder(folder)
            if (result > 0) {
                Result.success(Unit)
            } else {
                Result.failure(NoSuchElementException("Folder with id ${folder.id} not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFolder(id: UUID): Result<Unit> {
        return try {
            // Check if folder has children
            val allFolders = folderDao.getAllFolders()
            val hasChildren = allFolders.any { it.parent == id }
            
            if (hasChildren) {
                Result.failure(IllegalStateException("Cannot delete folder with children"))
            } else {
                val result = folderDao.deleteFolderById(id)
                if (result > 0) {
                    Result.success(Unit)
                } else {
                    Result.failure(NoSuchElementException("Folder with id $id not found"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper function to convert folders to TagFolders
    private fun buildTagFolders(folders: List<Folder>): List<TagFolder> {
        // Find root folders (those with null parent)
        val rootFolders = folders.filter { it.parent == null }

        // Build TagFolder hierarchy
        return rootFolders.map { buildTagFolderHierarchy(it.id, folders) }
    }

    // Recursively build TagFolder hierarchy
    private fun buildTagFolderHierarchy(folderId: UUID, allFolders: List<Folder>): TagFolder {
        val folder = allFolders.find { it.id == folderId }
            ?: throw IllegalStateException("Folder not found: $folderId")
        
        val children = allFolders
            .filter { it.parent == folderId }
            .map { buildTagFolderHierarchy(it.id, allFolders) }

        return TagFolder(
            id = folder.id,
            tag = folder.tag,
            children = children,
            rootFolder = folder.parent == null
        )
    }
}