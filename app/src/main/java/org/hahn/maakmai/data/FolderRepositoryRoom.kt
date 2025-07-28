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

    override suspend fun getFolderById(id: UUID): Result<Folder> {
        return try {
            val folder = folderDao.getFolderById(id)
            if (folder != null) {
                Result.success(folder)
            } else {
                Result.failure(NoSuchElementException("Folder with id $id not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRootFolders(): List<TagFolder> {
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
            val allFolders = folderDao.getAllFolders()

            // Recursively delete the folder and all its children
            deleteRecursively(id, allFolders)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper function to recursively delete a folder and all its children
    private suspend fun deleteRecursively(folderId: UUID, allFolders: List<Folder>) {
        // Find all direct children of this folder
        val childFolders = allFolders.filter { it.parent == folderId }

        // Recursively delete each child folder
        for (childFolder in childFolders) {
            deleteRecursively(childFolder.id, allFolders)
        }

        // Delete the folder itself
        folderDao.deleteFolderById(folderId)
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
            rootFolder = folder.parent == null,
            tagGroups = folder.tagGroups,
            color = folder.color
        )
    }
}
