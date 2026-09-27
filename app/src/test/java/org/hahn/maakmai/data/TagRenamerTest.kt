package org.hahn.maakmai.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TagRenamerTest {

    private lateinit var bookmarks: BookmarkRepositoryMemory
    private lateinit var folders: FolderRepositoryMemory
    private lateinit var renamer: TagRenamer

    @Before
    fun setup() {
        bookmarks = BookmarkRepositoryMemory()
        folders = FolderRepositoryMemory()
        renamer = TagRenamer(bookmarks, folders)
    }

    @Test
    fun `check reports no change for blank or same tag ignoring case`() = runBlocking {
        assertEquals(RenameCheck.NoChange, renamer.check("mittens", "  "))
        assertEquals(RenameCheck.NoChange, renamer.check("mittens", "MITTENS"))
    }

    @Test
    fun `check reports a plain rename for a new tag`() = runBlocking {
        assertEquals(RenameCheck.Rename, renamer.check("mittens", "gloves"))
    }

    @Test
    fun `check reports a merge when the new tag exists`() = runBlocking {
        // sweater only lives under knitting, scarf only under crochet: no sibling clash
        assertEquals(RenameCheck.Merge, renamer.check("sweater", "scarf"))
    }

    @Test
    fun `check reports a conflict when a sibling folder has the new tag`() = runBlocking {
        assertEquals(RenameCheck.Conflict, renamer.check("scarf", "mittens"))
    }

    @Test
    fun `rename updates folders and bookmarks together`() = runBlocking {
        val mittensCount = bookmarks.getTagsWithCount()["mittens"]

        renamer.rename("mittens", "gloves").getOrThrow()

        val allFolders = folders.getAllFolders()
        assertEquals(2, allFolders.count { it.tag == "gloves" })
        assertTrue(allFolders.none { it.tag == "mittens" })
        val counts = bookmarks.getTagsWithCount()
        assertEquals(mittensCount, counts["gloves"])
        assertEquals(null, counts["mittens"])
    }

    @Test
    fun `rename with a conflict changes nothing`() = runBlocking {
        val before = folders.getAllFolders().toSet()
        val countsBefore = bookmarks.getTagsWithCount()

        val result = renamer.rename("scarf", "mittens")

        assertTrue(result.exceptionOrNull() is TagConflictException)
        assertEquals(before, folders.getAllFolders().toSet())
        assertEquals(countsBefore, bookmarks.getTagsWithCount())
    }
}
