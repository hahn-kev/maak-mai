package org.hahn.maakmai.addeditbookmark

import org.hahn.maakmai.model.TagFolder
import java.util.UUID

/**
 * The folder and tag picking rules shared by the Add screen and bulk add.
 */
object TagSelection {

    /** Existing tags, most used first, labelled with their use count. */
    fun prioritisedTags(tagsWithCount: Map<String, Int>): List<TagUiState> =
        tagsWithCount.entries.sortedByDescending { it.value }.map { (tag, count) ->
            TagUiState(tag = tag, label = "$tag ($count)")
        }

    /** The folders along [path], such as the browse folder a screen was opened from. */
    fun folderPathFor(folders: List<TagFolder>, path: String?): List<TagFolder> =
        path?.let { TagFolder(tag = "/", children = folders, id = UUID.randomUUID()).findFolders(it) } ?: emptyList()

    /**
     * Selecting a folder already in the path goes back to it; any other folder is
     * added to the end of the path.
     */
    fun selectFolder(path: List<TagFolder>, folder: TagFolder): List<TagFolder> {
        val existingIndex = path.indexOfFirst { it.id == folder.id }
        return if (existingIndex != -1) path.subList(0, existingIndex + 1) else path + folder
    }

    /** Groups the existing tags under the tag groups of the folders in [path]. */
    fun groupFolderTags(path: List<TagFolder>, allTags: List<String>): List<TagGroup> {
        val folderTags = path.asReversed().flatMap { it.tagGroups.sorted() }
        val groupedTags = mutableListOf<TagGroup>()
        folderTags.forEach { tag ->
            // A tag group is a prefix of other tags; it shows as a section header
            val matchingTags = allTags.filter { it != tag && it.startsWith(tag) }
            if (matchingTags.isEmpty()) {
                return@forEach
            }
            // Only add if not already added (avoid duplicates)
            if (!groupedTags.any { it.prefix == tag }) {
                groupedTags.add(
                    TagGroup(
                        prefix = tag,
                        tags = matchingTags.map { TagUiState(it, false, label = it.substring(tag.length + 1)) }
                    )
                )
            }
        }
        return groupedTags
    }

    fun togglePriorityTag(tags: List<TagUiState>, tag: TagUiState): List<TagUiState> =
        tags.map { if (it.tag == tag.tag) it.copy(isSelected = !tag.isSelected) else it }

    fun toggleFolderTag(groups: List<TagGroup>, group: TagGroup, tag: TagUiState): List<TagGroup> =
        groups.map {
            if (it.prefix == group.prefix) {
                it.copy(tags = it.tags.map { t -> if (t.tag == tag.tag) t.copy(isSelected = !tag.isSelected) else t })
            } else {
                it
            }
        }

    /** All tags picked: typed, from the folder path, and from the selected chips. */
    fun selectedTags(
        typedTags: String,
        path: List<TagFolder>,
        priorityTags: List<TagUiState>,
        groupedFolderTags: List<TagGroup>
    ): List<String> {
        val rawTags = typedTags.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val folderTags = path.map { it.tag }
        val selectedPriorityTags = priorityTags.filter { it.isSelected }.map { it.tag }
        val selectedFolderTags = groupedFolderTags.flatMap { group -> group.tags.filter { it.isSelected }.map { it.tag } }
        return (rawTags + folderTags + selectedPriorityTags + selectedFolderTags).distinct()
    }
}
