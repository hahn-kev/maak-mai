package org.hahn.maakmai.data

/**
 * Thrown when renaming a tag would leave two folders with the same tag under one parent,
 * which breaks path navigation.
 */
class TagConflictException(val tag: String) :
    IllegalStateException("A folder named '$tag' already exists next to one being renamed")

/**
 * Renames [oldTag] to [newTag] across [folders]: each folder's own tag and its tag groups.
 * Matching ignores case. Returns only the folders that changed, or fails with
 * [TagConflictException] without changing anything.
 */
fun renameTagInFolders(folders: List<Folder>, oldTag: String, newTag: String): Result<List<Folder>> {
    val renamed = folders.filter { it.tag.equals(oldTag, ignoreCase = true) }
    val conflict = renamed.any { folder ->
        folders.any { sibling ->
            sibling.id != folder.id &&
                sibling.parent == folder.parent &&
                sibling.tag.equals(newTag, ignoreCase = true)
        }
    }
    if (conflict) return Result.failure(TagConflictException(newTag))

    val changed = folders.mapNotNull { folder ->
        val updated = folder.copy(
            tag = if (folder.tag.equals(oldTag, ignoreCase = true)) newTag else folder.tag,
            tagGroups = folder.tagGroups
                .map { if (it.equals(oldTag, ignoreCase = true)) newTag else it }
                .distinct()
        )
        updated.takeIf { it != folder }
    }
    return Result.success(changed)
}
