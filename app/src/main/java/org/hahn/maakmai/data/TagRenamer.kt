package org.hahn.maakmai.data

import javax.inject.Inject
import javax.inject.Singleton

enum class RenameCheck { NoChange, Rename, Merge, Conflict }

/**
 * The single path for renaming a tag: it renames the tag on bookmarks, on folders with
 * that tag, and in folder tag groups.
 */
@Singleton
class TagRenamer @Inject constructor(
    private val bookmarkRepository: BookmarkRepository,
    private val folderRepository: FolderRepository
) {
    suspend fun check(oldTag: String, newTag: String): RenameCheck {
        val target = newTag.trim()
        if (target.isEmpty() || target.equals(oldTag, ignoreCase = true)) return RenameCheck.NoChange

        val folders = folderRepository.getAllFolders()
        if (renameTagInFolders(folders, oldTag, target).isFailure) return RenameCheck.Conflict

        val existingTags = bookmarkRepository.getTagsWithCount().keys + folders.map { it.tag }
        return if (existingTags.any { it.equals(target, ignoreCase = true) }) {
            RenameCheck.Merge
        } else {
            RenameCheck.Rename
        }
    }

    suspend fun rename(oldTag: String, newTag: String): Result<Unit> {
        val target = newTag.trim()
        val changed = renameTagInFolders(folderRepository.getAllFolders(), oldTag, target)
            .getOrElse { return Result.failure(it) }
        for (folder in changed) {
            folderRepository.updateFolder(folder).onFailure { return Result.failure(it) }
        }
        bookmarkRepository.renameTag(oldTag, target)
        return Result.success(Unit)
    }
}
