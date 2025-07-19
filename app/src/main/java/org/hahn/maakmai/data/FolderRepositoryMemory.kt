package org.hahn.maakmai.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.hahn.maakmai.model.TagFolder
import java.util.UUID
import javax.inject.Inject

class FolderRepositoryMemory @Inject constructor() : FolderRepository {

    // In-memory storage for folders
    private val folders = mutableMapOf<UUID, Folder>()

    // StateFlow to emit updates when folders change
    private val _foldersFlow = MutableStateFlow<List<TagFolder>>(emptyList())

    init {
        createTagFolder(
            TagFolder(
                id = UUID.randomUUID(),
                tag = "crochet",
                children = listOf(
                    TagFolder(id = UUID.randomUUID(), tag = "mittens", children = listOf(), tagGroups = listOf("winter", "accessories")),
                    TagFolder(id = UUID.randomUUID(), tag = "scarf", children = listOf(), tagGroups = listOf("winter", "accessories"))
                ),
                rootFolder = true,
                tagGroups = listOf("craft", "hobby")
            )
        )
        createTagFolder(
            TagFolder(
                id = UUID.randomUUID(),
                tag = "knitting",
                children = listOf(
                    TagFolder(id = UUID.randomUUID(), tag = "mittens", children = listOf(), tagGroups = listOf("winter", "accessories")),
                    TagFolder(id = UUID.randomUUID(), tag = "sweater", children = listOf(), tagGroups = listOf("winter", "clothing"))
                ),
                rootFolder = true,
                tagGroups = listOf("craft", "hobby")
            )
        )
    }

    // Helper function to convert folders to TagFolders
    private fun buildTagFolders(): List<TagFolder> {
        // Find root folders (those with null parent)
        val rootFolders = folders.values.filter { it.parent == null }

        // Build TagFolder hierarchy
        return rootFolders.map { buildTagFolderHierarchy(it.id) }
    }

    // Recursively build TagFolder hierarchy
    private fun buildTagFolderHierarchy(folderId: UUID): TagFolder {
        val folder = folders[folderId] ?: throw IllegalStateException("Folder not found: $folderId")
        val children = folders.values
            .filter { it.parent == folderId }
            .map { buildTagFolderHierarchy(it.id) }

        return TagFolder(
            id = folder.id,
            tag = folder.tag,
            children = children,
            rootFolder = folder.parent == null,
            tagGroups = folder.tagGroups
        )
    }

    // Update the flow with current folders
    private fun updateFoldersFlow() {
        _foldersFlow.update { buildTagFolders() }
    }

    override suspend fun createFolder(folder: Folder): Result<Unit> {
        return try {
            folders[folder.id] = folder
            updateFoldersFlow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createTagFolder(folder: TagFolder, parentId: UUID? = null) {
        // Convert TagFolder to Folder
        val newFolder = Folder(
            id = folder.id,
            tag = folder.tag,
            parent = parentId,
            tagGroups = folder.tagGroups
        )

        // Create the folder and all its children
        folders[newFolder.id] = newFolder
        folder.children.forEach { child ->
            createTagFolder(child, newFolder.id)
        }

        // Update the flow
        updateFoldersFlow()
    }

    override suspend fun getFolderByTag(tag: String): Result<Folder> {
        return try {
            val folder = folders.values.find { it.tag == tag }
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
            val folder = folders[id]
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
        return buildTagFolders()
    }

    override fun getFoldersStream(): Flow<List<TagFolder>> {
        return _foldersFlow.asStateFlow()
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> {
        return try {
            if (!folders.containsKey(folder.id)) {
                Result.failure(NoSuchElementException("Folder with id ${folder.id} not found"))
            } else {
                folders[folder.id] = folder
                updateFoldersFlow()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteFolder(id: UUID): Result<Unit> {
        return try {
            if (!folders.containsKey(id)) {
                Result.failure(NoSuchElementException("Folder with id $id not found"))
            } else {
                // Check if folder has children
                val hasChildren = folders.values.any { it.parent == id }
                if (hasChildren) {
                    Result.failure(IllegalStateException("Cannot delete folder with children"))
                } else {
                    folders.remove(id)
                    updateFoldersFlow()
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
