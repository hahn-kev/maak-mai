package org.hahn.maakmai.addeditbookmark

import com.google.common.truth.Truth.assertThat
import org.hahn.maakmai.model.TagFolder
import org.junit.Test
import java.util.UUID

class TagSelectionTest {

    private val child = TagFolder(tag = "rust", id = UUID.randomUUID(), children = emptyList(), tagGroups = listOf("lang"))
    private val root = TagFolder(tag = "code", id = UUID.randomUUID(), rootFolder = true, children = listOf(child))

    @Test
    fun `selecting a new folder adds it, selecting one in the path goes back to it`() {
        val path = TagSelection.selectFolder(listOf(root), child)
        assertThat(path).containsExactly(root, child).inOrder()

        assertThat(TagSelection.selectFolder(path, root)).containsExactly(root)
    }

    @Test
    fun `folder path comes from the browse path`() {
        assertThat(TagSelection.folderPathFor(listOf(root), "/code/rust")).containsExactly(root, child).inOrder()
        assertThat(TagSelection.folderPathFor(listOf(root), "/")).isEmpty()
        assertThat(TagSelection.folderPathFor(listOf(root), null)).isEmpty()
    }

    @Test
    fun `tag groups of the folder path collect matching tags`() {
        val groups = TagSelection.groupFolderTags(listOf(root, child), listOf("lang", "lang/en", "lang/fr", "other"))

        assertThat(groups).hasSize(1)
        assertThat(groups[0].prefix).isEqualTo("lang")
        assertThat(groups[0].tags.map { it.tag }).containsExactly("lang/en", "lang/fr").inOrder()
        assertThat(groups[0].tags.map { it.label }).containsExactly("en", "fr").inOrder()
    }

    @Test
    fun `selected tags combine typed, folder, priority and group tags without repeats`() {
        val priority = TagSelection.togglePriorityTag(
            listOf(TagUiState("news"), TagUiState("code")),
            TagUiState("news")
        )
        val group = TagGroup("lang", listOf(TagUiState("lang/en"), TagUiState("lang/fr")))
        val groups = TagSelection.toggleFolderTag(listOf(group), group, TagUiState("lang/fr"))

        val tags = TagSelection.selectedTags(" a , ,code ", listOf(root, child), priority, groups)

        assertThat(tags).containsExactly("a", "code", "rust", "news", "lang/fr").inOrder()
    }

    @Test
    fun `priority tags are ordered by use`() {
        val tags = TagSelection.prioritisedTags(mapOf("rare" to 1, "common" to 5))

        assertThat(tags.map { it.label }).containsExactly("common (5)", "rare (1)").inOrder()
    }
}
