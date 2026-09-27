package org.hahn.maakmai.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FolderTagRenameTest {

    private val crochet = Folder(id = UUID.randomUUID(), tag = "crochet", parent = null)
    private val knitting = Folder(id = UUID.randomUUID(), tag = "knitting", parent = null)
    private val crochetMittens = Folder(id = UUID.randomUUID(), tag = "mittens", parent = crochet.id, tagGroups = listOf("winter"))
    private val crochetScarf = Folder(id = UUID.randomUUID(), tag = "scarf", parent = crochet.id, tagGroups = listOf("winter", "mittens"))
    private val knittingMittens = Folder(id = UUID.randomUUID(), tag = "mittens", parent = knitting.id)
    private val folders = listOf(crochet, knitting, crochetMittens, crochetScarf, knittingMittens)

    @Test
    fun `renames every folder with the tag, ignoring case`() {
        val changed = renameTagInFolders(folders, "MITTENS", "gloves").getOrThrow()

        val renamed = changed.filter { it.tag == "gloves" }.map { it.id }.toSet()
        assertEquals(setOf(crochetMittens.id, knittingMittens.id), renamed)
    }

    @Test
    fun `replaces the tag in tag groups`() {
        val changed = renameTagInFolders(folders, "mittens", "gloves").getOrThrow()

        assertEquals(listOf("winter", "gloves"), changed.single { it.id == crochetScarf.id }.tagGroups)
    }

    @Test
    fun `returns only folders that change`() {
        val changed = renameTagInFolders(folders, "knitting", "knit").getOrThrow()

        assertEquals(listOf(knitting.copy(tag = "knit")), changed)
    }

    @Test
    fun `fails when a sibling folder already has the new tag`() {
        val result = renameTagInFolders(folders, "scarf", "Mittens")

        assertTrue(result.exceptionOrNull() is TagConflictException)
    }

    @Test
    fun `root folders are siblings of each other`() {
        val result = renameTagInFolders(folders, "crochet", "knitting")

        assertTrue(result.exceptionOrNull() is TagConflictException)
    }

    @Test
    fun `merges into a tag that lives under a different parent`() {
        val sweater = Folder(id = UUID.randomUUID(), tag = "sweater", parent = knitting.id)

        val changed = renameTagInFolders(folders + sweater, "sweater", "scarf").getOrThrow()

        assertEquals(listOf(sweater.copy(tag = "scarf")), changed)
    }

    @Test
    fun `de-duplicates tag groups after a merge`() {
        val folder = Folder(id = UUID.randomUUID(), tag = "hats", parent = null, tagGroups = listOf("knit", "knitting"))

        val changed = renameTagInFolders(listOf(folder), "knit", "knitting").getOrThrow()

        assertEquals(listOf("knitting"), changed.single().tagGroups)
    }
}
